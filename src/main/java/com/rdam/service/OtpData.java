package com.rdam.service;

import java.time.LocalDateTime;

public class OtpData {

    private final String codigo;
    private final LocalDateTime expirationTime;
    private int attempts;

    public OtpData(String codigo, LocalDateTime expirationTime) {
        this.codigo = codigo;
        this.expirationTime = expirationTime;
        this.attempts = 0;
    }

    public String getCodigo() {
        return codigo;
    }

    public LocalDateTime getExpirationTime() {
        return expirationTime;
    }

    public int getAttempts() {
        return attempts;
    }

    public void incrementAttempts() {
        this.attempts++;
    }
}
