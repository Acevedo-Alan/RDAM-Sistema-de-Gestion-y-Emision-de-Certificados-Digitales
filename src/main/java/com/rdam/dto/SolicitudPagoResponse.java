package com.rdam.dto;

/**
 * DTO con los datos encriptados que el frontend envia al mock PlusPagos.
 */
public record SolicitudPagoResponse(
        String Comercio,
        String TransaccionComercioId,
        String Monto,
        String Informacion,
        String CallbackSuccess,
        String CallbackCancel,
        String UrlSuccess,
        String UrlError,
        String urlPasarela
) {
}
