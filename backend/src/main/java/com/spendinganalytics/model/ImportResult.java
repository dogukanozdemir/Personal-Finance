package com.spendinganalytics.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportResult {
    private int totalRows;
    private int newTransactions;
    private int duplicates;
    private String dateRangeStart;
    private String dateRangeEnd;
    private String accountName;
}

