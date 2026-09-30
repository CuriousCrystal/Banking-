package com.amc.bfsi.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Table(name = "loan")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loan_id")
    private Long loanId;

    @Column(name = "loan_type", nullable = false, length = 20)
    private String loanType;

    @Column(name = "principal_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Column(name = "sanctioned_on")
    private LocalDate sanctionedOn = LocalDate.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    public Loan() { }

    public Loan(String loanType, BigDecimal principalAmount, BigDecimal interestRate, Integer tenureMonths) {
        this.loanType = loanType;
        this.principalAmount = principalAmount;
        this.interestRate = interestRate;
        this.tenureMonths = tenureMonths;
    }

    /**
     * Standard reducing-balance EMI: P·r·(1+r)^n / ((1+r)^n - 1), rounded to paise.
     * High-precision intermediate math (DECIMAL64), exact half-up rounding at the end.
     */
    public BigDecimal calculateEmi() {
        BigDecimal monthlyRate = interestRate.divide(BigDecimal.valueOf(1200), MathContext.DECIMAL64);
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principalAmount.divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
        }
        // pow on BigDecimal is exponent-limited, so use the double bridge for the
        // (1+r)^n factor - precision loss there is far below one paisa for bank ranges
        double factor = Math.pow(BigDecimal.ONE.add(monthlyRate).doubleValue(), tenureMonths);
        BigDecimal bigFactor = BigDecimal.valueOf(factor);
        BigDecimal numerator = principalAmount.multiply(monthlyRate).multiply(bigFactor);
        BigDecimal denominator = bigFactor.subtract(BigDecimal.ONE);
        return numerator.divide(denominator, MathContext.DECIMAL64).setScale(2, RoundingMode.HALF_UP);
    }

    /** Total money paid back: EMI x tenure. */
    public BigDecimal totalPayable() {
        return calculateEmi().multiply(BigDecimal.valueOf(tenureMonths)).setScale(2, RoundingMode.HALF_UP);
    }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }
    public String getLoanType() { return loanType; }
    public void setLoanType(String loanType) { this.loanType = loanType; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public Integer getTenureMonths() { return tenureMonths; }
    public void setTenureMonths(Integer tenureMonths) { this.tenureMonths = tenureMonths; }
    public LocalDate getSanctionedOn() { return sanctionedOn; }
    public void setSanctionedOn(LocalDate sanctionedOn) { this.sanctionedOn = sanctionedOn; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
}
