package com.spendinganalytics.dto;

import java.math.BigDecimal;

public record ProjectedMonthEndDto(BigDecimal projection, BigDecimal comparedPercentage) {
}
