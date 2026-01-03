package com.spendinganalytics.service;

import com.spendinganalytics.model.Transaction;
import com.spendinganalytics.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TransactionService {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }
    
    public List<Transaction> getTransactionsByDateRange(LocalDate start, LocalDate end) {
        return transactionRepository.findByTransactionDateBetween(start, end);
    }
    
    public List<Transaction> getRecentTransactions(int days) {
        LocalDate startDate = LocalDate.now().minusDays(days);
        return transactionRepository.findRecentTransactions(startDate);
    }
    
    public Transaction updateTransaction(Long id, Transaction updatedTransaction) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        if (updatedTransaction.getUserCategory() != null) {
            transaction.setUserCategory(updatedTransaction.getUserCategory());
        }
        if (updatedTransaction.getIsSubscription() != null) {
            transaction.setIsSubscription(updatedTransaction.getIsSubscription());
        }
        
        return transactionRepository.save(transaction);
    }
}

