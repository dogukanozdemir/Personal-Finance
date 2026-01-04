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
    if (period == DashboardPeriod.THIS_MONTH) {
      List<Transaction> lastTwelveFullMonths = loadLastTwelveFullMonths(today);
      projectedMonthEnd = statisticsService.projectedMonthEnd(today, current, lastTwelveFullMonths);
    }

    Map<String, BigDecimal> dataPoints =
        statisticsService.dataPoints(current, startDate, endDate, period);

    return new DashboardResponseDto(
        totalSpent, previousSpent, changePercent, avgPerDay, projectedMonthEnd, dataPoints);
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
    };
  }

  private LocalDate resolvePreviousStartDate(DashboardPeriod period, LocalDate currentStart) {
    return switch (period) {
      case THIS_MONTH, MONTH -> currentStart.minusMonths(1);
      case YTD -> currentStart.minusYears(1);
    };
  }

  private LocalDate resolvePreviousEndDate(
      DashboardPeriod period, LocalDate previousStart, LocalDate currentEnd) {
    return switch (period) {
      case THIS_MONTH, MONTH -> previousStart.withDayOfMonth(previousStart.lengthOfMonth());
      case YTD -> currentEnd.minusYears(1);
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
