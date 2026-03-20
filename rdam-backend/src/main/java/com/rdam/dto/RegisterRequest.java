package com.rdam.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "El nombre es obligatorio")
    String nombre,

    @NotBlank(message = "El CUIL es obligatorio")
    @Pattern(regexp = "\\d{11}", message = "El CUIL debe tener 11 digitos")
    String cuil,

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email no valido")
    String email,

    @NotBlank(message = "La contrasena es obligatoria")
    @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres")
    String password
) {}
