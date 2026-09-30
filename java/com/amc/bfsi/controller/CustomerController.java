package com.amc.bfsi.controller;

import com.amc.bfsi.dto.CustomerRequest;
import com.amc.bfsi.dto.CustomerResponse;
import com.amc.bfsi.entity.Customer;
import com.amc.bfsi.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll(@RequestParam(required = false) String city,
                                          @RequestParam(required = false) String search) {
        return service.findAll(city, search).stream().map(CustomerResponse::summary).toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public CustomerResponse findById(@PathVariable Long id) {
        return CustomerResponse.full(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        Customer saved = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(CustomerResponse.summary(saved));
    }

    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return CustomerResponse.summary(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
