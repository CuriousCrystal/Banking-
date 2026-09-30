package com.amc.bfsi.controller;

import com.amc.bfsi.dto.AccountRequest;
import com.amc.bfsi.dto.AccountResponse;
import com.amc.bfsi.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<AccountResponse> findAll(@RequestParam(required = false) Long customerId) {
        return service.findAll(customerId).stream().map(AccountResponse::of).toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public AccountResponse findById(@PathVariable Long id) {
        return AccountResponse.of(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<AccountResponse> open(@Valid @RequestBody AccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AccountResponse.of(service.open(request)));
    }

    @PatchMapping("/{id}/deposit")
    public AccountResponse deposit(@PathVariable Long id, @RequestBody Map<String, Double> body) {
        return AccountResponse.of(service.deposit(id, body.getOrDefault("amount", 0.0)));
    }

    @PatchMapping("/{id}/withdraw")
    public AccountResponse withdraw(@PathVariable Long id, @RequestBody Map<String, Double> body) {
        return AccountResponse.of(service.withdraw(id, body.getOrDefault("amount", 0.0)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> close(@PathVariable Long id) {
        service.close(id);
        return ResponseEntity.noContent().build();
    }
}
