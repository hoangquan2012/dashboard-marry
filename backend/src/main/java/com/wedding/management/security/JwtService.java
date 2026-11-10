package com.wedding.management.security;

import com.wedding.management.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class JwtService {

    private final SecretKey key;
    private final Duration expiration;
    private final String issuer;

    public JwtService(AppProperties properties, Environment environment) {
        AppProperties.Jwt jwt = properties.getJwt();
        this.expiration = Duration.ofMinutes(jwt.getExpirationMinutes());
        this.issuer = jwt.getIssuer();
        this.key = resolveKey(jwt.getSecret(), environment);
    }

    private static SecretKey resolveKey(String secret, Environment environment) {
        boolean prod = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (secret == null || secret.isBlank()) {
            if (prod) {
                throw new IllegalStateException("APP_JWT_SECRET must be configured in production (at least 32 characters)");
            }
            byte[] random = new byte[64];
            new SecureRandom().nextBytes(random);
            log.warn("APP_JWT_SECRET is not configured - a random secret was generated. "
                    + "All sessions will be invalidated when the application restarts.");
            return Keys.hmacShaKeyFor(random);
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(secret);
            if (bytes.length < 32) {
                bytes = secret.getBytes(StandardCharsets.UTF_8);
            }
        } catch (IllegalArgumentException e) {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (bytes.length < 32) {
            throw new IllegalStateException("APP_JWT_SECRET must be at least 32 bytes long");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    public String generateToken(String username, String role, String fullName) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .claims(Map.of("role", role, "name", fullName))
                .signWith(key)
                .compact();
    }

    public long expiresInSeconds() {
        return expiration.toSeconds();
    }

    public Optional<Claims> parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).requireIssuer(issuer).build()
                    .parseSignedClaims(token).getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
