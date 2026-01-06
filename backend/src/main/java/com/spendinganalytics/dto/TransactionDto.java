package com.spendinganalytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {
  private Long id;
  private LocalDate transactionDate;
  private String merchant;
  private BigDecimal amount;
  private BigDecimal balance;
  private String transactionId;
  private Boolean isSubscription;
  private String rawDescription;
  private LocalDateTime importTimestamp;
}

