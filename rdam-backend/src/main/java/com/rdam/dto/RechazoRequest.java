package com.rdam.dto;

import jakarta.validation.constraints.NotBlank;

public record RechazoRequest(
        @NotBlank String motivo
) {
}
