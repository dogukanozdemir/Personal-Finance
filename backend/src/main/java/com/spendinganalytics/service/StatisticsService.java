package com.spendinganalytics.service;

import com.spendinganalytics.dto.ProjectedMonthEndDto;
import com.spendinganalytics.entity.Transaction;
import com.spendinganalytics.enums.DashboardPeriod;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class StatisticsService {

  private static final int MONEY_SCALE = 2;
  private static final int INTERNAL_SCALE = 4;
  private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

  private static final BigDecimal ZERO = BigDecimal.ZERO;
  private static final BigDecimal ONE = BigDecimal.ONE;

  private static final BigDecimal MIN_FRACTION = new BigDecimal("0.02");
  private static final BigDecimal MAX_FRACTION = new BigDecimal("0.98");

  private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

  public BigDecimal totalSpent(List<Transaction> transactions) {
    return transactions.stream()
        .map(Transaction::getAmount)
        .map(BigDecimal::abs)
        .reduce(ZERO, BigDecimal::add)
        .setScale(MONEY_SCALE, ROUNDING_MODE);
  }

  public BigDecimal changePercent(BigDecimal currentTotal, BigDecimal previousTotal) {
    if (previousTotal.compareTo(ZERO) <= 0) {
      return ZERO.setScale(MONEY_SCALE, ROUNDING_MODE);
    }
    return currentTotal
        .subtract(previousTotal)
        .divide(previousTotal, INTERNAL_SCALE, ROUNDING_MODE)
        .multiply(BigDecimal.valueOf(100))
        .setScale(MONEY_SCALE, ROUNDING_MODE);
  }

  public BigDecimal averagePerActiveDay(BigDecimal totalSpent, List<Transaction> transactions) {
    long activeDays = transactions.stream().map(Transaction::getTransactionDate).distinct().count();

    if (activeDays <= 0) {
      return ZERO.setScale(MONEY_SCALE, ROUNDING_MODE);
    }

    return totalSpent.divide(BigDecimal.valueOf(activeDays), MONEY_SCALE, ROUNDING_MODE);
  }

  /** Data points for charts: - THIS_MONTH and MONTH => daily totals - YTD and YEAR => monthly totals */
  public Map<String, BigDecimal> dataPoints(
      List<Transaction> transactions,
      LocalDate startDate,
      LocalDate endDate,
      DashboardPeriod period) {
    return (period == DashboardPeriod.YTD || period == DashboardPeriod.YEAR)
        ? monthlyTotals(transactions, startDate, endDate)
        : dailyTotals(transactions, startDate, endDate);
  }

  public ProjectedMonthEndDto projectedMonthEnd(
      LocalDate asOfDate,
      List<Transaction> currentMonthTransactions,
      List<Transaction> lastTwelveFullMonthsTransactions) {
    BigDecimal spentSoFar = totalSpent(currentMonthTransactions);

    int dayNumber = asOfDate.getDayOfMonth();
    int totalDaysInMonth = asOfDate.lengthOfMonth();

    Map<YearMonth, BigDecimal> monthTotal =
        lastTwelveFullMonthsTransactions.stream()
            .collect(
                Collectors.groupingBy(
                    t -> YearMonth.from(t.getTransactionDate()),
                    Collectors.reducing(ZERO, t -> t.getAmount().abs(), BigDecimal::add)));

    List<YearMonth> nonZeroMonths =
        monthTotal.entrySet().stream()
            .filter(e -> e.getValue().compareTo(ZERO) > 0)
            .map(Map.Entry::getKey)
            .sorted()
            .toList();

    BigDecimal usualMonthlySpending =
        averageMonthlyTotal(monthTotal, nonZeroMonths).setScale(MONEY_SCALE, ROUNDING_MODE);

    BigDecimal projected;
    if (nonZeroMonths.size() < 3) {
      projected = paceProjection(spentSoFar, dayNumber, totalDaysInMonth);
      BigDecimal comparedPercentage = calculateComparisonPercentage(projected, usualMonthlySpending);
      return new ProjectedMonthEndDto(projected, comparedPercentage);
    } else {
      BigDecimal usualFraction =
          averageFractionSpentByDay(
              lastTwelveFullMonthsTransactions, monthTotal, nonZeroMonths, dayNumber);

      if (usualFraction.compareTo(ZERO) <= 0) {
        projected = paceProjection(spentSoFar, dayNumber, totalDaysInMonth);
      } else {
        usualFraction = clamp(usualFraction, MIN_FRACTION, MAX_FRACTION);

        BigDecimal impliedMonthTotal = spentSoFar.divide(usualFraction, INTERNAL_SCALE, ROUNDING_MODE);

        BigDecimal usualSpendingSoFar =
            usualMonthlySpending.multiply(usualFraction).setScale(INTERNAL_SCALE, ROUNDING_MODE);

        BigDecimal speedFactor =
            (usualSpendingSoFar.compareTo(ZERO) > 0)
                ? spentSoFar
                    .setScale(INTERNAL_SCALE, ROUNDING_MODE)
                    .divide(usualSpendingSoFar, INTERNAL_SCALE, ROUNDING_MODE)
                : ONE;

        BigDecimal correctedImplied = impliedMonthTotal.multiply(speedFactor);

        BigDecimal trustWeight =
            BigDecimal.valueOf(dayNumber)
                .divide(BigDecimal.valueOf(totalDaysInMonth), INTERNAL_SCALE, ROUNDING_MODE);

        projected =
            ONE.subtract(trustWeight)
                .multiply(usualMonthlySpending)
                .add(trustWeight.multiply(correctedImplied));
        projected = projected.setScale(MONEY_SCALE, ROUNDING_MODE);
      }
    }

    BigDecimal comparedPercentage = calculateComparisonPercentage(projected, usualMonthlySpending);

    return new ProjectedMonthEndDto(projected, comparedPercentage);
  }

  private Map<String, BigDecimal> dailyTotals(
      List<Transaction> transactions, LocalDate startDate, LocalDate endDate) {
    Map<LocalDate, BigDecimal> dailyTotals =
        transactions.stream()
            .collect(
                Collectors.groupingBy(
                    Transaction::getTransactionDate,
                    Collectors.reducing(ZERO, t -> t.getAmount().abs(), BigDecimal::add)));

    Map<String, BigDecimal> result = new LinkedHashMap<>();
    for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
      BigDecimal amount = dailyTotals.getOrDefault(d, ZERO);
      result.put(d.format(DAY_FORMAT), amount.setScale(MONEY_SCALE, ROUNDING_MODE));
    }
    return result;
  }

  private Map<String, BigDecimal> monthlyTotals(
      List<Transaction> transactions, LocalDate startDate, LocalDate endDate) {
    Map<YearMonth, BigDecimal> monthlyTotals =
        transactions.stream()
            .collect(
                Collectors.groupingBy(
                    t -> YearMonth.from(t.getTransactionDate()),
                    Collectors.reducing(ZERO, t -> t.getAmount().abs(), BigDecimal::add)));

    Map<String, BigDecimal> result = new LinkedHashMap<>();
    YearMonth current = YearMonth.from(startDate);
    YearMonth last = YearMonth.from(endDate);

    while (!current.isAfter(last)) {
      BigDecimal amount = monthlyTotals.getOrDefault(current, ZERO);
      result.put(current.format(MONTH_FORMAT), amount.setScale(MONEY_SCALE, ROUNDING_MODE));
      current = current.plusMonths(1);
    }
    return result;
  }

  private BigDecimal averageMonthlyTotal(
      Map<YearMonth, BigDecimal> monthTotal, List<YearMonth> months) {
    BigDecimal sum = ZERO;
    for (YearMonth m : months) {
      sum = sum.add(monthTotal.getOrDefault(m, ZERO));
    }
    return sum.divide(BigDecimal.valueOf(months.size()), INTERNAL_SCALE, ROUNDING_MODE);
  }

  private BigDecimal averageFractionSpentByDay(
      List<Transaction> history,
      Map<YearMonth, BigDecimal> monthTotal,
      List<YearMonth> nonZeroMonths,
      int dayNumber) {
    Map<YearMonth, Map<LocalDate, BigDecimal>> dailyByMonth =
        history.stream()
            .collect(
                Collectors.groupingBy(
                    t -> YearMonth.from(t.getTransactionDate()),
                    Collectors.groupingBy(
                        Transaction::getTransactionDate,
                        Collectors.reducing(ZERO, t -> t.getAmount().abs(), BigDecimal::add))));

    BigDecimal sumFractions = ZERO;
    int count = 0;

    for (YearMonth month : nonZeroMonths) {
      BigDecimal total = monthTotal.getOrDefault(month, ZERO);
      if (total.compareTo(ZERO) <= 0) continue;

      int comparisonDay = Math.min(dayNumber, month.lengthOfMonth());
      LocalDate cutOff = month.atDay(comparisonDay);

      Map<LocalDate, BigDecimal> dailyTotals = dailyByMonth.getOrDefault(month, Map.of());

      BigDecimal cumulative = ZERO;
      for (var entry : dailyTotals.entrySet()) {
        if (!entry.getKey().isAfter(cutOff)) {
          cumulative = cumulative.add(entry.getValue());
        }
      }

      BigDecimal fraction =
          cumulative
              .setScale(INTERNAL_SCALE, ROUNDING_MODE)
              .divide(total, INTERNAL_SCALE, ROUNDING_MODE);

      sumFractions = sumFractions.add(fraction);
      count++;
    }

    if (count == 0) return ZERO;
    return sumFractions.divide(BigDecimal.valueOf(count), INTERNAL_SCALE, ROUNDING_MODE);
  }

  private BigDecimal paceProjection(BigDecimal spentSoFar, int dayNumber, int totalDaysInMonth) {
    return spentSoFar
        .divide(BigDecimal.valueOf(dayNumber), INTERNAL_SCALE, ROUNDING_MODE)
        .multiply(BigDecimal.valueOf(totalDaysInMonth))
        .setScale(MONEY_SCALE, ROUNDING_MODE);
  }

  private BigDecimal calculateComparisonPercentage(
      BigDecimal projection, BigDecimal averageMonthlySpending) {
    if (averageMonthlySpending.compareTo(ZERO) <= 0) {
      return ZERO.setScale(MONEY_SCALE, ROUNDING_MODE);
    }
    return projection
        .subtract(averageMonthlySpending)
        .divide(averageMonthlySpending, INTERNAL_SCALE, ROUNDING_MODE)
        .multiply(BigDecimal.valueOf(100))
        .setScale(MONEY_SCALE, ROUNDING_MODE);
  }

  private BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max) {
    if (value.compareTo(min) < 0) return min;
    if (value.compareTo(max) > 0) return max;
    return value;
  }
}
