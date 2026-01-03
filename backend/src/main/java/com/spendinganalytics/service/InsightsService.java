package com.spendinganalytics.service;

import com.spendinganalytics.dto.InsightCacheDTO;
import com.spendinganalytics.entity.InsightCache;
import com.spendinganalytics.repository.InsightCacheRepository;
import com.spendinganalytics.repository.TransactionRepository;
import com.spendinganalytics.util.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InsightsService {
    
    private final TransactionRepository transactionRepository;
    private final InsightCacheRepository insightCacheRepository;
    
    public List<InsightCacheDTO> generateInsights() {
        List<InsightCache> insights = new ArrayList<>();
        
        // Clear old insights
        insightCacheRepository.deleteAll();
        
        // Find potential subscriptions
        LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);
        List<Object[]> subscriptions = transactionRepository.findPotentialSubscriptions(threeMonthsAgo);
        
        for (Object[] sub : subscriptions) {
            String merchant = (String) sub[0];
            Long count = (Long) sub[1];
            
            InsightCache insight = new InsightCache();
            insight.setInsightType("recurring_charge");
            insight.setTitle("Potential Subscription: " + merchant);
            insight.setDescription("Charged " + count + " times in the last 3 months");
            insight.setSeverity("medium");
            insights.add(insight);
        }
        
        // Weekend vs Weekday spending
        List<Object[]> weekendData = transactionRepository.getWeekendVsWeekdaySpending();
        if (!weekendData.isEmpty()) {
            Map<String, Double> spending = new HashMap<>();
            for (Object[] data : weekendData) {
                String period = (String) data[0];
                Double amount = ((Number) data[1]).doubleValue();
                spending.put(period, Math.abs(amount));
            }
            
            Double weekend = spending.getOrDefault("Weekend", 0.0);
            Double weekday = spending.getOrDefault("Weekday", 0.0);
            
            if (weekend > weekday * 1.5) {
                InsightCache insight = new InsightCache();
                insight.setInsightType("weekend_spending");
                insight.setTitle("High Weekend Spending");
                insight.setDescription(String.format("You spend %.0f%% more on weekends", 
                        ((weekend - weekday) / weekday) * 100));
                insight.setSeverity("medium");
                insights.add(insight);
            }
        }
        
        // Save all insights
        return insightCacheRepository.saveAll(insights).stream()
            .map(DtoMapper::toDto)
            .collect(Collectors.toList());
    }
    
    public List<InsightCacheDTO> getRecentInsights() {
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        List<InsightCache> recent = insightCacheRepository.findByGeneratedAtAfter(oneDayAgo);
        
        // If no recent insights, generate them
        if (recent.isEmpty()) {
            return generateInsights();
        }
        
        return recent.stream()
            .map(DtoMapper::toDto)
            .collect(Collectors.toList());
    }
}

