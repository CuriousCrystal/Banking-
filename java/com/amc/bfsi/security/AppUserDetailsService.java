package com.amc.bfsi.security;

import com.amc.bfsi.entity.AppUser;
import com.amc.bfsi.repository.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository repository;

    public AppUserDetailsService(AppUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = repository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No user named " + username));

        return User.withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(normalizeRole(user.getRole()))   // roles() adds the ROLE_ prefix for us
                .build();
    }

    /** roles() rejects values that already start with ROLE_, so accept "ROLE_OFFICER" as well as "OFFICER". */
    private static String normalizeRole(String role) {
        String r = role == null ? "USER" : role.trim().toUpperCase();
        return r.startsWith("ROLE_") ? r.substring(5) : r;
    }
}
