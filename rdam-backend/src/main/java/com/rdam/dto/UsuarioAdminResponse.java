package com.rdam.dto;

import java.time.OffsetDateTime;

public record UsuarioAdminResponse(
        Integer id,
        String nombre,
        String apellido,
        String email,
        String cuil,
        String rol,
        Boolean activo,
        OffsetDateTime createdAt,
        OffsetDateTime eliminadoEn,
        Integer circunscripcionId,
        String legajo,
        String cargo
) {
}
