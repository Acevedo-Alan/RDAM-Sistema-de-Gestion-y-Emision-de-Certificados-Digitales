package com.rdam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TipoCertificadoRequest(
        @NotBlank @Size(max = 150) String nombre,
        String descripcion
) {
}
