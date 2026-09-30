package com.amc.bfsi.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @Value("${bank.name}")
    private String bankName;

    @Value("${bank.branch-code}")
    private String branchCode;

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "bank", bankName,
                "branch", branchCode,
                "time", LocalDateTime.now().toString());
    }
}
