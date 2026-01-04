package com.spendinganalytics.controller;

import com.spendinganalytics.dto.DashboardResponseDto;
import com.spendinganalytics.enums.DashboardPeriod;
import com.spendinganalytics.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class DashboardController {

  private final DashboardService dashboardService;

  @GetMapping
  public ResponseEntity<DashboardResponseDto> getDashboard(
      @RequestParam(defaultValue = "THIS_MONTH") DashboardPeriod period,
      @RequestParam(required = false) Integer month,
      @RequestParam(required = false) Integer year) {
    return ResponseEntity.ok(dashboardService.getDashboard(period, month, year));
  }
}
