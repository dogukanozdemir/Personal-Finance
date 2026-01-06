package com.spendinganalytics.controller;

import com.spendinganalytics.dto.TransactionDto;
import com.spendinganalytics.service.TransactionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {

  @Autowired
  private TransactionService transactionService;

  @GetMapping
  public ResponseEntity<Page<TransactionDto>> getTransactions(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate,
      @RequestParam(required = false) BigDecimal minAmount,
      @RequestParam(required = false) BigDecimal maxAmount,
      @RequestParam(required = false) String merchant,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    Page<TransactionDto> transactions =
        transactionService.getTransactions(
            startDate, endDate, minAmount, maxAmount, merchant, page, size);
    return ResponseEntity.ok(transactions);
  }

  @GetMapping("/merchants")
  public ResponseEntity<List<String>> getMerchants() {
    List<String> merchants = transactionService.getDistinctMerchants();
    return ResponseEntity.ok(merchants);
  }
}

