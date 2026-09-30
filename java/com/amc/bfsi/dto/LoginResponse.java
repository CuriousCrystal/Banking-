package com.amc.bfsi.dto;

import java.util.List;

public class LoginResponse {

    private String token;
    private String username;
    private List<String> roles;
    private long expiresInMs;
    private TenantResponse tenant;

    public LoginResponse(String token, String username, List<String> roles, long expiresInMs,
                         TenantResponse tenant) {
        this.token = token;
        this.username = username;
        this.roles = roles;
        this.expiresInMs = expiresInMs;
        this.tenant = tenant;
    }

    public String getToken() { return token; }
    public String getUsername() { return username; }
    public List<String> getRoles() { return roles; }
    public long getExpiresInMs() { return expiresInMs; }
    public TenantResponse getTenant() { return tenant; }
}
