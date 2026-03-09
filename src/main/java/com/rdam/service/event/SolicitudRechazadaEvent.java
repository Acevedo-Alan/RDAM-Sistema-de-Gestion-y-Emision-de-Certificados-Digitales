package com.rdam.service.event;

public record SolicitudRechazadaEvent(
        Integer solicitudId,
        String ciudadanoEmail,
        String tipoCertificado,
        String motivo
) {
}
