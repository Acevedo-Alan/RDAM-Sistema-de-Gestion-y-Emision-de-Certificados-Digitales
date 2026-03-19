package com.rdam.dto;

import java.time.OffsetDateTime;

public record TipoCertificadoResponse(
        Integer id,
        String nombre,
        String descripcion,
        Boolean activo,
        OffsetDateTime createdAt,
        OffsetDateTime eliminadoEn
) {
}
