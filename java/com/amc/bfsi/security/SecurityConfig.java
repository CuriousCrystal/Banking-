package com.amc.bfsi.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AuthRateLimitFilter rateLimitFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    // the H2 console exists only in the h2 profile - never open it anywhere else
    @Value("${spring.h2.console.enabled:false}")
    private boolean h2Console;

    // set (production) = health probes and Prometheus live on a separate internal port that is not published
    @Value("${management.server.port:}")
    private String managementPort;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, AuthRateLimitFilter rateLimitFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsSource()))
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers("/api/auth/login", "/api/auth/signup", "/api/health", "/error").permitAll();
                auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                // API docs: these paths only exist when springdoc is enabled (dev, h2)
                auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                if (h2Console) {
                    auth.requestMatchers("/h2-console/**").permitAll();
                }
                if (managementPort.isBlank()) {
                    // same port as the API: only the orchestrator probes are public, metrics are not
                    auth.requestMatchers("/actuator/health/**", "/actuator/info").permitAll();
                    auth.requestMatchers("/actuator/**").denyAll();
                } else {
                    auth.requestMatchers("/actuator/**").permitAll();
                }
                auth.requestMatchers("/api/users/**").hasRole("ADMIN");
                auth.requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN");
                auth.requestMatchers("/api/loans/**").hasAnyRole("ADMIN", "OFFICER");
                auth.anyRequest().authenticated();
            })
            .headers(headers -> {
                if (h2Console) {
                    headers.frameOptions(frame -> frame.sameOrigin());   // the console renders inside frames
                }
            })
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // no token / expired token -> 401 (the React app then sends the user back to login)
            // logged in but wrong role  -> 403, both in the same JSON shape as GlobalExceptionHandler
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) ->
                    writeError(res, 401, "Unauthorized", "Please sign in - token missing or expired"))
                .accessDeniedHandler((req, res, e) ->
                    writeError(res, 403, "Forbidden", "Your role is not allowed to perform this action")))
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static void writeError(jakarta.servlet.http.HttpServletResponse res, int status,
                                   String error, String message) throws java.io.IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.getWriter().write("{\"timestamp\":\"" + java.time.LocalDateTime.now()
                + "\",\"status\":" + status + ",\"error\":\"" + error
                + "\",\"message\":\"" + message + "\",\"errors\":{}}");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsSource() {
        CorsConfiguration config = new CorsConfiguration();
        // patterns allow any port, e.g. localhost:5173 or 5174 when Vite picks the next free port
        config.setAllowedOriginPatterns(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
