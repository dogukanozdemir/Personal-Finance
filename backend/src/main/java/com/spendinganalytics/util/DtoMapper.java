package com.spendinganalytics.util;

import com.spendinganalytics.dto.*;
import com.spendinganalytics.entity.Account;
import com.spendinganalytics.entity.InsightCache;
import com.spendinganalytics.entity.Transaction;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DtoMapper {
    
    public AccountDTO toDto(Account account) {
        return new AccountDTO(
            account.getId(),
            account.getName(),
            account.getType(),
            account.getAccountNumber(),
            account.getIban(),
            account.getCurrency(),
            account.getCreatedAt()
        );
    }
    
    public TransactionDTO toDto(Transaction transaction) {
        return new TransactionDTO(
            transaction.getId(),
            transaction.getAccountId(),
            transaction.getTransactionDate(),
            transaction.getMerchant(),
            transaction.getAmount(),
            transaction.getBalance(),
            transaction.getTransactionId(),
            transaction.getCategory(),
            transaction.getUserCategory(),
            transaction.getIsSubscription(),
            transaction.getBonusPoints(),
            transaction.getRawDescription(),
            transaction.getImportTimestamp()
        );
    }
    
    public InsightCacheDTO toDto(InsightCache insightCache) {
        return new InsightCacheDTO(
            insightCache.getId(),
            insightCache.getInsightType(),
            insightCache.getTitle(),
            insightCache.getDescription(),
            insightCache.getSeverity(),
            insightCache.getGeneratedAt()
        );
    }
}

