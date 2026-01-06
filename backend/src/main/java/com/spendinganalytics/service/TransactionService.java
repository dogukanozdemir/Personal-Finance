package com.spendinganalytics.service;

import com.spendinganalytics.dto.TransactionDto;
import com.spendinganalytics.entity.Transaction;
import com.spendinganalytics.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {

  @Autowired
  private TransactionRepository transactionRepository;

  public Page<TransactionDto> getTransactions(
      LocalDate startDate,
      LocalDate endDate,
      BigDecimal minAmount,
      BigDecimal maxAmount,
      String merchantSearch,
      int page,
      int size) {

    Specification<Transaction> spec = Specification.where(null);

    // Date range filter
    if (startDate != null) {
      Specification<Transaction> dateSpec =
          (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("transactionDate"), startDate);
      spec = spec.and(dateSpec);
    }
    if (endDate != null) {
      Specification<Transaction> dateSpec =
          (root, query, cb) -> cb.lessThanOrEqualTo(root.get("transactionDate"), endDate);
      spec = spec.and(dateSpec);
    }

    // Amount filters (only negative amounts for spending)
    // minAmount means minimum spending (e.g., 100 TL), which is -100 in the database
    // maxAmount means maximum spending (e.g., 1000 TL), which is -1000 in the database
    // For minAmount: we want amounts <= -minAmount (spending at least minAmount)
    // For maxAmount: we want amounts >= -maxAmount (spending at most maxAmount)
    if (minAmount != null) {
      Specification<Transaction> minSpec =
          (root, query, cb) ->
              cb.and(
                  cb.lessThanOrEqualTo(root.get("amount"), minAmount.negate()),
                  cb.lessThan(root.get("amount"), BigDecimal.ZERO));
      spec = spec.and(minSpec);
    }
    if (maxAmount != null) {
      Specification<Transaction> maxSpec =
          (root, query, cb) ->
              cb.and(
                  cb.greaterThanOrEqualTo(root.get("amount"), maxAmount.negate()),
                  cb.lessThan(root.get("amount"), BigDecimal.ZERO));
      spec = spec.and(maxSpec);
    }

    // Merchant search (case-insensitive)
    if (merchantSearch != null && !merchantSearch.trim().isEmpty()) {
      Specification<Transaction> merchantSpec =
          (root, query, cb) ->
              cb.like(
                  cb.lower(root.get("merchant")),
                  "%" + merchantSearch.toLowerCase() + "%");
      spec = spec.and(merchantSpec);
    }

    // Only spending transactions (negative amounts)
    Specification<Transaction> spendingSpec =
        (root, query, cb) -> cb.lessThan(root.get("amount"), BigDecimal.ZERO);
    spec = spec.and(spendingSpec);

    // Sort by date descending (newest first)
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate"));

    Page<Transaction> transactions = transactionRepository.findAll(spec, pageable);

    return transactions.map(this::toDto);
  }

  public List<String> getDistinctMerchants() {
    return transactionRepository.findAll().stream()
        .map(Transaction::getMerchant)
        .distinct()
        .sorted()
        .collect(Collectors.toList());
  }

  private TransactionDto toDto(Transaction transaction) {
    return new TransactionDto(
        transaction.getId(),
        transaction.getTransactionDate(),
        transaction.getMerchant(),
        transaction.getAmount(),
        transaction.getBalance(),
        transaction.getTransactionId(),
        transaction.getIsSubscription(),
        transaction.getRawDescription(),
        transaction.getImportTimestamp());
  }
}

