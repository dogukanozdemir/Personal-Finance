package com.spendinganalytics.util;

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
                throw new Exception("Unable to detect account type (Garanti Debit or Credit)");
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
        // Scan first 15 rows to find header
        for (int i = 0; i < Math.min(15, sheet.getLastRowNum()); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            for (Cell cell : row) {
                String value = getCellValueAsString(cell);
                if (value != null) {
                    if (value.contains("Dekont No") || value.contains("IBAN")) {
                        return "debit";
                    } else if (value.contains("Bonus") && value.contains("Tutar")) {
                        return "credit";
                    }
                }
            }
        }
        return "unknown";
    }
    
    private static List<ParsedTransaction> parseDebitAccount(Sheet sheet) {
        List<ParsedTransaction> transactions = new ArrayList<>();
        
        // Find header row (contains "Tarih", "Açıklama", "Tutar", etc.)
        int headerRow = -1;
        for (int i = 0; i < Math.min(15, sheet.getLastRowNum() + 1); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            String firstCell = getCellValueAsString(row.getCell(0));
            if (firstCell != null && firstCell.equals("Tarih")) {
                headerRow = i;
                break;
            }
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
                
                // Merchant (Açıklama)
                Cell merchantCell = row.getCell(columnMap.getOrDefault("Açıklama", 1));
                transaction.merchant = getCellValueAsString(merchantCell);
                if (transaction.merchant == null || transaction.merchant.trim().isEmpty()) continue;
                
                // Category (Etiket)
                Cell categoryCell = row.getCell(columnMap.getOrDefault("Etiket", 2));
                transaction.category = getCellValueAsString(categoryCell);
                
                // Amount (Tutar)
                Cell amountCell = row.getCell(columnMap.getOrDefault("Tutar", 3));
                transaction.amount = parseNumericCell(amountCell);
                
                // Balance (Bakiye)
                Cell balanceCell = row.getCell(columnMap.getOrDefault("Bakiye", 4));
                transaction.balance = parseNumericCell(balanceCell);
                
                // Transaction ID (Dekont No)
                Cell transactionIdCell = row.getCell(columnMap.getOrDefault("Dekont No", 5));
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
        for (int i = 0; i < Math.min(10, sheet.getLastRowNum() + 1); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            String firstCell = getCellValueAsString(row.getCell(0));
            if (firstCell != null && firstCell.equals("Tarih")) {
                headerRow = i;
                break;
            }
        }
        
        if (headerRow == -1) {
            logger.error("Could not find header row in credit account");
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
                transaction.accountType = "credit";
                
                // Date
                Cell dateCell = row.getCell(columnMap.getOrDefault("Tarih", 0));
                transaction.date = parseDateCell(dateCell);
                if (transaction.date == null) continue;
                
                // Merchant (İşlem)
                Cell merchantCell = row.getCell(columnMap.getOrDefault("İşlem", 1));
                transaction.merchant = getCellValueAsString(merchantCell);
                if (transaction.merchant == null || transaction.merchant.trim().isEmpty()) continue;
                
                // Category (Etiket)
                Cell categoryCell = row.getCell(columnMap.getOrDefault("Etiket", 2));
                transaction.category = getCellValueAsString(categoryCell);
                
                // Bonus
                Cell bonusCell = row.getCell(columnMap.getOrDefault("Bonus", 3));
                transaction.bonusPoints = parseNumericCell(bonusCell);
                
                // Amount (Tutar(TL))
                Cell amountCell = row.getCell(columnMap.getOrDefault("Tutar(TL)", 4));
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

