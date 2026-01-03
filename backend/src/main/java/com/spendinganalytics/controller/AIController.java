package com.spendinganalytics.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "http://localhost:3000")
public class AIController {
    
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        
        // Simple rule-based responses for MVP
        Map<String, Object> response = new HashMap<>();
        response.put("answer", "AI analysis feature coming soon. Currently using rule-based insights.");
        response.put("citations", new String[]{});
        
        return ResponseEntity.ok(response);
    }
}

