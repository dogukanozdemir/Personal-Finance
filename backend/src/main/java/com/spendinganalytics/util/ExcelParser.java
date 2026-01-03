package com.spendinganalytics.util;

import lombok.experimental.UtilityClass;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@UtilityClass
public class ExcelParser {
    
    private static final Logger logger = LoggerFactory.getLogger(ExcelParser.class);
    
    public static class ParsedTransaction {
        public LocalDate date;
        public String merchant;
        public String category;
        public Double amount;
        public Double balance;
        public String transactionId;
        public Double bonusPoints;
        public String accountType; // 'debit' or 'credit'
    }
    
    public static List<ParsedTransaction> parseGarantiFile(InputStream inputStream) throws Exception {
        List<ParsedTransaction> transactions = new ArrayList<>();
        
        try {
            // Buffer the input stream so we can retry if needed
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[16384];
            int nRead;
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            byte[] excelData = buffer.toByteArray();
            
            Workbook workbook = null;
            
            // Try to open as XSSF first
            try {
                workbook = new XSSFWorkbook(new ByteArrayInputStream(excelData));
            } catch (Exception e) {
                // If it fails due to Strict OOXML, try with WorkbookFactory
                if (e.getMessage() != null && e.getMessage().contains("Strict OOXML")) {
                    logger.warn("Detected Strict OOXML format, using WorkbookFactory...");
                    workbook = WorkbookFactory.create(new ByteArrayInputStream(excelData));
                } else {
                    throw e;
                }
            }
            
            if (workbook == null) {
                throw new Exception("Unable to read Excel file");
            }
            
            Sheet sheet = workbook.getSheetAt(0);
            
            // Detect file type by looking for specific columns
            String accountType = detectAccountType(sheet);
            logger.info("Detected account type: {}", accountType);
            
            if ("debit".equals(accountType)) {
                transactions = parseDebitAccount(sheet);
            } else if ("credit".equals(accountType)) {
                transactions = parseCreditAccount(sheet);
            } else {
                // Try to parse as debit first, then credit if that fails
                logger.warn("Account type unknown, attempting to parse as debit first...");
                transactions = parseDebitAccount(sheet);
                
                if (transactions.isEmpty()) {
                    logger.warn("Debit parsing returned empty, trying credit...");
                    transactions = parseCreditAccount(sheet);
                }
                
                if (transactions.isEmpty()) {
                    throw new Exception("Unable to detect account type (Garanti Debit or Credit). " +
                            "Please ensure the file is a valid Garanti Bank statement with headers like 'Tarih', 'Açıklama/İşlem', 'Tutar'.");
                }
                
                logger.info("Successfully parsed {} transactions despite unknown account type", transactions.size());
            }
            
            workbook.close();
        } catch (Exception e) {
            logger.error("Error parsing Excel file: {}", e.getMessage());
            
            if (e.getMessage() != null && e.getMessage().contains("Strict OOXML")) {
                throw new Exception("This Excel file uses Strict OOXML format which is not fully supported. " +
                        "Please open the file in Excel and re-save it as 'Excel Workbook (.xlsx)' (not Strict).");
            }
            
            throw new Exception("Error parsing Excel file: " + e.getMessage());
        }
        
        return transactions;
    }
    
    private static String detectAccountType(Sheet sheet) {
        logger.info("Detecting account type, scanning up to row {}", Math.min(20, sheet.getLastRowNum() + 1));
        
        // Collect all cell values for debugging
        List<String> foundValues = new ArrayList<>();
        
        // Scan first 20 rows to find header (increased from 15)
        for (int i = 0; i < Math.min(20, sheet.getLastRowNum() + 1); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            for (Cell cell : row) {
                String value = getCellValueAsString(cell);
                if (value != null && !value.trim().isEmpty()) {
                    String lowerValue = value.toLowerCase().trim();
                    foundValues.add(value);
                    
                    // Debit indicators (case-insensitive, partial matches)
                    if (lowerValue.contains("dekont") || 
                        lowerValue.contains("iban") ||
                        lowerValue.contains("bakiye") ||
                        (lowerValue.contains("açıklama") && lowerValue.contains("tutar"))) {
                        logger.info("Detected DEBIT account type based on: {}", value);
                        return "debit";
                    }
                    
                    // Credit indicators (case-insensitive, partial matches)
                    if (lowerValue.contains("bonus") || 
                        (lowerValue.contains("işlem") && lowerValue.contains("tutar")) ||
                        lowerValue.contains("tutar(tl)")) {
                        logger.info("Detected CREDIT account type based on: {}", value);
                        return "credit";
                    }
                }
            }
        }
        
        // If we found a "Tarih" header, try to detect by column structure
        for (int i = 0; i < Math.min(20, sheet.getLastRowNum() + 1); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            boolean hasTarih = false;
            boolean hasDekont = false;
            boolean hasBonus = false;
            boolean hasBakiye = false;
            boolean hasIslem = false;
            
            for (Cell cell : row) {
                String value = getCellValueAsString(cell);
                if (value != null) {
                    String lowerValue = value.toLowerCase().trim();
                    if (lowerValue.contains("tarih")) hasTarih = true;
                    if (lowerValue.contains("dekont")) hasDekont = true;
                    if (lowerValue.contains("bonus")) hasBonus = true;
                    if (lowerValue.contains("bakiye")) hasBakiye = true;
                    if (lowerValue.contains("işlem")) hasIslem = true;
                }
            }
            
            if (hasTarih) {
                if (hasDekont || hasBakiye) {
                    logger.info("Detected DEBIT account type by header structure (Tarih + Dekont/Bakiye)");
                    return "debit";
                }
                if (hasBonus || (hasIslem && !hasBakiye)) {
                    logger.info("Detected CREDIT account type by header structure (Tarih + Bonus/İşlem)");
                    return "credit";
                }
            }
        }
        
        logger.warn("Could not detect account type. Found values in first rows: {}", 
                foundValues.size() > 10 ? foundValues.subList(0, 10) : foundValues);
        return "unknown";
    }
    
    private static List<ParsedTransaction> parseDebitAccount(Sheet sheet) {
        List<ParsedTransaction> transactions = new ArrayList<>();
        
        // Find header row (contains "Tarih", "Açıklama", "Tutar", etc.)
        int headerRow = -1;
        for (int i = 0; i < Math.min(20, sheet.getLastRowNum() + 1); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            // Check if this row contains "Tarih" (case-insensitive)
            for (Cell cell : row) {
                String cellValue = getCellValueAsString(cell);
                if (cellValue != null && cellValue.toLowerCase().trim().equals("tarih")) {
                    headerRow = i;
                    break;
                }
            }
            if (headerRow != -1) break;
        }
        
        if (headerRow == -1) {
            logger.error("Could not find header row in debit account");
            return transactions;
        }
        
        // Parse header to get column indices
        Row header = sheet.getRow(headerRow);
        Map<String, Integer> columnMap = new HashMap<>();
        for (Cell cell : header) {
            String value = getCellValueAsString(cell);
            if (value != null) {
                columnMap.put(value, cell.getColumnIndex());
            }
        }
        
        // Parse transactions
        for (int i = headerRow + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            try {
                ParsedTransaction transaction = new ParsedTransaction();
                transaction.accountType = "debit";
                
                // Date
                Cell dateCell = row.getCell(columnMap.getOrDefault("Tarih", 0));
                transaction.date = parseDateCell(dateCell);
                if (transaction.date == null) continue;
                
                // Merchant (Açıklama) - try multiple column name variations
                Integer merchantCol = columnMap.get("Açıklama");
                if (merchantCol == null) merchantCol = columnMap.get("açıklama");
                if (merchantCol == null) merchantCol = 1; // fallback
                Cell merchantCell = row.getCell(merchantCol);
                transaction.merchant = getCellValueAsString(merchantCell);
                if (transaction.merchant == null || transaction.merchant.trim().isEmpty()) continue;
                
                // Category (Etiket)
                Integer categoryCol = columnMap.get("Etiket");
                if (categoryCol == null) categoryCol = columnMap.get("etiket");
                if (categoryCol == null) categoryCol = 2; // fallback
                Cell categoryCell = row.getCell(categoryCol);
                transaction.category = getCellValueAsString(categoryCell);
                
                // Amount (Tutar)
                Integer amountCol = columnMap.get("Tutar");
                if (amountCol == null) amountCol = columnMap.get("tutar");
                if (amountCol == null) amountCol = 3; // fallback
                Cell amountCell = row.getCell(amountCol);
                transaction.amount = parseNumericCell(amountCell);
                
                // Balance (Bakiye)
                Integer balanceCol = columnMap.get("Bakiye");
                if (balanceCol == null) balanceCol = columnMap.get("bakiye");
                if (balanceCol == null) balanceCol = 4; // fallback
                Cell balanceCell = row.getCell(balanceCol);
                transaction.balance = parseNumericCell(balanceCell);
                
                // Transaction ID (Dekont No)
                Integer transactionIdCol = columnMap.get("Dekont No");
                if (transactionIdCol == null) transactionIdCol = columnMap.get("dekont no");
                if (transactionIdCol == null) transactionIdCol = 5; // fallback
                Cell transactionIdCell = row.getCell(transactionIdCol);
                transaction.transactionId = getCellValueAsString(transactionIdCell);
                
                transactions.add(transaction);
            } catch (Exception e) {
                logger.warn("Error parsing debit row {}: {}", i, e.getMessage());
            }
        }
        
        return transactions;
    }
    
    private static List<ParsedTransaction> parseCreditAccount(Sheet sheet) {
        List<ParsedTransaction> transactions = new ArrayList<>();
        
        // Find header row (contains "Tarih", "İşlem", "Tutar(TL)", etc.)
        int headerRow = -1;
        for (int i = 0; i < Math.min(20, sheet.getLastRowNum() + 1); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            // Check if this row contains "Tarih" (case-insensitive)
            for (Cell cell : row) {
                String cellValue = getCellValueAsString(cell);
                if (cellValue != null && cellValue.toLowerCase().trim().equals("tarih")) {
                    headerRow = i;
                    break;
                }
            }
            if (headerRow != -1) break;
        }
        
        if (headerRow == -1) {
            logger.error("Could not find header row in credit account");
            return transactions;
        }
        
        // Parse header to get column indices (case-insensitive matching)
        Row header = sheet.getRow(headerRow);
        Map<String, Integer> columnMap = new HashMap<>();
        for (Cell cell : header) {
            String value = getCellValueAsString(cell);
            if (value != null) {
                String normalized = value.trim();
                columnMap.put(normalized, cell.getColumnIndex());
                // Also store lowercase version for flexible matching
                columnMap.put(normalized.toLowerCase(), cell.getColumnIndex());
            }
        }
        
        // Parse transactions
        for (int i = headerRow + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            try {
                ParsedTransaction transaction = new ParsedTransaction();
                transaction.accountType = "credit";
                
                // Date
                Integer dateCol = columnMap.get("Tarih");
                if (dateCol == null) dateCol = columnMap.get("tarih");
                if (dateCol == null) dateCol = 0; // fallback
                Cell dateCell = row.getCell(dateCol);
                transaction.date = parseDateCell(dateCell);
                if (transaction.date == null) continue;
                
                // Merchant (İşlem)
                Integer merchantCol = columnMap.get("İşlem");
                if (merchantCol == null) merchantCol = columnMap.get("işlem");
                if (merchantCol == null) merchantCol = 1; // fallback
                Cell merchantCell = row.getCell(merchantCol);
                transaction.merchant = getCellValueAsString(merchantCell);
                if (transaction.merchant == null || transaction.merchant.trim().isEmpty()) continue;
                
                // Category (Etiket)
                Integer categoryCol = columnMap.get("Etiket");
                if (categoryCol == null) categoryCol = columnMap.get("etiket");
                if (categoryCol == null) categoryCol = 2; // fallback
                Cell categoryCell = row.getCell(categoryCol);
                transaction.category = getCellValueAsString(categoryCell);
                
                // Bonus
                Integer bonusCol = columnMap.get("Bonus");
                if (bonusCol == null) bonusCol = columnMap.get("bonus");
                if (bonusCol == null) bonusCol = 3; // fallback
                Cell bonusCell = row.getCell(bonusCol);
                transaction.bonusPoints = parseNumericCell(bonusCell);
                
                // Amount (Tutar(TL)) - try variations
                Integer amountCol = columnMap.get("Tutar(TL)");
                if (amountCol == null) amountCol = columnMap.get("tutar(tl)");
                if (amountCol == null) amountCol = columnMap.get("Tutar");
                if (amountCol == null) amountCol = columnMap.get("tutar");
                if (amountCol == null) amountCol = 4; // fallback
                Cell amountCell = row.getCell(amountCol);
                transaction.amount = parseNumericCell(amountCell);
                
                transactions.add(transaction);
            } catch (Exception e) {
                logger.warn("Error parsing credit row {}: {}", i, e.getMessage());
            }
        }
        
        return transactions;
    }
    
    private static String getCellValueAsString(Cell cell) {
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
                return cell.getCellFormula();
            default:
                return null;
        }
    }
    
    private static Double parseNumericCell(Cell cell) {
        if (cell == null) return null;
        
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                String value = cell.getStringCellValue().trim();
                // Handle Turkish number format (comma as decimal separator)
                value = value.replace(".", "").replace(",", ".");
                return Double.parseDouble(value);
            }
        } catch (Exception e) {
            logger.warn("Error parsing numeric cell: {}", e.getMessage());
        }
        return null;
    }
    
    private static LocalDate parseDateCell(Cell cell) {
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
}

