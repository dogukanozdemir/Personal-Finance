package com.spendinganalytics.service;

import com.spendinganalytics.dto.DeleteAllDataResultDTO;
import com.spendinganalytics.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DataService {

  private final TransactionRepository transactionRepository;

  @Transactional
  public DeleteAllDataResultDTO deleteAllData() {
    long transactionCount = transactionRepository.count();
    transactionRepository.deleteAll();
    return new DeleteAllDataResultDTO(transactionCount);
  }
}
