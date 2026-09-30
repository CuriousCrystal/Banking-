package com.amc.bfsi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** What the admin sends to create a user. The password arrives in plain text and is hashed by UserService. */
public class UserRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "[a-zA-Z0-9._-]+", message = "Username may contain letters, digits, dot, dash and underscore only")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 72, message = "Password must be between 6 and 72 characters")   // BCrypt reads at most 72 bytes
    private String password;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "ADMIN|OFFICER|USER", message = "Role must be ADMIN, OFFICER or USER")
    private String role;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
