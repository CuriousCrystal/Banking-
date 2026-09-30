package com.amc.bfsi.dto;

import java.util.List;

public class LoginResponse {

    private String token;
    private String username;
    private List<String> roles;
    private long expiresInMs;

    public LoginResponse(String token, String username, List<String> roles, long expiresInMs) {
        this.token = token;
        this.username = username;
        this.roles = roles;
        this.expiresInMs = expiresInMs;
    }

    public String getToken() { return token; }
    public String getUsername() { return username; }
    public List<String> getRoles() { return roles; }
    public long getExpiresInMs() { return expiresInMs; }
}
