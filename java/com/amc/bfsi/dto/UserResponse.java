package com.amc.bfsi.dto;

import com.amc.bfsi.entity.AppUser;

/** A user as the API shows it - the password hash is never sent back. */
public class UserResponse {

    private Long userId;
    private String username;
    private String role;

    public static UserResponse of(AppUser u) {
        UserResponse r = new UserResponse();
        r.userId = u.getUserId();
        r.username = u.getUsername();
        r.role = u.getRole();
        return r;
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
}
