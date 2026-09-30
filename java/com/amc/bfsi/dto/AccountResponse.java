package com.amc.bfsi.dto;

import com.amc.bfsi.entity.Account;

import java.time.LocalDate;

public class AccountResponse {

    private Long accountId;
    private String accountNumber;
    private String accountType;
    private Double balance;
    private LocalDate openedOn;
    private Long customerId;
    private String customerName;

    public static AccountResponse of(Account a) {
        AccountResponse r = new AccountResponse();
        r.accountId = a.getAccountId();
        r.accountNumber = a.getAccountNumber();
        r.accountType = a.getAccountType().name();
        r.balance = a.getBalance();
        r.openedOn = a.getOpenedOn();
        if (a.getCustomer() != null) {
            r.customerId = a.getCustomer().getCustomerId();
            r.customerName = a.getCustomer().getName();
        }
        return r;
    }

    public Long getAccountId() { return accountId; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountType() { return accountType; }
    public Double getBalance() { return balance; }
    public LocalDate getOpenedOn() { return openedOn; }
    public Long getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
}
