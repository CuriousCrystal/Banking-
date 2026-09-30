package com.amc.bfsi.dto;

import jakarta.validation.constraints.*;

public class AccountRequest {

    @NotNull(message = "Customer id is required")
    private Long customerId;

    @NotBlank(message = "Account type is required")
    @Pattern(regexp = "SAVINGS|CURRENT", message = "Account type must be SAVINGS or CURRENT")
    private String accountType;

    @NotNull(message = "Opening balance is required")
    @DecimalMin(value = "0.0", message = "Opening balance cannot be negative")
    private Double balance;

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }
}
