package com.rdam.security.jwt;

import com.rdam.security.service.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Servicio de generacion y validacion de tokens JWT.
 * Soporta access tokens y refresh tokens (stateless, con claim token_type).
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;
    private final long refreshExpirationMs;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-ms}") long expirationMs,
                      @Value("${jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    /**
     * Genera un access token JWT.
     */
    public String generateToken(CustomUserDetails userDetails) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        var builder = Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("rol", userDetails.getRol())
                .claim("userId", userDetails.getUserId())
                .claim("token_type", "access")
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey);

        if (userDetails.getCircunscripcionId() != null) {
            builder.claim("circunscripcion_id", userDetails.getCircunscripcionId());
        }

        return builder.compact();
    }

    /**
     * Genera un refresh token JWT (stateless, sin tabla nueva).
     * Contiene solo claims minimos + token_type=refresh.
     */
    public String generateRefreshToken(CustomUserDetails userDetails) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshExpirationMs);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("userId", userDetails.getUserId())
                .claim("rol", userDetails.getRol())
                .claim("token_type", "refresh")
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractRol(String token) {
        return extractClaims(token).get("rol", String.class);
    }

    public Integer extractCircunscripcionId(String token) {
        return extractClaims(token).get("circunscripcion_id", Integer.class);
    }

    public Integer extractUserId(String token) {
        return extractClaims(token).get("userId", Integer.class);
    }

    public String extractTokenType(String token) {
        return extractClaims(token).get("token_type", String.class);
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
