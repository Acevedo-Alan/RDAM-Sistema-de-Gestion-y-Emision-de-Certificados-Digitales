package com.rdam.service;

import com.rdam.dto.SolicitudPagoResponse;
import com.rdam.dto.WebhookPagoRequest;

public interface PagoService {

    /**
     * Genera los datos encriptados de pago para que el frontend envie al mock PlusPagos.
     * Solo para solicitudes en estado APROBADA.
     */
    SolicitudPagoResponse generarDatosPago(Integer solicitudId, Integer userId);

    /**
     * Procesa el webhook de pago de PlusPagos.
     * Idempotente: si ya esta PAGADA, retorna sin error.
     * Transiciona APROBADA → PAGADA.
     */
    void procesarWebhookPago(WebhookPagoRequest request);
}
