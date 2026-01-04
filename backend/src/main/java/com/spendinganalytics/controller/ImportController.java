package com.spendinganalytics.controller;

import com.spendinganalytics.dto.DeleteAllDataResultDTO;
import com.spendinganalytics.dto.TransactionImportResult;
import com.spendinganalytics.service.DataService;
import com.spendinganalytics.service.TransactionImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/import")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class ImportController {

  private final TransactionImportService transactionImportService;
  private final DataService dataService;

  @PostMapping
  public ResponseEntity<?> importTransactions(@RequestParam("files") MultipartFile[] files) {
    try {
      TransactionImportResult result = transactionImportService.importTransactions(files);
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("Error importing files: " + e.getMessage());
    }
  }

  @DeleteMapping("/delete-all")
  public ResponseEntity<DeleteAllDataResultDTO> deleteAllData() {
    try {
      DeleteAllDataResultDTO result = dataService.deleteAllData();
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      return ResponseEntity.internalServerError().build();
    }
  }
}
