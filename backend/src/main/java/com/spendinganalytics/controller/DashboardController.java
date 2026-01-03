package com.spendinganalytics.controller;

import com.spendinganalytics.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "http://localhost:3000")
public class DashboardController {
    
    @Autowired
    private DashboardService dashboardService;
    
    @GetMapping("/kpis")
    public ResponseEntity<Map<String, Object>> getDashboardKPIs(
            @RequestParam(defaultValue = "month") String period) {
        return ResponseEntity.ok(dashboardService.getDashboardKPIs(period));
    }
}

