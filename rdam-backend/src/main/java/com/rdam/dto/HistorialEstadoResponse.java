package com.rdam.dto;

import java.time.OffsetDateTime;

public record HistorialEstadoResponse(
        Integer id,
        String estadoAnterior,
        String estadoNuevo,
        Integer usuarioId,
        String usuarioNombre,
        Integer empleadoId,
        String comentario,
        String ipOrigen,
        OffsetDateTime createdAt
) {
}
