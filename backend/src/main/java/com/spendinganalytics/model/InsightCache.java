package com.spendinganalytics.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "insights_cache")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsightCache {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "insight_type", nullable = false)
    private String insightType;
    
    @Column(nullable = false)
    private String title;
    
    @Column(length = 2000)
    private String description;
    
    private String severity; // 'low', 'medium', 'high'
    
    @Column(name = "transaction_ids", length = 5000)
    private String transactionIds; // JSON array
    
    @Column(name = "generated_at")
    private LocalDateTime generatedAt = LocalDateTime.now();
}

