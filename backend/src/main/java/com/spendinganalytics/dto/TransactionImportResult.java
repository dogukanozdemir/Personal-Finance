package com.spendinganalytics.dto;

import java.util.List;

public record TransactionImportResult(
    int totalFiles,
    int totalRowsParsed,
    int totalInserted,
    int totalSkippedDuplicates,
    List<FileImportResult> fileResults) {}
