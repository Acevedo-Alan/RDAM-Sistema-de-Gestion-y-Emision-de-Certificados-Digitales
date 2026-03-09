package com.rdam.service.event;

public record PagoAprobadoEvent(
        Integer solicitudId,
        String ciudadanoEmail
) {
}
