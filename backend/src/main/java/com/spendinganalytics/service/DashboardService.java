package com.spendinganalytics.service;

import com.spendinganalytics.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    public Map<String, Object> getDashboardKPIs(String period) {
        Map<String, Object> kpis = new HashMap<>();
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate;
        LocalDate previousStart;
        LocalDate previousEnd;
        
        switch (period.toLowerCase()) {
            case "week":
                startDate = endDate.minusWeeks(1);
                previousStart = startDate.minusWeeks(1);
                previousEnd = startDate.minusDays(1);
                break;
            case "month":
                startDate = endDate.minusMonths(1);
                previousStart = startDate.minusMonths(1);
                previousEnd = startDate.minusDays(1);
                break;
            case "year":
                startDate = endDate.minusYears(1);
                previousStart = startDate.minusYears(1);
                previousEnd = startDate.minusDays(1);
                break;
            default: // today
                startDate = endDate;
                previousStart = endDate.minusDays(1);
                previousEnd = endDate.minusDays(1);
        }
        
        // Total spending current period
        BigDecimal currentSpending = transactionRepository.getTotalSpendingBetween(startDate, endDate);
        if (currentSpending == null) currentSpending = BigDecimal.ZERO;
        kpis.put("totalSpent", currentSpending.abs());
        
        // Total spending previous period
        BigDecimal previousSpending = transactionRepository.getTotalSpendingBetween(previousStart, previousEnd);
        if (previousSpending == null) previousSpending = BigDecimal.ZERO;
        kpis.put("previousPeriodSpent", previousSpending.abs());
        
        // Calculate change percentage
        if (previousSpending.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal change = currentSpending.subtract(previousSpending)
                    .divide(previousSpending.abs(), 2, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            kpis.put("changePercent", change);
        } else {
            kpis.put("changePercent", 0);
        }
        
        // Average per day
        long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        BigDecimal avgPerDay = currentSpending.abs().divide(BigDecimal.valueOf(days), 2, BigDecimal.ROUND_HALF_UP);
        kpis.put("avgPerDay", avgPerDay);
        
        // Category breakdown
        List<Object[]> categoryData = transactionRepository.getSpendingByCategory(startDate, endDate);
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
        
        // Projected month-end (if current period is this month)
        if ("month".equals(period.toLowerCase()) && startDate.getMonth() == endDate.getMonth()) {
            int daysInMonth = endDate.lengthOfMonth();
            int daysPassed = endDate.getDayOfMonth();
            BigDecimal projected = avgPerDay.multiply(BigDecimal.valueOf(daysInMonth));
            kpis.put("projectedMonthEnd", projected);
        }
        
        return kpis;
    }
}

