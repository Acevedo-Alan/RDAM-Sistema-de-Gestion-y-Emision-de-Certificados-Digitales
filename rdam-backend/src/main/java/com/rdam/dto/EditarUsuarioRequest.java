package com.rdam.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record EditarUsuarioRequest(

        @Size(max = 150)
        String nombre,

        @Size(max = 150)
        String apellido,

        @Email(message = "El email debe ser válido")
        @Size(max = 255)
        String email,

        @Size(max = 100)
        String cargo
) {
}
