package com.amc.bfsi.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class AccountRequest {

    @NotNull(message = "Customer id is required")
    private Long customerId;

    @NotBlank(message = "Account type is required")
    @Pattern(regexp = "SAVINGS|CURRENT", message = "Account type must be SAVINGS or CURRENT")
    private String accountType;

    @NotNull(message = "Opening balance is required")
    @DecimalMin(value = "0.0", message = "Opening balance cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Balance allows 12 whole digits and 2 decimals")
    private BigDecimal balance;

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}
