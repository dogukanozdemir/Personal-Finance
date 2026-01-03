package com.spendinganalytics.service;

import com.spendinganalytics.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {
    
    private final TransactionRepository transactionRepository;
    
    public Map<String, Object> getDashboardKPIs(String period) {
        Map<String, Object> kpis = new HashMap<>();
        
        LocalDate today = LocalDate.now();
        
        // Always use current month for "this period"
        LocalDate currentMonthStart = today.withDayOfMonth(1);
        LocalDate currentMonthEnd = today;
        
        // Previous period is always previous month
        LocalDate previousMonthStart = currentMonthStart.minusMonths(1);
        LocalDate previousMonthEnd = currentMonthStart.minusDays(1);
        
        // Total spending current month (from start of month to today)
        // Query already returns ABS(amount) and filters amount < 0, so result is positive
        BigDecimal currentSpending = transactionRepository.getTotalSpendingBetween(currentMonthStart, currentMonthEnd);
        if (currentSpending == null) currentSpending = BigDecimal.ZERO;
        kpis.put("totalSpent", currentSpending);
        
        // Total spending previous month
        BigDecimal previousSpending = transactionRepository.getTotalSpendingBetween(previousMonthStart, previousMonthEnd);
        if (previousSpending == null) previousSpending = BigDecimal.ZERO;
        kpis.put("previousPeriodSpent", previousSpending);
        
        // Calculate change percentage
        if (previousSpending.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal change = currentSpending.subtract(previousSpending)
                    .divide(previousSpending, 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            kpis.put("changePercent", change);
        } else {
            kpis.put("changePercent", BigDecimal.ZERO);
        }
        
        // Average per day (current month so far)
        long daysPassed = java.time.temporal.ChronoUnit.DAYS.between(currentMonthStart, currentMonthEnd) + 1;
        BigDecimal avgPerDay = daysPassed > 0 
            ? currentSpending.divide(BigDecimal.valueOf(daysPassed), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        kpis.put("avgPerDay", avgPerDay);
        
        // Projected month-end: based on average spending per day so far
        int daysInMonth = today.lengthOfMonth();
        BigDecimal projectedMonthEnd = avgPerDay.multiply(BigDecimal.valueOf(daysInMonth)).setScale(2, RoundingMode.HALF_UP);
        kpis.put("projectedMonthEnd", projectedMonthEnd.doubleValue());
        
        // Category breakdown (current month)
        List<Object[]> categoryData = transactionRepository.getSpendingByCategory(currentMonthStart, currentMonthEnd);
        Map<String, Double> categories = new HashMap<>();
        String topCategory = null;
        double topCategoryAmount = 0;
        
        for (Object[] data : categoryData) {
            String category = data[0] != null ? (String) data[0] : "Uncategorized";
            Double amount = ((BigDecimal) data[1]).abs().doubleValue();
            categories.put(category, amount);
            
            if (amount > topCategoryAmount) {
                topCategoryAmount = amount;
                topCategory = category;
            }
        }
        
        kpis.put("categories", categories);
        kpis.put("topCategory", topCategory);
        kpis.put("topCategoryAmount", topCategoryAmount);
        
        return kpis;
    }
}

