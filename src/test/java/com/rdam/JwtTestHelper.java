package com.rdam;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Helper para generar tokens JWT firmados en tests de integracion.
 * Usa la misma clave secreta configurada en BaseIntegrationTest.
 */
public final class JwtTestHelper {

    private static final String TEST_SECRET = "test-secret-key-rdam-256bits-minimo-para-hmac-sha256";
    private static final SecretKey SIGNING_KEY =
            Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
    private static final long EXPIRATION_MS = 3600000;

    private JwtTestHelper() {
    }

    public static String generarTokenCiudadano(Integer userId, String cuil) {
        Date now = new Date();
        return Jwts.builder()
                .subject(cuil)
                .claim("rol", "ciudadano")
                .claim("userId", userId)
                .claim("token_type", "access")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + EXPIRATION_MS))
                .signWith(SIGNING_KEY)
                .compact();
    }

    public static String generarTokenInterno(Integer userId, String legajo,
                                              Integer circunscripcionId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(legajo)
                .claim("rol", "interno")
                .claim("userId", userId)
                .claim("circunscripcion_id", circunscripcionId)
                .claim("token_type", "access")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + EXPIRATION_MS))
                .signWith(SIGNING_KEY)
                .compact();
    }

    public static String generarTokenAdmin(Integer userId, String legajo,
                                            Integer circunscripcionId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(legajo)
                .claim("rol", "admin")
                .claim("userId", userId)
                .claim("circunscripcion_id", circunscripcionId)
                .claim("token_type", "access")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + EXPIRATION_MS))
                .signWith(SIGNING_KEY)
                .compact();
    }
}
