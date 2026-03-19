package com.rdam.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SolicitudAdminResponse(
        Integer id,
        String numeroTramite,
        String estado,
        String tipoCertificado,
        String circunscripcion,
        String ciudadanoNombre,
        String motivoRechazo,
        String empleadoAsignado,
        BigDecimal montoArancel,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Integer ciudadanoId,
        Integer circunscripcionId,
        Integer empleadoAsignadoId
) {
}
