package com.amc.bfsi.controller;

import com.amc.bfsi.dto.LoanRequest;
import com.amc.bfsi.dto.LoanResponse;
import com.amc.bfsi.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService service;

    public LoanController(LoanService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<LoanResponse> sanction(@Valid @RequestBody LoanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(LoanResponse.of(service.sanction(request)));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<LoanResponse> findAll() {
        return service.findAll().stream().map(LoanResponse::of).toList();
    }

    @GetMapping("/customer/{customerId}")
    @Transactional(readOnly = true)
    public List<LoanResponse> byCustomer(@PathVariable Long customerId) {
        return service.findByCustomer(customerId).stream().map(LoanResponse::of).toList();
    }
}
