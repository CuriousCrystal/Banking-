package com.amc.bfsi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Self-service onboarding: a new bank plus its first ADMIN user. */
public class SignupRequest {

    @NotBlank(message = "Bank name is required")
    @Size(min = 3, max = 100, message = "Bank name must be between 3 and 100 characters")
    private String bankName;

    @NotBlank(message = "Bank code is required")
    @Pattern(regexp = "[a-z0-9][a-z0-9-]{1,38}[a-z0-9]",
             message = "Bank code must be 3 to 40 lowercase letters, digits or dashes and cannot start or end with a dash")
    private String bankCode;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "[a-zA-Z0-9._-]+", message = "Username may contain letters, digits, dot, dash and underscore only")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")   // BCrypt reads at most 72 bytes
    private String password;

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
