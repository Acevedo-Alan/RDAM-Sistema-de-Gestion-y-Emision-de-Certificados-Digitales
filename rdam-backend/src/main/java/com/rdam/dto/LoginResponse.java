package com.rdam.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tipo,
        String username,
        String rol,
        Integer userId
) {
}
