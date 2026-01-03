package com.spendinganalytics.service;

import com.spendinganalytics.dto.DeleteAllDataResultDTO;
import com.spendinganalytics.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DataService {
    
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final InsightCacheRepository insightCacheRepository;
    private final BudgetRepository budgetRepository;
    private final RuleRepository ruleRepository;
    
    @Transactional
    public DeleteAllDataResultDTO deleteAllData() {
        // Count and delete in order to respect foreign key constraints
        // Transactions first (they reference accounts)
        long transactionCount = transactionRepository.count();
        transactionRepository.deleteAll();
        
        // Then accounts
        long accountCount = accountRepository.count();
        accountRepository.deleteAll();
        
        // Then other data
        long insightCount = insightCacheRepository.count();
        insightCacheRepository.deleteAll();
        
        long budgetCount = budgetRepository.count();
        budgetRepository.deleteAll();
        
        long ruleCount = ruleRepository.count();
        ruleRepository.deleteAll();
        
        return new DeleteAllDataResultDTO(
            transactionCount,
            accountCount,
            insightCount,
            budgetCount,
            ruleCount
        );
    }
}

