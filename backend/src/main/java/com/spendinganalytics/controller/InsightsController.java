package com.spendinganalytics.controller;

import com.spendinganalytics.dto.InsightCacheDTO;
import com.spendinganalytics.service.InsightsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/insights")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class InsightsController {
    
    private final InsightsService insightsService;
    
    @GetMapping
    public ResponseEntity<List<InsightCacheDTO>> getInsights() {
        return ResponseEntity.ok(insightsService.getRecentInsights());
    }
    
    @PostMapping("/generate")
    public ResponseEntity<List<InsightCacheDTO>> generateInsights() {
        return ResponseEntity.ok(insightsService.generateInsights());
    }
}

