package com.rdam.service.event;

import java.math.BigDecimal;

public record SolicitudAprobadaEvent(
        Integer solicitudId,
        String ciudadanoEmail,
        String tipoCertificado,
        BigDecimal montoArancel
) {
}
