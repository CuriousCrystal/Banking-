package com.amc.bfsi.security;

import com.amc.bfsi.repository.TenantRepository;
import com.amc.bfsi.entity.Tenant;
import com.amc.bfsi.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TenantRepository tenantRepository;

    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService,
                         TenantRepository tenantRepository) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.tenantRepository = tenantRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            authenticate(header.substring(7), request);
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();      // the thread goes back to the pool - never leave a bank behind
        }
    }

    private void authenticate(String token, HttpServletRequest request) {
        try {
            Claims claims = jwtService.parse(token);
            String tenantId = JwtService.tenantOf(claims);
            if (tenantId == null || SecurityContextHolder.getContext().getAuthentication() != null) {
                return;                 // no bank in the token (issued before multi-tenancy): log in again
            }
            // one primary-key read per request; ponytail: add a short-lived cache if this ever shows up in profiles
            boolean active = tenantRepository.findById(tenantId)
                    .map(t -> t.getStatus() == Tenant.Status.ACTIVE).orElse(false);
            if (!active) {
                return;                 // unknown or suspended bank: the token is worthless
            }
            TenantContext.set(tenantId);
            UserDetails user = userDetailsService.loadUserByUsername(claims.getSubject());
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (Exception ignored) {
            // expired, tampered or unknown user - the request stays unauthenticated and ends as 401
            TenantContext.clear();
        }
    }
}
