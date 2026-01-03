package com.spendinganalytics.service;

import com.spendinganalytics.model.Account;
import com.spendinganalytics.model.ImportResult;
import com.spendinganalytics.model.Transaction;
import com.spendinganalytics.repository.AccountRepository;
import com.spendinganalytics.repository.TransactionRepository;
import com.spendinganalytics.util.ExcelParser;
import com.spendinganalytics.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class ImportService {
    
    private static final Logger logger = LoggerFactory.getLogger(ImportService.class);
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Transactional
    public ImportResult importFile(MultipartFile file, Long accountId) throws Exception {
        logger.info("Starting import for file: {}", file.getOriginalFilename());
        
        ImportResult result = new ImportResult();
        result.setTotalRows(0);
        result.setNewTransactions(0);
        result.setDuplicates(0);
        
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
        
        result.setAccountName(account.getName());
        
        // Parse Excel file
        try (InputStream inputStream = file.getInputStream()) {
            List<ExcelParser.ParsedTransaction> parsedTransactions = 
                    ExcelParser.parseGarantiFile(inputStream);
            
            result.setTotalRows(parsedTransactions.size());
            
            if (parsedTransactions.isEmpty()) {
                logger.warn("No transactions found in file");
                return result;
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
                    result.setDuplicates(result.getDuplicates() + 1);
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
                result.setNewTransactions(result.getNewTransactions() + 1);
            }
            
            result.setDateRangeStart(minDate != null ? minDate.toString() : "");
            result.setDateRangeEnd(maxDate != null ? maxDate.toString() : "");
            
            logger.info("Import completed: {} new, {} duplicates", 
                    result.getNewTransactions(), result.getDuplicates());
        }
        
        return result;
    }
    
    @Transactional
    public ImportResult importMultipleFiles(MultipartFile[] files, Long accountId) throws Exception {
        logger.info("Starting batch import for {} files", files.length);
        
        ImportResult aggregatedResult = new ImportResult();
        aggregatedResult.setTotalRows(0);
        aggregatedResult.setNewTransactions(0);
        aggregatedResult.setDuplicates(0);
        
        LocalDate overallMinDate = null;
        LocalDate overallMaxDate = null;
        
        for (MultipartFile file : files) {
            logger.info("Processing file: {}", file.getOriginalFilename());
            
            try {
                ImportResult fileResult = importFile(file, accountId);
                
                // Aggregate results
                aggregatedResult.setTotalRows(
                    aggregatedResult.getTotalRows() + fileResult.getTotalRows()
                );
                aggregatedResult.setNewTransactions(
                    aggregatedResult.getNewTransactions() + fileResult.getNewTransactions()
                );
                aggregatedResult.setDuplicates(
                    aggregatedResult.getDuplicates() + fileResult.getDuplicates()
                );
                
                // Update date range
                if (fileResult.getDateRangeStart() != null && !fileResult.getDateRangeStart().isEmpty()) {
                    LocalDate fileMinDate = LocalDate.parse(fileResult.getDateRangeStart());
                    if (overallMinDate == null || fileMinDate.isBefore(overallMinDate)) {
                        overallMinDate = fileMinDate;
                    }
                }
                
                if (fileResult.getDateRangeEnd() != null && !fileResult.getDateRangeEnd().isEmpty()) {
                    LocalDate fileMaxDate = LocalDate.parse(fileResult.getDateRangeEnd());
                    if (overallMaxDate == null || fileMaxDate.isAfter(overallMaxDate)) {
                        overallMaxDate = fileMaxDate;
                    }
                }
                
                // Use first file's account name
                if (aggregatedResult.getAccountName() == null) {
                    aggregatedResult.setAccountName(fileResult.getAccountName());
                }
                
            } catch (Exception e) {
                logger.error("Error processing file {}: {}", file.getOriginalFilename(), e.getMessage());
                // Continue processing other files even if one fails
            }
        }
        
        aggregatedResult.setDateRangeStart(overallMinDate != null ? overallMinDate.toString() : "");
        aggregatedResult.setDateRangeEnd(overallMaxDate != null ? overallMaxDate.toString() : "");
        
        logger.info("Batch import completed: {} files processed, {} new transactions, {} duplicates", 
                files.length, aggregatedResult.getNewTransactions(), aggregatedResult.getDuplicates());
        
        return aggregatedResult;
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

