package com.spendinganalytics.controller;

import com.spendinganalytics.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class DashboardController {
    
    private final DashboardService dashboardService;
    
    @GetMapping("/kpis")
    public ResponseEntity<Map<String, Object>> getDashboardKPIs(
            @RequestParam(defaultValue = "month") String period) {
        return ResponseEntity.ok(dashboardService.getDashboardKPIs(period));
    }
}

