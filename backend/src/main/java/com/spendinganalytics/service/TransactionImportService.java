package com.spendinganalytics.service;

import com.spendinganalytics.dto.FileImportResult;
import com.spendinganalytics.dto.TransactionImportResult;
import com.spendinganalytics.entity.Transaction;
import com.spendinganalytics.repository.TransactionRepository;
import com.spendinganalytics.util.HashUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class TransactionImportService {

  private static final Logger logger = LoggerFactory.getLogger(TransactionImportService.class);

  private final TransactionRepository transactionRepository;

  @Transactional
  public TransactionImportResult importTransactions(MultipartFile[] files) {
    logger.info("Starting import for {} files", files.length);

    // Parse all files and collect transactions per file
    List<List<Transaction>> fileTransactionsList = new ArrayList<>();
    List<FileImportResult> fileResults = new ArrayList<>();

    for (int i = 0; i < files.length; i++) {
      MultipartFile file = files[i];
      try {
        List<Transaction> fileTransactions = parseFileToTransactions(file);
        fileTransactionsList.add(fileTransactions);
      } catch (Exception e) {
        logger.error("Error parsing file {}: {}", file.getOriginalFilename(), e.getMessage(), e);
        fileTransactionsList.add(new ArrayList<>());
        fileResults.add(
            new FileImportResult(
                file.getOriginalFilename(), 0, 0, 0, List.of("Error: " + e.getMessage())));
      }
    }

    // Collect all transactions and deduplicate within batch (keep first occurrence)
    Map<String, Transaction> uniqueTransactions = new LinkedHashMap<>();
    for (List<Transaction> fileTransactions : fileTransactionsList) {
      for (Transaction t : fileTransactions) {
        Transaction existing = uniqueTransactions.putIfAbsent(t.getDedupHash(), t);
        if (existing != null) {
          // Duplicate found within batch
          logger.info(
              "Duplicate transaction found within batch - Hash: {}, "
                  + "Existing: {} | {} | {} | {}, "
                  + "Duplicate: {} | {} | {} | {}",
              t.getDedupHash(),
              existing.getTransactionDate(),
              existing.getMerchant(),
              existing.getAmount(),
              existing.getTransactionId(),
              t.getTransactionDate(),
              t.getMerchant(),
              t.getAmount(),
              t.getTransactionId());
        }
      }
    }

    // Check against database for existing hashes
    Set<String> allHashes = uniqueTransactions.keySet();
    Set<String> existingHashes =
        new HashSet<>(transactionRepository.findExistingDedupHashes(allHashes));

    // Filter out transactions that already exist in database
    List<Transaction> newTransactions = new ArrayList<>();
    for (Transaction t : uniqueTransactions.values()) {
      if (existingHashes.contains(t.getDedupHash())) {
        // Duplicate found in database
        transactionRepository
            .findByDedupHash(t.getDedupHash())
            .ifPresent(
                existing -> {
                  logger.info(
                      "Duplicate transaction found in database - Hash: {}, "
                          + "Existing in DB: {} | {} | {} | {}, "
                          + "Skipped: {} | {} | {} | {}",
                      t.getDedupHash(),
                      existing.getTransactionDate(),
                      existing.getMerchant(),
                      existing.getAmount(),
                      existing.getTransactionId(),
                      t.getTransactionDate(),
                      t.getMerchant(),
                      t.getAmount(),
                      t.getTransactionId());
                });
      } else {
        newTransactions.add(t);
      }
    }

    // Save all new transactions in one batch
    if (!newTransactions.isEmpty()) {
      transactionRepository.saveAll(newTransactions);
    }

    Set<String> insertedHashes =
        newTransactions.stream().map(Transaction::getDedupHash).collect(Collectors.toSet());

    // Calculate per-file statistics
    int totalRowsParsed = 0;
    int totalInserted = 0;
    int totalSkippedDuplicates = 0;

    for (int i = 0; i < files.length; i++) {
      if (i < fileResults.size()) {
        // Already added error result
        continue;
      }

      List<Transaction> fileTransactions = fileTransactionsList.get(i);
      int rowsParsed = fileTransactions.size();

      // Count unique inserted transactions from this file
      Set<String> fileInsertedHashes =
          fileTransactions.stream()
              .map(Transaction::getDedupHash)
              .filter(insertedHashes::contains)
              .collect(Collectors.toSet());

      int inserted = fileInsertedHashes.size();
      int skipped = rowsParsed - inserted;

      totalRowsParsed += rowsParsed;
      totalInserted += inserted;
      totalSkippedDuplicates += skipped;

      fileResults.add(
          new FileImportResult(
              files[i].getOriginalFilename(), rowsParsed, inserted, skipped, new ArrayList<>()));
    }

    logger.info(
        "Import completed: {} files, {} rows parsed, {} inserted, {} duplicates",
        files.length,
        totalRowsParsed,
        totalInserted,
        totalSkippedDuplicates);

    return new TransactionImportResult(
        files.length, totalRowsParsed, totalInserted, totalSkippedDuplicates, fileResults);
  }

  private List<Transaction> parseFileToTransactions(MultipartFile file) throws Exception {
    logger.info("Parsing file: {}", file.getOriginalFilename());

    List<String> errors = new ArrayList<>();
    List<Transaction> transactions = new ArrayList<>();

    try (InputStream inputStream = file.getInputStream()) {
      // Buffer the input stream
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      byte[] data = new byte[16384];
      int nRead;
      while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
        buffer.write(data, 0, nRead);
      }
      buffer.flush();
      byte[] excelData = buffer.toByteArray();

      Workbook workbook;
      try {
        workbook = new XSSFWorkbook(new ByteArrayInputStream(excelData));
      } catch (Exception e) {
        if (e.getMessage() != null && e.getMessage().contains("Strict OOXML")) {
          workbook = WorkbookFactory.create(new ByteArrayInputStream(excelData));
        } else {
          throw e;
        }
      }

      Sheet sheet = workbook.getSheetAt(0);

      // Detect file type by header row
      String fileType = detectFileType(sheet);
      if (fileType == null) {
        workbook.close();
        throw new Exception(
            "Unable to detect file type (debit or credit). Expected headers: "
                + "Debit: Tarih, Açıklama, Etiket, Tutar, Bakiye, Dekont No; "
                + "Credit: Tarih, İşlem, Etiket, Bonus, Tutar(TL)");
      }

      logger.info("Detected file type: {} for file: {}", fileType, file.getOriginalFilename());

      // Parse transactions based on file type
      List<ParsedRow> parsedRows;
      if ("debit".equals(fileType)) {
        parsedRows = parseDebitFile(sheet, errors);
      } else {
        parsedRows = parseCreditFile(sheet, errors);
      }

      workbook.close();

      // Convert parsed rows to Transaction entities
      for (ParsedRow row : parsedRows) {
        try {
          Transaction transaction = createTransaction(row, fileType);
          transactions.add(transaction);
        } catch (Exception e) {
          errors.add("Row error: " + e.getMessage());
        }
      }
    }

    if (!errors.isEmpty()) {
      logger.warn("Errors parsing file {}: {}", file.getOriginalFilename(), errors);
    }

    return transactions;
  }

  private String detectFileType(Sheet sheet) {
    // Scan first 20 rows for header
    for (int i = 0; i < Math.min(20, sheet.getLastRowNum() + 1); i++) {
      Row row = sheet.getRow(i);
      if (row == null) continue;

      Set<String> headers = new HashSet<>();
      for (Cell cell : row) {
        String value = getCellValueAsString(cell);
        if (value != null && !value.trim().isEmpty()) {
          headers.add(value.trim());
        }
      }

      // Check for debit headers
      boolean hasDebitHeaders =
          headers.contains("Tarih")
              && headers.contains("Açıklama")
              && headers.contains("Etiket")
              && headers.contains("Tutar")
              && headers.contains("Bakiye")
              && headers.contains("Dekont No");

      // Check for credit headers
      boolean hasCreditHeaders =
          headers.contains("Tarih")
              && headers.contains("İşlem")
              && headers.contains("Etiket")
              && headers.contains("Bonus")
              && headers.contains("Tutar(TL)");

      if (hasDebitHeaders) {
        return "debit";
      }
      if (hasCreditHeaders) {
        return "credit";
      }
    }
    return null;
  }

  private List<ParsedRow> parseDebitFile(Sheet sheet, List<String> errors) {
    List<ParsedRow> rows = new ArrayList<>();

    // Find header row
    int headerRow = findHeaderRow(sheet, "Tarih");
    if (headerRow == -1) {
      errors.add("Could not find header row");
      return rows;
    }

    // Map columns
    Row header = sheet.getRow(headerRow);
    Map<String, Integer> columnMap = new HashMap<>();
    for (Cell cell : header) {
      String value = getCellValueAsString(cell);
      if (value != null) {
        columnMap.put(value.trim(), cell.getColumnIndex());
      }
    }

    // Parse data rows
    for (int i = headerRow + 1; i <= sheet.getLastRowNum(); i++) {
      Row row = sheet.getRow(i);
      if (row == null) continue;

      try {
        ParsedRow parsed = new ParsedRow();
        parsed.fileType = "debit";

        // Date (Tarih)
        Integer dateCol = columnMap.get("Tarih");
        if (dateCol == null) continue;
        parsed.date = parseDateCell(row.getCell(dateCol));
        if (parsed.date == null) continue;

        // Merchant (Açıklama)
        Integer merchantCol = columnMap.get("Açıklama");
        if (merchantCol == null) continue;
        parsed.merchant = getCellValueAsString(row.getCell(merchantCol));
        if (parsed.merchant == null || parsed.merchant.trim().isEmpty()) continue;
        parsed.rawDescription = parsed.merchant;

        // Category (Etiket)
        Integer categoryCol = columnMap.get("Etiket");
        if (categoryCol != null) {
          parsed.userCategory = getCellValueAsString(row.getCell(categoryCol));
        }

        // Skip rows with specific tags
        if (parsed.userCategory != null) {
          String etiketLower = parsed.userCategory.toLowerCase().trim();
          if (etiketLower.contains("döviz al / sat") || etiketLower.contains("kart ödemesi")) {
            continue; // Skip this row
          }
        }

        // Amount (Tutar)
        Integer amountCol = columnMap.get("Tutar");
        if (amountCol == null) continue;
        parsed.amount = parseTurkishAmount(row.getCell(amountCol));
        if (parsed.amount == null) continue;

        // Balance (Bakiye)
        Integer balanceCol = columnMap.get("Bakiye");
        if (balanceCol != null) {
          parsed.balance = parseTurkishAmount(row.getCell(balanceCol));
        }

        // Transaction ID (Dekont No) - REQUIRED for debit
        Integer transactionIdCol = columnMap.get("Dekont No");
        if (transactionIdCol == null) {
          errors.add(String.format("Row %d: Missing Dekont No column", i + 1));
          continue;
        }
        parsed.transactionId = getCellValueAsString(row.getCell(transactionIdCol));
        if (parsed.transactionId == null || parsed.transactionId.trim().isEmpty()) {
          errors.add(String.format("Row %d: Missing Dekont No value", i + 1));
          continue; // Skip row if Dekont No is missing
        }

        rows.add(parsed);
      } catch (Exception e) {
        errors.add(String.format("Row %d: %s", i + 1, e.getMessage()));
      }
    }

    return rows;
  }

  private List<ParsedRow> parseCreditFile(Sheet sheet, List<String> errors) {
    List<ParsedRow> rows = new ArrayList<>();

    // Find header row
    int headerRow = findHeaderRow(sheet, "Tarih");
    if (headerRow == -1) {
      errors.add("Could not find header row");
      return rows;
    }

    // Map columns
    Row header = sheet.getRow(headerRow);
    Map<String, Integer> columnMap = new HashMap<>();
    for (Cell cell : header) {
      String value = getCellValueAsString(cell);
      if (value != null) {
        columnMap.put(value.trim(), cell.getColumnIndex());
      }
    }

    // Parse data rows
    for (int i = headerRow + 1; i <= sheet.getLastRowNum(); i++) {
      Row row = sheet.getRow(i);
      if (row == null) continue;

      try {
        ParsedRow parsed = new ParsedRow();
        parsed.fileType = "credit";

        // Date (Tarih)
        Integer dateCol = columnMap.get("Tarih");
        if (dateCol == null) continue;
        parsed.date = parseDateCell(row.getCell(dateCol));
        if (parsed.date == null) continue;

        // Merchant (İşlem)
        Integer merchantCol = columnMap.get("İşlem");
        if (merchantCol == null) continue;
        parsed.merchant = getCellValueAsString(row.getCell(merchantCol));
        if (parsed.merchant == null || parsed.merchant.trim().isEmpty()) continue;
        parsed.rawDescription = parsed.merchant;

        // Category (Etiket) - optional, treat as empty string if missing
        Integer categoryCol = columnMap.get("Etiket");
        if (categoryCol != null) {
          parsed.userCategory = getCellValueAsString(row.getCell(categoryCol));
        }
        if (parsed.userCategory == null) {
          parsed.userCategory = "";
        }

        // Skip rows with specific tags
        if (parsed.userCategory != null && !parsed.userCategory.isEmpty()) {
          String etiketLower = parsed.userCategory.toLowerCase().trim();
          if (etiketLower.contains("döviz al / sat") || etiketLower.contains("kart ödemesi")) {
            continue; // Skip this row
          }
        }

        // Amount (Tutar(TL))
        Integer amountCol = columnMap.get("Tutar(TL)");
        if (amountCol == null) continue;
        parsed.amount = parseTurkishAmount(row.getCell(amountCol));
        if (parsed.amount == null) continue;

        // Bonus - read but don't use in dedup
        Integer bonusCol = columnMap.get("Bonus");
        if (bonusCol != null) {
          parsed.bonusPoints = parseTurkishAmount(row.getCell(bonusCol));
        }

        rows.add(parsed);
      } catch (Exception e) {
        errors.add(String.format("Row %d: %s", i + 1, e.getMessage()));
      }
    }

    return rows;
  }

  private Transaction createTransaction(ParsedRow row, String fileType) {
    Transaction transaction = new Transaction();
    transaction.setTransactionDate(row.date);
    transaction.setMerchant(row.merchant);
    transaction.setAmount(row.amount);
    transaction.setBalance(row.balance);
    transaction.setTransactionId(row.transactionId);
    transaction.setRawDescription(row.rawDescription);
    transaction.setImportTimestamp(LocalDateTime.now());
    transaction.setIsSubscription(false);

    // Generate dedup hash based on file type
    String dedupHash;
    if ("debit".equals(fileType)) {
      // Debit: SHA256("DEBIT|" + dateISO + "|" + descriptionTrim + "|" + amountNormalized + "|" +
      // dekontNoTrim)
      String dateISO = row.date.format(DateTimeFormatter.ISO_LOCAL_DATE);
      String descriptionTrim = row.merchant.trim();
      String amountNormalized = row.amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
      String dekontNoTrim = row.transactionId != null ? row.transactionId.trim() : "";
      dedupHash =
          HashUtil.generateSHA256(
              "DEBIT|"
                  + dateISO
                  + "|"
                  + descriptionTrim
                  + "|"
                  + amountNormalized
                  + "|"
                  + dekontNoTrim);
    } else {
      // Credit: SHA256("CREDIT|" + dateISO + "|" + islemTrim + "|" + etiketTrimOrEmpty + "|" +
      // amountNormalized)
      String dateISO = row.date.format(DateTimeFormatter.ISO_LOCAL_DATE);
      String islemTrim = row.merchant.trim();
      String etiketTrimOrEmpty = (row.userCategory != null ? row.userCategory.trim() : "");
      String amountNormalized = row.amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
      dedupHash =
          HashUtil.generateSHA256(
              "CREDIT|"
                  + dateISO
                  + "|"
                  + islemTrim
                  + "|"
                  + etiketTrimOrEmpty
                  + "|"
                  + amountNormalized);
    }

    transaction.setDedupHash(dedupHash);
    return transaction;
  }

  private int findHeaderRow(Sheet sheet, String headerName) {
    for (int i = 0; i < Math.min(20, sheet.getLastRowNum() + 1); i++) {
      Row row = sheet.getRow(i);
      if (row == null) continue;

      for (Cell cell : row) {
        String value = getCellValueAsString(cell);
        if (value != null && value.trim().equalsIgnoreCase(headerName)) {
          return i;
        }
      }
    }
    return -1;
  }

  private String getCellValueAsString(Cell cell) {
    if (cell == null) return null;

    switch (cell.getCellType()) {
      case STRING:
        return cell.getStringCellValue().trim();
      case NUMERIC:
        if (DateUtil.isCellDateFormatted(cell)) {
          return cell.getLocalDateTimeCellValue().toString();
        } else {
          double value = cell.getNumericCellValue();
          if (value == (long) value) {
            return String.valueOf((long) value);
          } else {
            return String.valueOf(value);
          }
        }
      case BOOLEAN:
        return String.valueOf(cell.getBooleanCellValue());
      case FORMULA:
        try {
          return cell.getStringCellValue();
        } catch (Exception e) {
          return String.valueOf(cell.getNumericCellValue());
        }
      default:
        return null;
    }
  }

  private LocalDate parseDateCell(Cell cell) {
    if (cell == null) return null;

    try {
      if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
        Date date = cell.getDateCellValue();
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
      } else if (cell.getCellType() == CellType.STRING) {
        String dateStr = cell.getStringCellValue().trim();
        // Parse DD/MM/YYYY format
        String[] parts = dateStr.split("/");
        if (parts.length == 3) {
          int day = Integer.parseInt(parts[0]);
          int month = Integer.parseInt(parts[1]);
          int year = Integer.parseInt(parts[2]);
          return LocalDate.of(year, month, day);
        }
      }
    } catch (Exception e) {
      logger.warn("Error parsing date cell: {}", e.getMessage());
    }
    return null;
  }

  private BigDecimal parseTurkishAmount(Cell cell) {
    if (cell == null) return null;

    try {
      if (cell.getCellType() == CellType.NUMERIC) {
        return BigDecimal.valueOf(cell.getNumericCellValue()).setScale(2, RoundingMode.HALF_UP);
      } else if (cell.getCellType() == CellType.STRING) {
        String value = cell.getStringCellValue().trim();
        value = value.replace(".", "").replace(",", ".");
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
      } else if (cell.getCellType() == CellType.FORMULA) {
        try {
          double numericValue = cell.getNumericCellValue();
          return BigDecimal.valueOf(numericValue).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
          // Try as string
          String value = cell.getStringCellValue().trim();
          value = value.replace(".", "").replace(",", ".");
          return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
        }
      }
    } catch (Exception e) {
      logger.warn("Error parsing amount cell: {}", e.getMessage());
    }
    return null;
  }

  // Inner class for parsed row data
  private static class ParsedRow {
    String fileType;
    LocalDate date;
    String merchant;
    BigDecimal amount;
    BigDecimal balance;
    String transactionId;
    String userCategory;
    BigDecimal bonusPoints;
    String rawDescription;
  }
}
