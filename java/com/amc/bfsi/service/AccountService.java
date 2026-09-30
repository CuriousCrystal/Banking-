package com.amc.bfsi.service;

import com.amc.bfsi.dto.AccountRequest;
import com.amc.bfsi.entity.Account;
import com.amc.bfsi.entity.AccountType;
import com.amc.bfsi.entity.Customer;
import com.amc.bfsi.exception.BusinessRuleException;
import com.amc.bfsi.exception.ResourceNotFoundException;
import com.amc.bfsi.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountService {

    private static final double SAVINGS_MINIMUM = 1000.0;

    private final AccountRepository accountRepository;
    private final CustomerService customerService;

    public AccountService(AccountRepository accountRepository, CustomerService customerService) {
        this.accountRepository = accountRepository;
        this.customerService = customerService;
    }

    public List<Account> findAll(Long customerId) {
        return customerId == null
                ? accountRepository.findAll()
                : accountRepository.findByCustomerCustomerId(customerId);
    }

    public Account findById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account " + id + " not found"));
    }

    @Transactional
    public Account open(AccountRequest request) {

        Customer customer = customerService.findById(request.getCustomerId());
        AccountType type = AccountType.valueOf(request.getAccountType());

        if (type == AccountType.SAVINGS && request.getBalance() < SAVINGS_MINIMUM) {
            throw new BusinessRuleException(
                    "A savings account needs an opening balance of at least " + SAVINGS_MINIMUM);
        }

        Account account = new Account(type, request.getBalance());
        customer.addAccount(account);

        Account saved = accountRepository.save(account);
        saved.setAccountNumber("AC" + (5000 + saved.getAccountId()));
        return accountRepository.save(saved);
    }

    @Transactional
    public void close(Long id) {
        Account account = findById(id);
        if (account.getBalance() != null && account.getBalance() != 0.0) {
            throw new BusinessRuleException(
                    "Account " + account.getAccountNumber() + " still has a balance of "
                            + account.getBalance());
        }
        accountRepository.delete(account);
    }

    @Transactional
    public Account deposit(Long id, double amount) {
        if (amount <= 0) {
            throw new BusinessRuleException("Deposit amount must be greater than zero");
        }
        Account account = findById(id);
        account.setBalance(account.getBalance() + amount);
        return accountRepository.save(account);
    }

    @Transactional
    public Account withdraw(Long id, double amount) {
        Account account = findById(id);
        if (amount <= 0) {
            throw new BusinessRuleException("Withdrawal amount must be greater than zero");
        }
        if (account.getAccountType() == AccountType.SAVINGS && account.getBalance() - amount < 0) {
            throw new BusinessRuleException("Insufficient balance");
        }
        account.setBalance(account.getBalance() - amount);
        return accountRepository.save(account);
    }
}
