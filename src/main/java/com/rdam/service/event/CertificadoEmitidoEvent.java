package com.rdam.service.event;

public record CertificadoEmitidoEvent(
        Integer solicitudId,
        String ciudadanoEmail,
        String numeroCertificado
) {
}
