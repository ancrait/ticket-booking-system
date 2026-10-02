package com.sorokaandriy.auth_service.security;

import com.sorokaandriy.auth_service.entity.Role;
import com.sorokaandriy.auth_service.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String ACCESS_SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String REFRESH_SECRET = "fedcba9876543210fedcba9876543210fedcba9876543210";
    private static final String FOREIGN_SECRET = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final long ACCESS_EXPIRATION = 900_000L;
    private static final long REFRESH_EXPIRATION = 604_800_000L;
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final JwtService jwtService = new JwtService(
            ACCESS_SECRET, REFRESH_SECRET, ACCESS_EXPIRATION, REFRESH_EXPIRATION);

    private User user() {
        return User.builder()
                .id(USER_ID)
                .email("user@example.com")
                .password("encoded-password")
                .firstName("John")
                .lastName("Doe")
                .phone("+380501234567")
                .role(Role.ORGANIZER)
                .emailVerified(true)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void generateAccessToken_containsCorrectClaims() {
        String token = jwtService.generateAccessToken(user());

        Claims claims = jwtService.parseAccessToken(token);

        assertThat(claims.getSubject()).isEqualTo(USER_ID.toString());
        assertThat(claims.get("email", String.class)).isEqualTo("user@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("ORGANIZER");
        assertThat(claims.get("type", String.class)).isEqualTo("ACCESS");
        assertThat(claims.getExpiration().getTime()).isGreaterThan(claims.getIssuedAt().getTime());
    }

    @Test
    void generateRefreshToken_containsCorrectClaims() {
        String token = jwtService.generateRefreshToken(user());

        Claims claims = jwtService.parseRefreshToken(token);

        assertThat(claims.getSubject()).isEqualTo(USER_ID.toString());
        assertThat(claims.get("type", String.class)).isEqualTo("REFRESH");
        assertThat(claims.get("email", String.class)).isNull();
    }

    @Test
    void accessAndRefreshTokens_areDifferent() {
        User user = user();

        assertThat(jwtService.generateAccessToken(user))
                .isNotEqualTo(jwtService.generateRefreshToken(user));
    }

    @Test
    void isAccessTokenValid_returnsTrueForValidToken() {
        assertThat(jwtService.isAccessTokenValid(jwtService.generateAccessToken(user()))).isTrue();
    }

    @Test
    void isRefreshTokenValid_returnsTrueForValidToken() {
        assertThat(jwtService.isRefreshTokenValid(jwtService.generateRefreshToken(user()))).isTrue();
    }

    @Test
    void isAccessTokenValid_returnsFalseForGarbageToken() {
        assertThat(jwtService.isAccessTokenValid("not-a-token")).isFalse();
    }

    @Test
    void isRefreshTokenValid_returnsFalseForAccessToken() {
        String accessToken = jwtService.generateAccessToken(user());

        assertThat(jwtService.isRefreshTokenValid(accessToken)).isFalse();
    }

    @Test
    void isAccessTokenValid_returnsFalseForExpiredToken() {
        JwtService expiredService = new JwtService(
                ACCESS_SECRET, REFRESH_SECRET, -1_000L, -1_000L);

        String expiredToken = expiredService.generateAccessToken(user());

        assertThat(expiredService.isAccessTokenValid(expiredToken)).isFalse();
    }

    @Test
    void parseRefreshToken_throwsForTokenSignedWithDifferentKey() {
        String foreignToken = Jwts.builder()
                .subject(USER_ID.toString())
                .claim("type", "REFRESH")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(FOREIGN_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> jwtService.parseRefreshToken(foreignToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void extractUserIdAndRole_workForAccessToken() {
        String token = jwtService.generateAccessToken(user());

        assertThat(jwtService.extractUserId(token)).isEqualTo(USER_ID.toString());
        assertThat(jwtService.extractRole(token)).isEqualTo("ORGANIZER");
    }

    @Test
    void getAccessExpiration_returnsConfiguredValue() {
        assertThat(jwtService.getAccessExpiration()).isEqualTo(ACCESS_EXPIRATION);
    }
}
