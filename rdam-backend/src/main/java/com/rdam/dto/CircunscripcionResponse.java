package com.rdam.dto;

import java.time.OffsetDateTime;

public record CircunscripcionResponse(
        Integer id,
        String nombre,
        String codigo,
        String descripcion,
        Boolean activo,
        OffsetDateTime createdAt
) {
}
