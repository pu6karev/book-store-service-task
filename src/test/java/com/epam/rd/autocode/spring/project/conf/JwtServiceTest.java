package com.epam.rd.autocode.spring.project.conf;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Plain unit test, no Spring: the service is created directly with a secret and a lifetime.
class JwtServiceTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-bytes-long";
    private static final String OTHER_SECRET = "another-secret-key-that-is-also-32-bytes-long";
    private static final String EMAIL = "client1@example.com";
    private static final String EMPLOYEE_EMAIL = "john.doe@email.com";
    private static final long LIFETIME_MINUTES = 60;

    private final JwtService jwtService = new JwtService(SECRET, LIFETIME_MINUTES);

    // ---------- generateToken / extractEmail ----------

    @Test
    void extractEmail_whenTokenIsFreshlyGenerated_shouldReturnSameEmail() {
        String token = jwtService.generateToken(EMAIL);

        assertEquals(EMAIL, jwtService.extractEmail(token));
    }

    @Test
    void generateToken_shouldContainOnlySubjectIssuedAtAndExpiration() {
        Claims claims = parseWithTestKey(jwtService.generateToken(EMAIL));

        // no roles, no password: authorities are always loaded from the database
        assertEquals(Set.of("sub", "iat", "exp"), claims.keySet());
        assertEquals(Duration.ofMinutes(LIFETIME_MINUTES),
                Duration.between(claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant()));
    }

    @Test
    void extractEmail_whenTokenIsExpired_shouldThrowExpiredJwtException() {
        // a negative lifetime gives a token that is already expired, no waiting is needed
        String expiredToken = new JwtService(SECRET, -1).generateToken(EMAIL);

        assertThrows(ExpiredJwtException.class, () -> jwtService.extractEmail(expiredToken));
    }

    @Test
    void extractEmail_whenSignedWithAnotherSecret_shouldThrowJwtException() {
        String foreignToken = new JwtService(OTHER_SECRET, LIFETIME_MINUTES).generateToken(EMAIL);

        assertThrows(JwtException.class, () -> jwtService.extractEmail(foreignToken));
    }

    @Test
    void extractEmail_whenPayloadIsTampered_shouldThrowJwtException() {
        // an attacker keeps the header and the signature but puts another email into the payload
        String[] parts = jwtService.generateToken(EMAIL).split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"sub\":\"" + EMPLOYEE_EMAIL + "\",\"exp\":9999999999}").getBytes(StandardCharsets.UTF_8));
        String tamperedToken = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThrows(JwtException.class, () -> jwtService.extractEmail(tamperedToken));
    }

    @Test
    void extractEmail_whenTokenIsUnsigned_shouldThrowJwtException() {
        // "alg: none" attack: a token without a signature must never be accepted
        String unsignedToken = Jwts.builder().subject(EMPLOYEE_EMAIL).compact();

        assertThrows(JwtException.class, () -> jwtService.extractEmail(unsignedToken));
    }

    @Test
    void extractEmail_whenStringIsNotAToken_shouldThrowJwtException() {
        assertThrows(JwtException.class, () -> jwtService.extractEmail("not-a-jwt"));
    }

    // ---------- constructor ----------

    @Test
    void constructor_whenSecretIsTooShort_shouldThrowWeakKeyException() {
        assertThrows(WeakKeyException.class, () -> new JwtService("short-secret", LIFETIME_MINUTES));
    }

    // ---------- createCookie ----------

    @Test
    void createCookie_shouldBeHttpOnlyLaxAndCarryValidToken() {
        ResponseCookie cookie = jwtService.createCookie(EMAIL);

        assertEquals(JwtService.COOKIE_NAME, cookie.getName());
        assertTrue(cookie.isHttpOnly());
        assertEquals("Lax", cookie.getSameSite());
        assertEquals("/", cookie.getPath());
        assertEquals(Duration.ofMinutes(LIFETIME_MINUTES), cookie.getMaxAge());
        assertEquals(EMAIL, jwtService.extractEmail(cookie.getValue()));
    }

    // ---------- helpers ----------

    // parses a token with the same key as the service, to look inside it
    private static Claims parseWithTestKey(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
