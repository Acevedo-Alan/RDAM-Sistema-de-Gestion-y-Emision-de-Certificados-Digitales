package com.rdam.dto;

import jakarta.validation.constraints.NotNull;

public record SolicitudRequest(
        @NotNull Long tipoCertificadoId,
        @NotNull Long circunscripcionId
) {
}
