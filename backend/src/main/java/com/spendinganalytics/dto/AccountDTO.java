package com.spendinganalytics.dto;

import java.time.LocalDateTime;

public record AccountDTO(
    Long id,
    String name,
    String type,
    String accountNumber,
    String iban,
    String currency,
    LocalDateTime createdAt
) {}

