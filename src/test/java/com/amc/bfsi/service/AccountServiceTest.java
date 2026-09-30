package com.amc.bfsi.service;

import com.amc.bfsi.dto.AccountRequest;
import com.amc.bfsi.dto.CustomerRequest;
import com.amc.bfsi.entity.Account;
import com.amc.bfsi.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Service rules against a real (H2) database - the same rules Postman tests exercise,
 * but repeatable and part of the build.
 */
@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class AccountServiceTest {

    @Autowired AccountService accountService;
    @Autowired CustomerService customerService;

    private Long newCustomer() {
        CustomerRequest r = new CustomerRequest();
        r.setName("Test User");
        r.setEmail("test" + System.nanoTime() + "@example.com");
        r.setCity("Bangalore");
        r.setPanNumber("ABCDE1234F");
        return customerService.create(r).getCustomerId();
    }

    private Account openSavings(String balance) {
        AccountRequest r = new AccountRequest();
        r.setCustomerId(newCustomer());
        r.setAccountType("SAVINGS");
        r.setBalance(new BigDecimal(balance));
        return accountService.open(r);
    }

    @Test
    void depositAddsExactly() {
        Account a = openSavings("1000.10");
        Account saved = accountService.deposit(a.getAccountId(), new BigDecimal("0.20"));
        assertThat(saved.getBalance()).isEqualByComparingTo("1000.30");   // 0.10 + 0.20 == 0.30 with BigDecimal
    }

    @Test
    void withdrawReducesBalance() {
        Account a = openSavings("1500.00");
        Account saved = accountService.withdraw(a.getAccountId(), new BigDecimal("120.50"));
        assertThat(saved.getBalance()).isEqualByComparingTo("1379.50");
    }

    @Test
    void withdrawMoreThanBalanceIsRejected() {
        Account a = openSavings("1500.00");
        assertThatThrownBy(() -> accountService.withdraw(a.getAccountId(), new BigDecimal("2000.00")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Insufficient balance");
    }

    @Test
    void zeroOrNegativeDepositIsRejected() {
        Account a = openSavings("1000.00");
        assertThatThrownBy(() -> accountService.deposit(a.getAccountId(), BigDecimal.ZERO))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void savingsNeedsMinimumOpeningBalance() {
        AccountRequest r = new AccountRequest();
        r.setCustomerId(newCustomer());
        r.setAccountType("SAVINGS");
        r.setBalance(new BigDecimal("100.00"));          // below 1000
        assertThatThrownBy(() -> accountService.open(r))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("at least 1000");
    }

    @Test
    void closeWithBalanceIsRejected() {
        Account a = openSavings("1300.00");
        assertThatThrownBy(() -> accountService.close(a.getAccountId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("still has a balance");
    }

    private Account openCurrent(String balance) {
        AccountRequest r = new AccountRequest();
        r.setCustomerId(newCustomer());
        r.setAccountType("CURRENT");
        r.setBalance(new BigDecimal(balance));
        return accountService.open(r);
    }

    @Test
    void closeIsSoft_rowStaysButIsClosed() {
        Account a = openCurrent("0.00");          // CURRENT has no minimum, so zero balance is fine
        accountService.close(a.getAccountId());

        Account reloaded = accountService.findById(a.getAccountId());   // still found
        assertThat(reloaded.getAccountStatus()).isEqualTo(Account.AccountStatus.CLOSED);
        assertThat(reloaded.getAccountId()).isEqualTo(a.getAccountId());

        assertThatThrownBy(() -> accountService.deposit(a.getAccountId(), new BigDecimal("10.00")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CLOSED");
    }
}
