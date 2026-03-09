package com.rdam.service;

import com.rdam.domain.entity.Certificado;

public interface CertificadoService {

    /**
     * Emite un certificado para una solicitud en estado PAGADA.
     * Transiciona PAGADA → EMITIDA.
     * No permite doble emision.
     */
    Certificado emitirCertificado(Integer solicitudId, Integer empleadoId, Long circunscripcionId);

    /**
     * Genera el PDF del certificado en memoria.
     * Valida propiedad (ciudadanoId) y estado (PAGADA o EMITIDA).
     */
    byte[] generarCertificadoPdf(Long solicitudId, Long ciudadanoId);
}
