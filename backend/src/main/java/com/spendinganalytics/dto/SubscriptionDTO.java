package com.spendinganalytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionDTO(
    String merchant,
    BigDecimal averageAmount,
    Long transactionCount,
    String frequency,
    LocalDate lastTransactionDate,
    LocalDate firstTransactionDate,
    boolean active,
    Double amountVariance
) {}

