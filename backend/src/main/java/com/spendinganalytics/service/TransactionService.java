package com.spendinganalytics.service;

import com.spendinganalytics.dto.TransactionDTO;
import com.spendinganalytics.entity.Transaction;
import com.spendinganalytics.repository.TransactionRepository;
import com.spendinganalytics.util.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {
    
    private final TransactionRepository transactionRepository;
    
    public List<TransactionDTO> getAllTransactions() {
        return transactionRepository.findAll().stream()
            .map(DtoMapper::toDto)
            .collect(Collectors.toList());
    }
    
    public List<TransactionDTO> getTransactionsByDateRange(LocalDate start, LocalDate end) {
        return transactionRepository.findByTransactionDateBetween(start, end).stream()
            .map(DtoMapper::toDto)
            .collect(Collectors.toList());
    }
    
    public List<TransactionDTO> getRecentTransactions(int days) {
        LocalDate startDate = LocalDate.now().minusDays(days);
        return transactionRepository.findRecentTransactions(startDate).stream()
            .map(DtoMapper::toDto)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public TransactionDTO updateTransaction(Long id, TransactionDTO updatedTransaction) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        if (updatedTransaction.userCategory() != null) {
            transaction.setUserCategory(updatedTransaction.userCategory());
        }
        if (updatedTransaction.isSubscription() != null) {
            transaction.setIsSubscription(updatedTransaction.isSubscription());
        }
        
        return DtoMapper.toDto(transactionRepository.save(transaction));
    }
}

