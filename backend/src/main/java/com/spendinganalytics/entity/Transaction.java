package com.spendinganalytics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "account_id", nullable = false)
    private Long accountId;
    
    // Deduplication keys
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;
    
    @Column(nullable = false)
    private String merchant;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal balance;
    
    @Column(name = "transaction_id")
    private String transactionId; // Dekont No for debit
    
    // Categorization
    private String category;
    
    @Column(name = "user_category")
    private String userCategory;
    
    @Column(name = "is_subscription")
    private Boolean isSubscription = false;
    
    // Metadata
    @Column(name = "bonus_points", precision = 10, scale = 2)
    private BigDecimal bonusPoints;
    
    @Column(name = "raw_description", length = 1000)
    private String rawDescription;
    
    @Column(name = "import_timestamp")
    private LocalDateTime importTimestamp = LocalDateTime.now();
    
    // Composite dedup key
    @Column(name = "dedup_hash", nullable = false, unique = true)
    private String dedupHash;
}

