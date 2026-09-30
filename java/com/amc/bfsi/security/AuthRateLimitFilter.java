package com.amc.bfsi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Throttles the two public endpoints an attacker can hammer: login (password guessing) and signup
 * (bank spam). Over the limit the client gets 429 with the same JSON error shape as everything else.
 *
 * ponytail: in-memory fixed window per client IP - right for one instance. Behind several replicas
 * swap the map for a shared counter (Redis / bucket4j).
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000;

    private final int limitPerMinute;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(@Value("${app.auth.rate-limit-per-minute}") int limitPerMinute) {
        this.limitPerMinute = limitPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return !"POST".equals(request.getMethod())
                || !(path.equals("/api/auth/login") || path.equals("/api/auth/signup"));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        if (allow(request.getRemoteAddr())) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(429);
        response.setHeader("Retry-After", "60");
        response.setContentType("application/json");
        response.getWriter().write("{\"timestamp\":\"" + LocalDateTime.now()
                + "\",\"status\":429,\"error\":\"Too Many Requests\","
                + "\"message\":\"Too many attempts - please wait a minute and try again\",\"errors\":{}}");
    }

    private boolean allow(String client) {
        long now = System.currentTimeMillis();
        if (windows.size() > 10_000) {                       // keep the map from growing without bound
            windows.values().removeIf(w -> now - w.start > WINDOW_MS);
        }
        Window window = windows.compute(client,
                (k, current) -> current == null || now - current.start > WINDOW_MS ? new Window(now) : current);
        return window.count.incrementAndGet() <= limitPerMinute;
    }

    private static final class Window {
        final long start;
        final AtomicInteger count = new AtomicInteger();

        Window(long start) {
            this.start = start;
        }
    }
}
