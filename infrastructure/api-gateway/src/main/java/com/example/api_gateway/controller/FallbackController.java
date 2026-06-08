package com.example.api_gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/user-service")
    public ResponseEntity<Map<String, Object>> userServiceFallback() {
        Map<String, Object> body = new HashMap<>();
        body.put("service", "user-service");
        body.put("message", "Service temporarily unavailable");
        body.put("status", 503);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @RequestMapping("/task-service")
    public ResponseEntity<Map<String, Object>> taskServiceFallback() {
        Map<String, Object> body = new HashMap<>();
        body.put("service", "task-service");
        body.put("message", "Service temporarily unavailable");
        body.put("status", 503);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
