package com.spendinganalytics.repository;

import com.spendinganalytics.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    Optional<Transaction> findByDedupHash(String dedupHash);
    
    List<Transaction> findByTransactionDateBetween(LocalDate start, LocalDate end);
    
    List<Transaction> findByAccountId(Long accountId);
    
    @Query("SELECT t FROM Transaction t WHERE t.transactionDate >= :startDate ORDER BY t.transactionDate DESC")
    List<Transaction> findRecentTransactions(@Param("startDate") LocalDate startDate);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.transactionDate BETWEEN :start AND :end AND t.amount < 0")
    BigDecimal getTotalSpendingBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
    
    @Query("SELECT t.category, SUM(t.amount) as total FROM Transaction t WHERE t.transactionDate BETWEEN :start AND :end AND t.amount < 0 GROUP BY t.category ORDER BY total ASC")
    List<Object[]> getSpendingByCategory(@Param("start") LocalDate start, @Param("end") LocalDate end);
    
    @Query("SELECT t.merchant, COUNT(t), AVG(t.amount) FROM Transaction t WHERE t.transactionDate >= :startDate AND t.amount < 0 GROUP BY t.merchant HAVING COUNT(t) >= 3")
    List<Object[]> findPotentialSubscriptions(@Param("startDate") LocalDate startDate);
    
    @Query("SELECT CASE WHEN CAST(FUNCTION('strftime', '%w', t.transactionDate) AS INTEGER) IN (0, 6) THEN 'Weekend' ELSE 'Weekday' END as period, SUM(t.amount) FROM Transaction t WHERE t.amount < 0 GROUP BY period")
    List<Object[]> getWeekendVsWeekdaySpending();
}

