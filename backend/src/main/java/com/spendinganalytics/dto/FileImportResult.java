package com.spendinganalytics.dto;

import java.util.List;

public record FileImportResult(
    String fileName, int rowsParsed, int inserted, int skippedDuplicates, List<String> errors) {}
