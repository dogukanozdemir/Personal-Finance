package com.spendinganalytics.dto;

public record DeleteAllDataResultDTO(
    long transactionsDeleted,
    long accountsDeleted,
    long insightsDeleted,
    long budgetsDeleted,
    long rulesDeleted
) {
    public long totalDeleted() {
        return transactionsDeleted + accountsDeleted + insightsDeleted + budgetsDeleted + rulesDeleted;
    }
}

