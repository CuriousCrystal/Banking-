package com.amc.bfsi.dto;

import com.amc.bfsi.entity.Customer;

import java.util.List;

public class CustomerResponse {

    private Long customerId;
    private String name;
    private String email;
    private String city;
    private String panNumber;
    private int accountCount;
    private List<AccountResponse> accounts;
    private List<LoanResponse> loans;

    public static CustomerResponse summary(Customer c) {
        CustomerResponse r = new CustomerResponse();
        r.customerId = c.getCustomerId();
        r.name = c.getName();
        r.email = c.getEmail();
        r.city = c.getCity();
        r.panNumber = c.getPanNumber();
        r.accountCount = c.getAccounts() == null ? 0 : c.getAccounts().size();
        return r;
    }

    public static CustomerResponse full(Customer c) {
        CustomerResponse r = summary(c);
        r.accounts = c.getAccounts().stream().map(AccountResponse::of).toList();
        r.loans = c.getLoans().stream().map(LoanResponse::of).toList();
        return r;
    }

    public Long getCustomerId() { return customerId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getCity() { return city; }
    public String getPanNumber() { return panNumber; }
    public int getAccountCount() { return accountCount; }
    public List<AccountResponse> getAccounts() { return accounts; }
    public List<LoanResponse> getLoans() { return loans; }
}
