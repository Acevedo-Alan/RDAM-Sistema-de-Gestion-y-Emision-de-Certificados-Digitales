package com.rdam.dto;

import jakarta.validation.constraints.NotNull;

public record ReasignacionRequest(
        @NotNull Integer nuevoEmpleadoId
) {
}
