package com.spendinganalytics.controller;

import com.spendinganalytics.model.InsightCache;
import com.spendinganalytics.service.InsightsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/insights")
@CrossOrigin(origins = "http://localhost:3000")
public class InsightsController {
    
    @Autowired
    private InsightsService insightsService;
    
    @GetMapping
    public ResponseEntity<List<InsightCache>> getInsights() {
        return ResponseEntity.ok(insightsService.getRecentInsights());
    }
    
    @PostMapping("/generate")
    public ResponseEntity<List<InsightCache>> generateInsights() {
        return ResponseEntity.ok(insightsService.generateInsights());
    }
}

