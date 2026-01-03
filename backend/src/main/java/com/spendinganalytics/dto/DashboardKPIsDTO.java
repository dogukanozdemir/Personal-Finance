package com.spendinganalytics.dto;

import java.math.BigDecimal;
import java.util.Map;

public record DashboardKPIsDTO(
    BigDecimal totalSpent,
    BigDecimal previousPeriodSpent,
    BigDecimal changePercent,
    BigDecimal avgPerDay,
    BigDecimal projectedMonthEnd,
    Map<String, Double> categories,
    String topCategory,
    Double topCategoryAmount
) {}

