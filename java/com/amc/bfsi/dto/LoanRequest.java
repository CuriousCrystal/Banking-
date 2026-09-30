package com.amc.bfsi.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class LoanRequest {

    @NotNull(message = "Customer id is required")
    private Long customerId;

    @NotBlank(message = "Loan type is required")
    @Pattern(regexp = "HOME|CAR|PERSONAL", message = "Loan type must be HOME, CAR or PERSONAL")
    private String loanType;

    @NotNull(message = "Principal is required")
    @DecimalMin(value = "10000.0", message = "Minimum loan amount is 10000")
    @Digits(integer = 12, fraction = 2, message = "Principal allows 12 whole digits and 2 decimals")
    private BigDecimal principalAmount;

    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "1.0", message = "Interest rate must be at least 1")
    @DecimalMax(value = "30.0", message = "Interest rate cannot exceed 30")
    private BigDecimal interestRate;

    @NotNull(message = "Tenure is required")
    @Min(value = 6, message = "Minimum tenure is 6 months")
    @Max(value = 360, message = "Maximum tenure is 360 months")
    private Integer tenureMonths;

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getLoanType() { return loanType; }
    public void setLoanType(String loanType) { this.loanType = loanType; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public Integer getTenureMonths() { return tenureMonths; }
    public void setTenureMonths(Integer tenureMonths) { this.tenureMonths = tenureMonths; }
}
