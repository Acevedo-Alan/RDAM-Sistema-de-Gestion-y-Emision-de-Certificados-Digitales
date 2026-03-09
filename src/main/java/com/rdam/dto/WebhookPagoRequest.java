package com.rdam.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO exacto del payload que envia el mock PlusPagos.
 * Campos en PascalCase para coincidir con el formato externo.
 */
public record WebhookPagoRequest(
        @NotBlank String Tipo,
        @NotBlank String TransaccionPlataformaId,
        @NotBlank String TransaccionComercioId,
        @NotBlank String Monto,
        @NotBlank String EstadoId,
        @NotBlank String Estado,
        @NotBlank String FechaProcesamiento
) {
}
