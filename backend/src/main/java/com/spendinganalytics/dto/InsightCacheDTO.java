package com.spendinganalytics.dto;

import java.time.LocalDateTime;

public record InsightCacheDTO(
    Long id,
    String insightType,
    String title,
    String description,
    String severity,
    LocalDateTime generatedAt
) {}

