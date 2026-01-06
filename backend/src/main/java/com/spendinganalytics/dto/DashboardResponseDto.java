package com.spendinganalytics.dto;

import java.math.BigDecimal;
import java.util.Map;

public record DashboardResponseDto(
    BigDecimal totalSpent,
    BigDecimal previousPeriodSpent,
    BigDecimal changePercent,
    BigDecimal avgPerDay,
    BigDecimal projectedMonthEnd,
    BigDecimal projectedMonthEndComparedPercent,
    BigDecimal overallAvgPerDay,
    BigDecimal avgMonthlySpend,
    Map<String, BigDecimal> dataPoints) {}
