package com.spendinganalytics.controller;

import com.spendinganalytics.model.Account;
import com.spendinganalytics.model.ImportResult;
import com.spendinganalytics.repository.AccountRepository;
import com.spendinganalytics.service.ImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/import")
@CrossOrigin(origins = "http://localhost:3000")
public class ImportController {
    
    @Autowired
    private ImportService importService;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "accountId", required = false) Long accountId) {
        try {
            ImportResult result = importService.importFile(file, accountId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error importing file: " + e.getMessage());
        }
    }
    
    @GetMapping("/accounts")
    public ResponseEntity<List<Account>> getAccounts() {
        return ResponseEntity.ok(accountRepository.findAll());
    }
    
    @PostMapping("/accounts")
    public ResponseEntity<Account> createAccount(@RequestBody Account account) {
        Account saved = accountRepository.save(account);
        return ResponseEntity.ok(saved);
    }
}

