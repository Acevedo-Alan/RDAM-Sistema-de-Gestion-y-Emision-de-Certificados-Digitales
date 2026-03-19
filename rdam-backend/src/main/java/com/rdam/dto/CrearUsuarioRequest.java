package com.rdam.dto;

import com.rdam.domain.entity.RolUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearUsuarioRequest(

        @Size(min = 11, max = 11, message = "El CUIL debe tener exactamente 11 caracteres")
        String cuil,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150)
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 150)
        String apellido,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email debe ser válido")
        @Size(max = 255)
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
        String password,

        @NotNull(message = "El rol es obligatorio")
        RolUsuario rol,

        Integer circunscripcionId,

        @Size(max = 100)
        String cargo
) {
}
