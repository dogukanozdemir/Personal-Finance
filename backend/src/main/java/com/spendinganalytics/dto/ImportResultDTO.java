package com.spendinganalytics.dto;

public record ImportResultDTO(
    int totalRows,
    int newTransactions,
    int duplicates,
    String dateRangeStart,
    String dateRangeEnd,
    String accountName
) {}

