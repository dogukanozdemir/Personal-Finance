package com.spendinganalytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransactionDTO(
    Long id,
    Long accountId,
    LocalDate transactionDate,
    String merchant,
    BigDecimal amount,
    BigDecimal balance,
    String transactionId,
    String category,
    String userCategory,
    Boolean isSubscription,
    BigDecimal bonusPoints,
    String rawDescription,
    LocalDateTime importTimestamp
) {}

