package com.spendinganalytics.service;

import com.spendinganalytics.dto.SubscriptionDTO;
import com.spendinganalytics.entity.Transaction;
import com.spendinganalytics.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionService {
    
    private final TransactionRepository transactionRepository;
    
    /**
     * Find potential subscriptions that need user confirmation
     * Criteria:
     * - Same merchant appears 3+ times in last 6 months
     * - Amount variance < 20% (consistent pricing)
     * - Regular intervals (monthly/weekly/quarterly pattern)
     */
    @Transactional(readOnly = true)
    public List<SubscriptionDTO> findPotentialSubscriptions() {
        LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);
        List<Transaction> allTransactions = transactionRepository.findByTransactionDateBetween(
            sixMonthsAgo, LocalDate.now()
        );
        
        // Group by merchant
        Map<String, List<Transaction>> byMerchant = allTransactions.stream()
            .filter(t -> t.getAmount() != null && t.getAmount().compareTo(BigDecimal.ZERO) != 0)
            .collect(Collectors.groupingBy(Transaction::getMerchant));
        
        List<SubscriptionDTO> potentialSubs = new ArrayList<>();
        
        for (Map.Entry<String, List<Transaction>> entry : byMerchant.entrySet()) {
            List<Transaction> transactions = entry.getValue();
            
            // Need at least 3 transactions
            if (transactions.size() < 3) continue;
            
            // Calculate statistics
            BigDecimal total = transactions.stream()
                .map(t -> t.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal avgAmount = total.divide(
                BigDecimal.valueOf(transactions.size()), 
                2, 
                RoundingMode.HALF_UP
            );
            
            // Calculate variance (standard deviation as % of average)
            double variance = calculateVariance(transactions, avgAmount);
            double variancePercent = (variance / avgAmount.doubleValue()) * 100;
            
            // Skip if variance is too high (>20%)
            if (variancePercent > 20) continue;
            
            // Check for subscription keywords in merchant name
            String merchantLower = entry.getKey().toLowerCase();
            boolean hasSubscriptionKeywords = merchantLower.contains("subscription") ||
                merchantLower.contains("premium") ||
                merchantLower.contains("pro") ||
                merchantLower.contains("plus") ||
                merchantLower.contains("membership") ||
                merchantLower.contains("plan") ||
                merchantLower.contains("abonelik") ||
                merchantLower.contains("üyelik");
            
            // Analyze frequency pattern
            String frequency = detectFrequency(transactions);
            
            // Check if active (transaction in last 60 days)
            LocalDate lastDate = transactions.stream()
                .map(Transaction::getTransactionDate)
                .max(LocalDate::compareTo)
                .orElse(LocalDate.MIN);
            
            LocalDate firstDate = transactions.stream()
                .map(Transaction::getTransactionDate)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.MIN);
            
            boolean isActive = lastDate.isAfter(LocalDate.now().minusDays(60));
            
            SubscriptionDTO sub = new SubscriptionDTO(
                entry.getKey(),
                avgAmount,
                (long) transactions.size(),
                frequency,
                lastDate,
                firstDate,
                isActive,
                variancePercent
            );
            
            potentialSubs.add(sub);
        }
        
        // Sort by transaction count (most frequent first)
        potentialSubs.sort((a, b) -> b.transactionCount().compareTo(a.transactionCount()));
        
        return potentialSubs;
    }
    
    private double calculateVariance(List<Transaction> transactions, BigDecimal average) {
        double avg = average.doubleValue();
        double sumSquaredDiff = transactions.stream()
            .mapToDouble(t -> Math.pow(t.getAmount().abs().doubleValue() - avg, 2))
            .sum();
        return Math.sqrt(sumSquaredDiff / transactions.size());
    }
    
    private String detectFrequency(List<Transaction> transactions) {
        if (transactions.size() < 2) return "Unknown";
        
        // Sort by date
        List<Transaction> sorted = transactions.stream()
            .sorted(Comparator.comparing(Transaction::getTransactionDate))
            .collect(Collectors.toList());
        
        // Calculate intervals between transactions
        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < sorted.size(); i++) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(
                sorted.get(i-1).getTransactionDate(),
                sorted.get(i).getTransactionDate()
            );
            intervals.add(days);
        }
        
        // Calculate average interval
        double avgInterval = intervals.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0);
        
        // Determine frequency based on average interval
        if (avgInterval >= 25 && avgInterval <= 35) {
            return "Monthly";
        } else if (avgInterval >= 6 && avgInterval <= 9) {
            return "Weekly";
        } else if (avgInterval >= 85 && avgInterval <= 95) {
            return "Quarterly";
        } else if (avgInterval >= 28 && avgInterval <= 32) {
            return "Monthly";
        } else {
            return "Irregular";
        }
    }
    
    /**
     * Mark a transaction as confirmed subscription
     */
    @Transactional
    public void confirmAsSubscription(String merchant) {
        List<Transaction> transactions = transactionRepository.findByTransactionDateBetween(
            LocalDate.now().minusMonths(12), 
            LocalDate.now()
        );
        
        transactions.stream()
            .filter(t -> t.getMerchant().equals(merchant))
            .forEach(t -> {
                t.setIsSubscription(true);
                transactionRepository.save(t);
            });
    }
    
    /**
     * Unmark a merchant as subscription
     */
    @Transactional
    public void unmarkAsSubscription(String merchant) {
        List<Transaction> transactions = transactionRepository.findByTransactionDateBetween(
            LocalDate.now().minusMonths(12), 
            LocalDate.now()
        );
        
        transactions.stream()
            .filter(t -> t.getMerchant().equals(merchant))
            .forEach(t -> {
                t.setIsSubscription(false);
                transactionRepository.save(t);
            });
    }
    
    /**
     * Get confirmed active subscriptions
     */
    @Transactional(readOnly = true)
    public List<SubscriptionDTO> getActiveSubscriptions() {
        LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);
        List<Transaction> subscriptionTransactions = transactionRepository.findByTransactionDateBetween(
            sixMonthsAgo, LocalDate.now()
        ).stream()
        .filter(t -> Boolean.TRUE.equals(t.getIsSubscription()))
        .collect(Collectors.toList());
        
        // Group by merchant
        Map<String, List<Transaction>> byMerchant = subscriptionTransactions.stream()
            .collect(Collectors.groupingBy(Transaction::getMerchant));
        
        List<SubscriptionDTO> subscriptions = new ArrayList<>();
        
        for (Map.Entry<String, List<Transaction>> entry : byMerchant.entrySet()) {
            List<Transaction> transactions = entry.getValue();
            
            BigDecimal avgAmount = transactions.stream()
                .map(t -> t.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(transactions.size()), 2, RoundingMode.HALF_UP);
            
            LocalDate lastDate = transactions.stream()
                .map(Transaction::getTransactionDate)
                .max(LocalDate::compareTo)
                .orElse(LocalDate.MIN);
            
            LocalDate firstDate = transactions.stream()
                .map(Transaction::getTransactionDate)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.MIN);
            
            SubscriptionDTO sub = new SubscriptionDTO(
                entry.getKey(),
                avgAmount,
                (long) transactions.size(),
                detectFrequency(transactions),
                lastDate,
                firstDate,
                lastDate.isAfter(LocalDate.now().minusDays(60)),
                null
            );
            
            subscriptions.add(sub);
        }
        
        return subscriptions;
    }
}

