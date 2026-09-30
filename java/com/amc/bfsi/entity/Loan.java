package com.amc.bfsi.entity;

import jakarta.persistence.*;

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

    @Column(name = "principal_amount", nullable = false)
    private Double principalAmount;

    @Column(name = "interest_rate", nullable = false)
    private Double interestRate;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Column(name = "sanctioned_on")
    private LocalDate sanctionedOn = LocalDate.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    public Loan() { }

    public Loan(String loanType, Double principalAmount, Double interestRate, Integer tenureMonths) {
        this.loanType = loanType;
        this.principalAmount = principalAmount;
        this.interestRate = interestRate;
        this.tenureMonths = tenureMonths;
    }

    /** Standard reducing-balance EMI. */
    public double calculateEmi() {
        double monthlyRate = interestRate / 12 / 100;
        if (monthlyRate == 0) {
            return principalAmount / tenureMonths;
        }
        double factor = Math.pow(1 + monthlyRate, tenureMonths);
        return (principalAmount * monthlyRate * factor) / (factor - 1);
    }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }
    public String getLoanType() { return loanType; }
    public void setLoanType(String loanType) { this.loanType = loanType; }
    public Double getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(Double principalAmount) { this.principalAmount = principalAmount; }
    public Double getInterestRate() { return interestRate; }
    public void setInterestRate(Double interestRate) { this.interestRate = interestRate; }
    public Integer getTenureMonths() { return tenureMonths; }
    public void setTenureMonths(Integer tenureMonths) { this.tenureMonths = tenureMonths; }
    public LocalDate getSanctionedOn() { return sanctionedOn; }
    public void setSanctionedOn(LocalDate sanctionedOn) { this.sanctionedOn = sanctionedOn; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
}
