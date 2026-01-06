package com.spendinganalytics.service;

import com.spendinganalytics.dto.DashboardResponseDto;
import com.spendinganalytics.entity.Transaction;
import com.spendinganalytics.enums.DashboardPeriod;
import com.spendinganalytics.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

  private final TransactionRepository transactionRepository;
  private final StatisticsService statisticsService;

  public DashboardResponseDto getDashboard(DashboardPeriod period, Integer month, Integer year) {
    LocalDate today = LocalDate.now();

    LocalDate startDate = resolveStartDate(period, month, year, today);
    LocalDate endDate = resolveEndDate(period, month, year, today);

    LocalDate previousStartDate = resolvePreviousStartDate(period, startDate);
    LocalDate previousEndDate = resolvePreviousEndDate(period, previousStartDate, endDate);

    List<Transaction> current = transactionRepository.findSpendingBetween(startDate, endDate);
    List<Transaction> previous =
        transactionRepository.findSpendingBetween(previousStartDate, previousEndDate);

    BigDecimal totalSpent = statisticsService.totalSpent(current);
    BigDecimal previousSpent = statisticsService.totalSpent(previous);
    BigDecimal changePercent = statisticsService.changePercent(totalSpent, previousSpent);
    BigDecimal avgPerDay = statisticsService.averagePerActiveDay(totalSpent, current);

    BigDecimal projectedMonthEnd = null;
    BigDecimal projectedMonthEndComparedPercent = null;
    if (period == DashboardPeriod.THIS_MONTH) {
      List<Transaction> lastTwelveFullMonths = loadLastTwelveFullMonths(today);
      var projectionDto = statisticsService.projectedMonthEnd(today, current, lastTwelveFullMonths);
      projectedMonthEnd = projectionDto.projection();
      projectedMonthEndComparedPercent = projectionDto.comparedPercentage();
    }

    // Calculate overall average per day across all transactions
    List<Transaction> allTransactions = transactionRepository.findAll();
    BigDecimal overallTotalSpent = statisticsService.totalSpent(allTransactions);
    BigDecimal overallAvgPerDay = statisticsService.averagePerActiveDay(overallTotalSpent, allTransactions);

    // Calculate average monthly spend for YTD and YEAR
    BigDecimal avgMonthlySpend = null;
    if (period == DashboardPeriod.YTD || period == DashboardPeriod.YEAR) {
      Map<String, BigDecimal> monthlyDataPoints = statisticsService.dataPoints(current, startDate, endDate, period);
      if (!monthlyDataPoints.isEmpty()) {
        BigDecimal sum = monthlyDataPoints.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        avgMonthlySpend = sum.divide(BigDecimal.valueOf(monthlyDataPoints.size()), 2, java.math.RoundingMode.HALF_UP);
      }
    }

    Map<String, BigDecimal> dataPoints =
        statisticsService.dataPoints(current, startDate, endDate, period);

    return new DashboardResponseDto(
        totalSpent, previousSpent, changePercent, avgPerDay, projectedMonthEnd, projectedMonthEndComparedPercent, overallAvgPerDay, avgMonthlySpend, dataPoints);
  }

  private List<Transaction> loadLastTwelveFullMonths(LocalDate today) {
    YearMonth current = YearMonth.from(today);
    YearMonth start = current.minusMonths(12);
    YearMonth end = current.minusMonths(1);
    return transactionRepository.findSpendingBetween(start.atDay(1), end.atEndOfMonth());
  }

  private LocalDate resolveStartDate(
      DashboardPeriod period, Integer month, Integer year, LocalDate today) {
    return switch (period) {
      case THIS_MONTH -> today.withDayOfMonth(1);
      case MONTH -> YearMonth.of(resolveYear(year, today), resolveMonth(month, today)).atDay(1);
      case YTD -> LocalDate.of(resolveYear(year, today), 1, 1);
      case YEAR -> LocalDate.of(resolveYear(year, today), 1, 1);
    };
  }

  private LocalDate resolveEndDate(
      DashboardPeriod period, Integer month, Integer year, LocalDate today) {
    return switch (period) {
      case THIS_MONTH -> today;
      case MONTH -> {
        YearMonth selected = YearMonth.of(resolveYear(year, today), resolveMonth(month, today));
        yield selected.equals(YearMonth.from(today)) ? today : selected.atEndOfMonth();
      }
      case YTD -> {
        int y = resolveYear(year, today);
        yield (y == today.getYear()) ? today : LocalDate.of(y, 12, 31);
      }
      case YEAR -> {
        int y = resolveYear(year, today);
        yield LocalDate.of(y, 12, 31);
      }
    };
  }

  private LocalDate resolvePreviousStartDate(DashboardPeriod period, LocalDate currentStart) {
    return switch (period) {
      case THIS_MONTH, MONTH -> currentStart.minusMonths(1);
      case YTD, YEAR -> currentStart.minusYears(1);
    };
  }

  private LocalDate resolvePreviousEndDate(
      DashboardPeriod period, LocalDate previousStart, LocalDate currentEnd) {
    return switch (period) {
      case THIS_MONTH, MONTH -> previousStart.withDayOfMonth(previousStart.lengthOfMonth());
      case YTD, YEAR -> currentEnd.minusYears(1);
    };
  }

  private int resolveMonth(Integer month, LocalDate today) {
    if (month == null) return today.getMonthValue();
    if (month < 1 || month > 12) return today.getMonthValue();
    return month;
  }

  private int resolveYear(Integer year, LocalDate today) {
    if (year == null) return today.getYear();
    if (year <= 0) return today.getYear();
    return year;
  }
}
