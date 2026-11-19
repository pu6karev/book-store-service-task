package com.epam.rd.autocode.spring.project.conf;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

// Issues and validates JWT tokens and builds the cookie that carries the token to the browser.
// The token contains only the subject (email), the issue time and the expiration time:
// roles are NOT stored in it, they are always loaded from the database (see JwtAuthenticationFilter).
@Slf4j
@Component
public class JwtService {

    public static final String COOKIE_NAME = "JWT";

    private final SecretKey key;
    private final Duration lifetime;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        // throws WeakKeyException when the secret is shorter than 32 bytes: the application fails at startup
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.lifetime = Duration.ofMinutes(expirationMinutes);
    }

    public String generateToken(String email) {
        Date now = new Date();
        log.debug("Issuing JWT for {}, valid for {} min", email, lifetime.toMinutes());
        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + lifetime.toMillis()))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    // Checks the signature and the expiration time and returns the email.
    // Throws a JwtException (ExpiredJwtException for an expired token) if the token is not valid.
    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // The cookie lives exactly as long as the token. Secure is not set on purpose:
    // the application runs on plain http locally, a Secure cookie would never be sent back.
    public ResponseCookie createCookie(String email) {
        return ResponseCookie.from(COOKIE_NAME, generateToken(email))
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(lifetime)
                .build();
    }
}
