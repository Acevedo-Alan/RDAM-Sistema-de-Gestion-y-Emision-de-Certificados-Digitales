package com.rdam.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);

    private static final int OTP_LENGTH = 6;
    private static final int EXPIRATION_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;

    private final ConcurrentHashMap<String, OtpData> otpStore = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    public String generarOtp(String email) {
        String codigo = generarCodigoSeguro();
        LocalDateTime expiration = LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES);
        otpStore.put(email, new OtpData(codigo, expiration));
        logger.info("action=OTP_GENERATED email={}", email);
        return codigo;
    }

    public boolean validarOtp(String email, String codigo) {
        OtpData otpData = otpStore.get(email);

        if (otpData == null) {
            logger.warn("action=OTP_INVALID email={}", email);
            return false;
        }

        if (LocalDateTime.now().isAfter(otpData.getExpirationTime())) {
            otpStore.remove(email);
            logger.warn("action=OTP_EXPIRED email={}", email);
            return false;
        }

        otpData.incrementAttempts();

        if (otpData.getAttempts() > MAX_ATTEMPTS) {
            otpStore.remove(email);
            logger.warn("action=OTP_MAX_ATTEMPTS_EXCEEDED email={}", email);
            return false;
        }

        if (!otpData.getCodigo().equals(codigo)) {
            logger.warn("action=OTP_INVALID email={}", email);
            return false;
        }

        otpStore.remove(email);
        logger.info("action=OTP_VERIFIED email={}", email);
        return true;
    }

    @Scheduled(fixedDelay = 60000)
    void limpiarOtpsExpirados() {
        LocalDateTime now = LocalDateTime.now();
        otpStore.entrySet().removeIf(entry ->
                now.isAfter(entry.getValue().getExpirationTime()));
    }

    private String generarCodigoSeguro() {
        int bound = (int) Math.pow(10, OTP_LENGTH);
        int code = secureRandom.nextInt(bound);
        return String.format("%0" + OTP_LENGTH + "d", code);
    }
}
