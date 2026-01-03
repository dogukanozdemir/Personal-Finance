package com.spendinganalytics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String type; // 'debit' or 'credit'
    
    @Column(name = "account_number")
    private String accountNumber;
    
    private String iban;
    
    @Column(nullable = false)
    private String currency = "TRY";
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}

