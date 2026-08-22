package com.sorokaandriy.auth_service.security;

import com.sorokaandriy.auth_service.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretRefreshKey;
    private final SecretKey secretAccessKey;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtService(@Value("${jwt.access.secret}") String secretAccess,
                      @Value("${jwt.refresh.secret}") String secretRefresh,
                      @Value("${jwt.access-expiration}") long accessExpiration,
                      @Value("${jwt.refresh-expiration}") long refreshExpiration) {
        this.secretAccessKey = Keys.hmacShaKeyFor(secretAccess.getBytes(StandardCharsets.UTF_8));
        this.secretRefreshKey = Keys.hmacShaKeyFor(secretRefresh.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }


    public String generateAccessToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("type", "ACCESS")
                .issuedAt(Date.from(now.toInstant()))
                .expiration(new Date(now.getTime() + accessExpiration))
                .signWith(secretAccessKey)
                .compact();
    }


    public String generateRefreshToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("type", "REFRESH")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshExpiration))
                .signWith(secretRefreshKey)
                .compact();
    }


    public Claims parseAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(secretAccessKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Claims parseRefreshToken(String token) {
        return Jwts.parser()
                .verifyWith(secretRefreshKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isAccessTokenValid(String token) {
        try {
            parseAccessToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public boolean isRefreshTokenValid(String token) {
        try {
            parseRefreshToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public String extractUserId(String token) {
        return parseAccessToken(token).getSubject();
    }

    public String extractRole(String token) {
        return parseAccessToken(token).get("role", String.class);
    }

    public long getAccessExpiration() {
        return accessExpiration;
    }
}
