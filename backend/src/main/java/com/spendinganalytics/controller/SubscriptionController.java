package com.spendinganalytics.controller;

import com.spendinganalytics.dto.SubscriptionDTO;
import com.spendinganalytics.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class SubscriptionController {
    
    private final SubscriptionService subscriptionService;
    
    /**
     * Get potential subscriptions that need user confirmation
     */
    @GetMapping("/potential")
    public ResponseEntity<List<SubscriptionDTO>> getPotentialSubscriptions() {
        return ResponseEntity.ok(subscriptionService.findPotentialSubscriptions());
    }
    
    /**
     * Get confirmed active subscriptions
     */
    @GetMapping("/active")
    public ResponseEntity<List<SubscriptionDTO>> getActiveSubscriptions() {
        return ResponseEntity.ok(subscriptionService.getActiveSubscriptions());
    }
    
    /**
     * Confirm a merchant as a subscription
     */
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, String>> confirmSubscription(@RequestBody Map<String, String> request) {
        String merchant = request.get("merchant");
        if (merchant == null || merchant.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Merchant name is required"));
        }
        
        subscriptionService.confirmAsSubscription(merchant);
        return ResponseEntity.ok(Map.of("message", "Subscription confirmed for " + merchant));
    }
    
    /**
     * Unmark a merchant as subscription
     */
    @PostMapping("/unmark")
    public ResponseEntity<Map<String, String>> unmarkSubscription(@RequestBody Map<String, String> request) {
        String merchant = request.get("merchant");
        if (merchant == null || merchant.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Merchant name is required"));
        }
        
        subscriptionService.unmarkAsSubscription(merchant);
        return ResponseEntity.ok(Map.of("message", "Subscription unmarked for " + merchant));
    }
}

