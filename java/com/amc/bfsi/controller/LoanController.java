package com.amc.bfsi.controller;

import com.amc.bfsi.dto.LoanRequest;
import com.amc.bfsi.dto.LoanResponse;
import com.amc.bfsi.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /** Full EMI breakdown for one loan: monthly instalment, total interest, total payable. */
    @GetMapping("/{id}/emi")
    @Transactional(readOnly = true)
    public Map<String, Object> emi(@PathVariable Long id) {
        var loan = service.findById(id);
        BigDecimal emi = loan.calculateEmi();
        BigDecimal totalPayable = loan.totalPayable();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("loanId", loan.getLoanId());
        out.put("loanType", loan.getLoanType());
        out.put("principalAmount", loan.getPrincipalAmount());
        out.put("interestRate", loan.getInterestRate());
        out.put("tenureMonths", loan.getTenureMonths());
        out.put("monthlyEmi", emi);
        out.put("totalPayable", totalPayable);
        out.put("totalInterest", totalPayable.subtract(loan.getPrincipalAmount()));
        return out;
    }
}
