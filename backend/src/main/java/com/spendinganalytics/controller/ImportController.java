package com.spendinganalytics.controller;

import com.spendinganalytics.dto.AccountDTO;
import com.spendinganalytics.dto.DeleteAllDataResultDTO;
import com.spendinganalytics.dto.ImportResultDTO;
import com.spendinganalytics.entity.Account;
import com.spendinganalytics.repository.AccountRepository;
import com.spendinganalytics.service.DataService;
import com.spendinganalytics.service.ImportService;
import com.spendinganalytics.util.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/import")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class ImportController {
    
    private final ImportService importService;
    private final AccountRepository accountRepository;
    private final DataService dataService;
    
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "accountId", required = false) Long accountId) {
        try {
            ImportResultDTO result = importService.importFile(file, accountId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error importing file: " + e.getMessage());
        }
    }
    
    @PostMapping("/upload-multiple")
    public ResponseEntity<?> uploadMultipleFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "accountId", required = false) Long accountId) {
        try {
            ImportResultDTO result = importService.importMultipleFiles(files, accountId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error importing files: " + e.getMessage());
        }
    }
    
    @GetMapping("/accounts")
    public ResponseEntity<List<AccountDTO>> getAccounts() {
        return ResponseEntity.ok(accountRepository.findAll().stream()
            .map(DtoMapper::toDto)
            .collect(Collectors.toList()));
    }
    
    @PostMapping("/accounts")
    public ResponseEntity<AccountDTO> createAccount(@RequestBody Account account) {
        Account saved = accountRepository.save(account);
        return ResponseEntity.ok(DtoMapper.toDto(saved));
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

