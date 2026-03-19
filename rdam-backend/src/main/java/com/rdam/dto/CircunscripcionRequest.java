package com.rdam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CircunscripcionRequest(
        @NotBlank String nombre,
        @NotBlank @Pattern(regexp = "^CIRC-\\d{2}$") String codigo,
        String descripcion
) {
}
