package com.amc.bfsi.service;

import com.amc.bfsi.dto.CustomerRequest;
import com.amc.bfsi.entity.Customer;
import com.amc.bfsi.exception.BusinessRuleException;
import com.amc.bfsi.exception.DuplicateResourceException;
import com.amc.bfsi.exception.ResourceNotFoundException;
import com.amc.bfsi.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository repository;

    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    public List<Customer> findAll(String city, String search) {
        if (search != null && !search.isBlank()) {
            return repository.search(search.trim());
        }
        if (city != null && !city.isBlank()) {
            return repository.findByCityIgnoreCase(city.trim());
        }
        return repository.findAll();
    }

    public Customer findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer " + id + " not found"));
    }

    @Transactional
    public Customer create(CustomerRequest request) {
        if (repository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("A customer with email "
                    + request.getEmail() + " already exists");
        }
        Customer customer = new Customer(request.getName(), request.getEmail(),
                request.getCity(), request.getPanNumber());
        return repository.save(customer);
    }

    @Transactional
    public Customer update(Long id, CustomerRequest request) {
        Customer customer = findById(id);

        repository.findByEmail(request.getEmail()).ifPresent(other -> {
            if (!other.getCustomerId().equals(id)) {
                throw new DuplicateResourceException("Email " + request.getEmail()
                        + " belongs to another customer");
            }
        });

        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setCity(request.getCity());
        customer.setPanNumber(request.getPanNumber());
        return repository.save(customer);
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = findById(id);

        boolean holdsMoney = customer.getAccounts().stream()
                .anyMatch(a -> a.getBalance() != null && a.getBalance() > 0);
        if (holdsMoney) {
            throw new BusinessRuleException(
                    "Customer still holds a balance - close the accounts first");
        }
        repository.delete(customer);
    }
}
