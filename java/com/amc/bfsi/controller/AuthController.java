package com.amc.bfsi.controller;

import com.amc.bfsi.dto.LoginRequest;
import com.amc.bfsi.dto.LoginResponse;
import com.amc.bfsi.dto.SignupRequest;
import com.amc.bfsi.dto.TenantResponse;
import com.amc.bfsi.entity.Tenant;
import com.amc.bfsi.security.JwtService;
import com.amc.bfsi.service.TenantService;
import com.amc.bfsi.tenant.TenantContext;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final TenantService tenantService;

    /** Bank code assumed when the client sends none - set in dev/demo profiles only. */
    @Value("${app.tenancy.default-slug:}")
    private String defaultTenant;

    public AuthController(AuthenticationManager authManager,
                          UserDetailsService userDetailsService,
                          JwtService jwtService,
                          TenantService tenantService) {
        this.authManager = authManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.tenantService = tenantService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        String tenantId = request.getTenant() == null || request.getTenant().isBlank()
                ? defaultTenant : request.getTenant().trim().toLowerCase();

        // unknown bank, suspended bank and wrong password all look the same from the outside
        Tenant tenant = tenantId.isBlank() ? null : tenantService.findActive(tenantId).orElse(null);
        if (tenant == null) {
            return unauthorized();
        }

        Authentication authentication;
        try {
            // the user lookup behind authenticate() is tenant-scoped, so it can only find this bank's users
            authentication = TenantContext.callAs(tenant.getId(), () -> authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())));
        } catch (BadCredentialsException e) {
            return unauthorized();
        }

        return ResponseEntity.ok(loginResponse(tenant, (UserDetails) authentication.getPrincipal()));
    }

    /** Self-service onboarding: creates a new bank with its first ADMIN and signs that admin in. */
    @PostMapping("/signup")
    public ResponseEntity<LoginResponse> signup(@Valid @RequestBody SignupRequest request) {
        Tenant tenant = tenantService.signup(request);
        UserDetails admin = TenantContext.callAs(tenant.getId(),
                () -> userDetailsService.loadUserByUsername(request.getUsername().trim()));
        return ResponseEntity.status(HttpStatus.CREATED).body(loginResponse(tenant, admin));
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        return Map.of(
                "username", auth.getName(),
                "roles", auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList(),
                "tenant", tenantService.current());
    }

    private LoginResponse loginResponse(Tenant tenant, UserDetails user) {
        return new LoginResponse(
                jwtService.generateToken(user, tenant.getId()),
                user.getUsername(),
                user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList(),
                jwtService.getExpirationMs(),
                TenantResponse.summary(tenant));
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(401).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 401,
                "error", "Unauthorized",
                "message", "Invalid bank code, username or password",
                "errors", Map.of()));
    }
}
