package com.amc.bfsi.dto;

import java.util.List;
import java.util.Map;

public class SummaryResponse {

    private long customers;
    private long accounts;
    private long activeLoans;
    private double totalDeposits;
    private List<Map<String, Object>> loansByType;
    private List<Map<String, Object>> depositsByCity;

    public SummaryResponse(long customers, long accounts, long activeLoans, double totalDeposits,
                           List<Map<String, Object>> loansByType,
                           List<Map<String, Object>> depositsByCity) {
        this.customers = customers;
        this.accounts = accounts;
        this.activeLoans = activeLoans;
        this.totalDeposits = totalDeposits;
        this.loansByType = loansByType;
        this.depositsByCity = depositsByCity;
    }

    public long getCustomers() { return customers; }
    public long getAccounts() { return accounts; }
    public long getActiveLoans() { return activeLoans; }
    public double getTotalDeposits() { return totalDeposits; }
    public List<Map<String, Object>> getLoansByType() { return loansByType; }
    public List<Map<String, Object>> getDepositsByCity() { return depositsByCity; }
}
