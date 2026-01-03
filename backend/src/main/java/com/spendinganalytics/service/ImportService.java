package com.spendinganalytics.service;

import com.spendinganalytics.dto.AccountDTO;
import com.spendinganalytics.dto.ImportResultDTO;
import com.spendinganalytics.entity.Account;
import com.spendinganalytics.entity.Transaction;
import com.spendinganalytics.repository.AccountRepository;
import com.spendinganalytics.repository.TransactionRepository;
import com.spendinganalytics.util.DtoMapper;
import com.spendinganalytics.util.ExcelParser;
import com.spendinganalytics.util.HashUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImportService {
    
    private static final Logger logger = LoggerFactory.getLogger(ImportService.class);
    
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    
    @Transactional
    public ImportResultDTO importFile(MultipartFile file, Long accountId) throws Exception {
        logger.info("Starting import for file: {}", file.getOriginalFilename());
        
        int totalRows = 0;
        int newTransactions = 0;
        int duplicates = 0;
        
        // Get or create account
        Account account;
        if (accountId != null) {
            account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new Exception("Account not found"));
        } else {
            // Create default account
            account = new Account();
            account.setName("Default Account");
            account.setType("unknown");
            account.setCurrency("TRY");
            account = accountRepository.save(account);
        }
        
        String accountName = account.getName();
        
        // Parse Excel file
        try (InputStream inputStream = file.getInputStream()) {
            List<ExcelParser.ParsedTransaction> parsedTransactions = 
                    ExcelParser.parseGarantiFile(inputStream);
            
            totalRows = parsedTransactions.size();
            
            if (parsedTransactions.isEmpty()) {
                logger.warn("No transactions found in file");
                return new ImportResultDTO(0, 0, 0, "", "", accountName);
            }
            
            // Update account type if not set
            if ("unknown".equals(account.getType())) {
                account.setType(parsedTransactions.get(0).accountType);
                accountRepository.save(account);
            }
            
            LocalDate minDate = null;
            LocalDate maxDate = null;
            
            // Process each transaction
            for (ExcelParser.ParsedTransaction parsed : parsedTransactions) {
                // Track date range
                if (minDate == null || parsed.date.isBefore(minDate)) {
                    minDate = parsed.date;
                }
                if (maxDate == null || parsed.date.isAfter(maxDate)) {
                    maxDate = parsed.date;
                }
                
                // Generate dedup hash
                String dedupHash = generateDedupHash(
                        account.getId(),
                        parsed.date,
                        parsed.merchant,
                        parsed.amount,
                        parsed.transactionId
                );
                
                // Check if transaction already exists
                if (transactionRepository.findByDedupHash(dedupHash).isPresent()) {
                    duplicates++;
                    continue;
                }
                
                // Create new transaction
                Transaction transaction = new Transaction();
                transaction.setAccountId(account.getId());
                transaction.setTransactionDate(parsed.date);
                transaction.setMerchant(parsed.merchant);
                transaction.setAmount(BigDecimal.valueOf(parsed.amount != null ? parsed.amount : 0));
                transaction.setBalance(parsed.balance != null ? BigDecimal.valueOf(parsed.balance) : null);
                transaction.setTransactionId(parsed.transactionId);
                transaction.setCategory(parsed.category);
                transaction.setBonusPoints(parsed.bonusPoints != null ? BigDecimal.valueOf(parsed.bonusPoints) : null);
                transaction.setRawDescription(parsed.merchant);
                transaction.setDedupHash(dedupHash);
                transaction.setIsSubscription(false);
                
                transactionRepository.save(transaction);
                newTransactions++;
            }
            
            String dateRangeStart = minDate != null ? minDate.toString() : "";
            String dateRangeEnd = maxDate != null ? maxDate.toString() : "";
            
            logger.info("Import completed: {} new, {} duplicates", newTransactions, duplicates);
            
            return new ImportResultDTO(totalRows, newTransactions, duplicates, dateRangeStart, dateRangeEnd, accountName);
        }
    }
    
    @Transactional
    public ImportResultDTO importMultipleFiles(MultipartFile[] files, Long accountId) throws Exception {
        logger.info("Starting batch import for {} files", files.length);
        
        int totalRows = 0;
        int newTransactions = 0;
        int duplicates = 0;
        LocalDate overallMinDate = null;
        LocalDate overallMaxDate = null;
        String accountName = null;
        
        for (MultipartFile file : files) {
            logger.info("Processing file: {}", file.getOriginalFilename());
            
            try {
                ImportResultDTO fileResult = importFile(file, accountId);
                
                // Aggregate results
                totalRows += fileResult.totalRows();
                newTransactions += fileResult.newTransactions();
                duplicates += fileResult.duplicates();
                
                // Update date range
                if (fileResult.dateRangeStart() != null && !fileResult.dateRangeStart().isEmpty()) {
                    LocalDate fileMinDate = LocalDate.parse(fileResult.dateRangeStart());
                    if (overallMinDate == null || fileMinDate.isBefore(overallMinDate)) {
                        overallMinDate = fileMinDate;
                    }
                }
                
                if (fileResult.dateRangeEnd() != null && !fileResult.dateRangeEnd().isEmpty()) {
                    LocalDate fileMaxDate = LocalDate.parse(fileResult.dateRangeEnd());
                    if (overallMaxDate == null || fileMaxDate.isAfter(overallMaxDate)) {
                        overallMaxDate = fileMaxDate;
                    }
                }
                
                // Use first file's account name
                if (accountName == null) {
                    accountName = fileResult.accountName();
                }
                
            } catch (Exception e) {
                logger.error("Error processing file {}: {}", file.getOriginalFilename(), e.getMessage());
                // Continue processing other files even if one fails
            }
        }
        
        String dateRangeStart = overallMinDate != null ? overallMinDate.toString() : "";
        String dateRangeEnd = overallMaxDate != null ? overallMaxDate.toString() : "";
        
        logger.info("Batch import completed: {} files processed, {} new transactions, {} duplicates", 
                files.length, newTransactions, duplicates);
        
        return new ImportResultDTO(totalRows, newTransactions, duplicates, dateRangeStart, dateRangeEnd, accountName != null ? accountName : "");
    }
    
    private String generateDedupHash(Long accountId, LocalDate date, String merchant, 
                                      Double amount, String transactionId) {
        StringBuilder sb = new StringBuilder();
        sb.append(accountId).append("|");
        sb.append(date.toString()).append("|");
        
        // If we have a transaction ID (debit), use it
        if (transactionId != null && !transactionId.trim().isEmpty()) {
            sb.append(transactionId);
        } else {
            // Otherwise use merchant + amount (credit)
            sb.append(merchant).append("|");
            sb.append(amount != null ? Math.abs(amount) : 0);
        }
        
        return HashUtil.generateSHA256(sb.toString());
    }
}

