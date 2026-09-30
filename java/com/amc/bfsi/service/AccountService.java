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

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountService {

    private static final BigDecimal SAVINGS_MINIMUM = new BigDecimal("1000.00");
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final AccountRepository accountRepository;
    private final CustomerService customerService;

    public AccountService(AccountRepository accountRepository, CustomerService customerService) {
        this.accountRepository = accountRepository;
        this.customerService = customerService;
    }

    public List<Account> findAll(Long customerId, String status) {
        List<Account> accounts = customerId == null
                ? accountRepository.findAll()
                : accountRepository.findByCustomerCustomerId(customerId);
        if (status != null && !status.isBlank()) {
            Account.AccountStatus wanted = Account.AccountStatus.valueOf(status.trim().toUpperCase());
            accounts = accounts.stream().filter(a -> a.getAccountStatus() == wanted).toList();
        }
        return accounts;
    }

    public Account findById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account " + id + " not found"));
    }

    @Transactional
    public Account open(AccountRequest request) {

        Customer customer = customerService.findById(request.getCustomerId());
        AccountType type = AccountType.valueOf(request.getAccountType());

        if (type == AccountType.SAVINGS && request.getBalance().compareTo(SAVINGS_MINIMUM) < 0) {
            throw new BusinessRuleException(
                    "A savings account needs an opening balance of at least " + SAVINGS_MINIMUM);
        }

        Account account = new Account(type, request.getBalance());
        account.setAccountNumber(generateAccountNumber());   // must exist before INSERT (NOT NULL column)
        customer.addAccount(account);

        return accountRepository.save(account);
    }

    /**
     * Unique human-readable number, decided before the row is inserted.
     * (The id does not exist yet at insert time, so "AC5000+id" cannot be used here.)
     */
    private String generateAccountNumber() {
        String number;
        do {
            number = "AC" + (100000 + java.util.concurrent.ThreadLocalRandom.current().nextInt(900000));
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }

    /**
     * Soft-close: the row is kept for audit with status CLOSED.
     * Only a zero-balance account can be closed.
     */
    @Transactional
    public void close(Long id) {
        Account account = findById(id);
        if (account.getBalance() != null && account.getBalance().compareTo(ZERO) != 0) {
            throw new BusinessRuleException(
                    "Account " + account.getAccountNumber() + " still has a balance of "
                            + account.getBalance() + " - withdraw or transfer it first");
        }
        if (account.getAccountStatus() == Account.AccountStatus.CLOSED) {
            throw new BusinessRuleException("Account " + account.getAccountNumber() + " is already closed");
        }
        account.setAccountStatus(Account.AccountStatus.CLOSED);
        accountRepository.save(account);
    }

    @Transactional
    public Account deposit(Long id, BigDecimal amount) {
        if (amount == null || amount.compareTo(ZERO) <= 0) {
            throw new BusinessRuleException("Deposit amount must be greater than zero");
        }
        Account account = findById(id);
        requireActive(account);
        account.setBalance(account.getBalance().add(amount));
        return accountRepository.save(account);
    }

    @Transactional
    public Account withdraw(Long id, BigDecimal amount) {
        if (amount == null || amount.compareTo(ZERO) <= 0) {
            throw new BusinessRuleException("Withdrawal amount must be greater than zero");
        }
        Account account = findById(id);
        requireActive(account);
        // no account type may go below zero (previously CURRENT could)
        if (account.getBalance().compareTo(amount) < 0) {
            throw new BusinessRuleException("Insufficient balance");
        }
        account.setBalance(account.getBalance().subtract(amount));
        return accountRepository.save(account);
    }

    private void requireActive(Account account) {
        if (account.getAccountStatus() != Account.AccountStatus.ACTIVE) {
            throw new BusinessRuleException("Account " + account.getAccountNumber()
                    + " is " + account.getAccountStatus() + " - transactions are not allowed");
        }
    }
}
