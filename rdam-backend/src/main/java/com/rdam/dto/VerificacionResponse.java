package com.rdam.dto;

public record VerificacionResponse(
        boolean valido,
        String numeroTramite,
        String titular,
        String tipoCertificado,
        String circunscripcion,
        String fechaEmision
) {
    public static VerificacionResponse invalido() {
        return new VerificacionResponse(false, null, null, null, null, null);
    }
}
