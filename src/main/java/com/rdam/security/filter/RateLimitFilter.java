package com.rdam.security.filter;

import com.rdam.security.jwt.JwtService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtro de rate limiting multi-endpoint usando Bucket4j.
 * <p>
 * Endpoints protegidos:
 * - POST /auth/login       → 10 req/min por IP
 * - POST /auth/registro    → 5 req/min por IP
 * - POST /api/solicitudes  → 20 req/min por userId (JWT)
 * - POST /api/pagos/webhook → 100 req/min por IP
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final JwtService jwtService;

    public RateLimitFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        RateLimitRule rule = resolveRule(uri, method);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String identifier = resolveIdentifier(request, rule);
        String bucketKey = rule.prefix + ":" + identifier;
        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> createBucket(rule.capacity));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Demasiados intentos. Espere un momento.\"}");
        }
    }

    private RateLimitRule resolveRule(String uri, String method) {
        if (!"POST".equalsIgnoreCase(method)) {
            return null;
        }
        if ("/auth/login".equals(uri)) {
            return new RateLimitRule("login", 10, IdentifierType.IP);
        }
        if ("/auth/registro".equals(uri)) {
            return new RateLimitRule("registro", 5, IdentifierType.IP);
        }
        if ("/api/solicitudes".equals(uri)) {
            return new RateLimitRule("solicitudes", 20, IdentifierType.USER_ID);
        }
        if ("/api/pagos/webhook".equals(uri)) {
            return new RateLimitRule("webhook", 100, IdentifierType.IP);
        }
        return null;
    }

    private String resolveIdentifier(HttpServletRequest request, RateLimitRule rule) {
        if (rule.identifierType == IdentifierType.USER_ID) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    Integer userId = jwtService.extractUserId(token);
                    if (userId != null) {
                        return userId.toString();
                    }
                } catch (Exception ignored) {
                    // Si el token es invalido, caemos a IP
                }
            }
            return request.getRemoteAddr();
        }
        return request.getRemoteAddr();
    }

    private Bucket createBucket(int capacity) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, Duration.ofMinutes(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    @Scheduled(fixedDelay = 300_000)
    void limpiarBucketsInactivos() {
        buckets.clear();
    }

    private enum IdentifierType {
        IP, USER_ID
    }

    private static class RateLimitRule {
        final String prefix;
        final int capacity;
        final IdentifierType identifierType;

        RateLimitRule(String prefix, int capacity, IdentifierType identifierType) {
            this.prefix = prefix;
            this.capacity = capacity;
            this.identifierType = identifierType;
        }
    }
}
