package com.rdam.service.event;

public record SolicitudCanceladaEvent(
        Integer solicitudId,
        String ciudadanoEmail
) {
}
