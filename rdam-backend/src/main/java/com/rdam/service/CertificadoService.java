package com.rdam.service;

import com.rdam.domain.entity.Certificado;
import com.rdam.dto.VerificacionResponse;

public interface CertificadoService {

    /**
     * Emite un certificado para una solicitud en estado PAGADA.
     * Transiciona PAGADA -> EMITIDA.
     * No permite doble emision.
     */
    Certificado emitirCertificado(Integer solicitudId, Integer empleadoId, Long circunscripcionId, String rol);

    /**
     * Genera el PDF del certificado en memoria.
     * Valida propiedad (ciudadanoId) y estado (PAGADA o EMITIDA).
     */
    byte[] generarCertificadoPdf(Long solicitudId, Long ciudadanoId);

    /**
     * Publica un certificado: genera PDF con iText, guarda en disco,
     * crea registro en certificados, transiciona PAGADO -> PUBLICADO.
     */
    Certificado publicarCertificado(Integer solicitudId, Integer empleadoId, Long circunscripcionId, String rol);

    /**
     * Descarga el PDF de un certificado desde disco.
     * Valida propiedad del ciudadano o rol interno/admin.
     */
    byte[] descargarCertificado(Integer certificadoId, Integer userId, String rol);

    /**
     * Verifica un certificado por token.
     * Devuelve datos basicos si es valido, o invalido si no existe/vencido.
     */
    VerificacionResponse verificarCertificado(String token);
}
