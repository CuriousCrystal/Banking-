package com.amc.bfsi.dto;

import com.amc.bfsi.entity.Loan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class LoanResponse {

    private Long loanId;
    private String loanType;
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private BigDecimal emi;
    private LocalDate sanctionedOn;
    private Long customerId;
    private String customerName;

    public static LoanResponse of(Loan l) {
        LoanResponse r = new LoanResponse();
        r.loanId = l.getLoanId();
        r.loanType = l.getLoanType();
        r.principalAmount = l.getPrincipalAmount();
        r.interestRate = l.getInterestRate();
        r.tenureMonths = l.getTenureMonths();
        r.emi = l.calculateEmi();                     // already 2dp, HALF_UP
        r.sanctionedOn = l.getSanctionedOn();
        if (l.getCustomer() != null) {
            r.customerId = l.getCustomer().getCustomerId();
            r.customerName = l.getCustomer().getName();
        }
        return r;
    }

    public Long getLoanId() { return loanId; }
    public String getLoanType() { return loanType; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public BigDecimal getInterestRate() { return interestRate; }
    public Integer getTenureMonths() { return tenureMonths; }
    public BigDecimal getEmi() { return emi; }
    public LocalDate getSanctionedOn() { return sanctionedOn; }
    public Long getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
}
