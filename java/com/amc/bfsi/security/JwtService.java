package com.amc.bfsi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    /** Claim that carries the bank (tenant) the user logged in to. */
    private static final String TENANT_CLAIM = "tid";

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey key;

    /** A missing, non-base64 or too-short (< 32 bytes) secret stops the start-up, not the first login. */
    @PostConstruct
    void init() {
        key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public String generateToken(UserDetails user, String tenantId) {
        return Jwts.builder()
                .claims(Map.of(
                        "roles", user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList(),
                        TENANT_CLAIM, tenantId))
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    /** Checks signature and expiry in one pass; throws a JwtException for a bad or expired token. */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public static String tenantOf(Claims claims) {
        return claims.get(TENANT_CLAIM, String.class);
    }
}
