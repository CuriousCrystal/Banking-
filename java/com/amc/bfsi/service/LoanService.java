package com.amc.bfsi.service;

import com.amc.bfsi.dto.LoanRequest;
import com.amc.bfsi.entity.Customer;
import com.amc.bfsi.entity.Loan;
import com.amc.bfsi.exception.BusinessRuleException;
import com.amc.bfsi.exception.ResourceNotFoundException;
import com.amc.bfsi.repository.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

@Service
public class LoanService {

    private static final int MAX_ACTIVE_LOANS = 3;
    private static final BigDecimal MIN_HOLDING_RATIO = new BigDecimal("0.10");

    private final LoanRepository loanRepository;
    private final CustomerService customerService;

    public LoanService(LoanRepository loanRepository, CustomerService customerService) {
        this.loanRepository = loanRepository;
        this.customerService = customerService;
    }

    public List<Loan> findAll() {
        return loanRepository.findAll();
    }

    public List<Loan> findByCustomer(Long customerId) {
        customerService.findById(customerId);          // 404 if the customer is unknown
        return loanRepository.findByCustomerCustomerId(customerId);
    }

    public Loan findById(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan " + id + " not found"));
    }

    @Transactional
    public Loan sanction(LoanRequest request) {

        Customer customer = customerService.findById(request.getCustomerId());

        long active = loanRepository.countByCustomerCustomerId(customer.getCustomerId());
        if (active >= MAX_ACTIVE_LOANS) {
            throw new BusinessRuleException(
                    "Customer already has " + active + " active loans");
        }

        BigDecimal holding = customer.getAccounts().stream()
                .map(a -> a.getBalance() == null ? BigDecimal.ZERO : a.getBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal required = request.getPrincipalAmount().multiply(MIN_HOLDING_RATIO);
        if (holding.compareTo(required) < 0) {
            throw new BusinessRuleException(
                    "Customer must hold at least 10% of the loan amount across accounts");
        }

        Loan loan = new Loan(request.getLoanType(), request.getPrincipalAmount(),
                request.getInterestRate(), request.getTenureMonths());
        customer.addLoan(loan);

        return loanRepository.save(loan);
    }
}
