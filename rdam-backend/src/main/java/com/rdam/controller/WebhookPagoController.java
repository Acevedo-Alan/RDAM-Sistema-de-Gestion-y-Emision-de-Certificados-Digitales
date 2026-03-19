package com.rdam.controller;

import com.rdam.dto.WebhookPagoRequest;
import com.rdam.service.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pagos")
@Tag(name = "Pagos", description = "Webhook de notificacion de pagos desde PlusPagos")
public class WebhookPagoController {

    private static final Logger log = LoggerFactory.getLogger(WebhookPagoController.class);

    private final PagoService pagoService;

    public WebhookPagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @Operation(summary = "Recibir notificacion de pago desde PlusPagos (sin autenticacion)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pago procesado correctamente"),
            @ApiResponse(responseCode = "400", description = "Payload invalido"),
            @ApiResponse(responseCode = "422", description = "Solicitud no encontrada o estado inconsistente"),
            @ApiResponse(responseCode = "429", description = "Rate limit excedido")
    })
    @PostMapping("/webhook")
    public ResponseEntity<Void> recibirWebhook(@Valid @RequestBody WebhookPagoRequest request) {
        MDC.put("transaccionComercioId", request.TransaccionComercioId());
        try {
            log.info("Webhook recibido - Tipo: {}, Estado: {}, Monto: {}",
                    request.Tipo(), request.Estado(), request.Monto());

            pagoService.procesarWebhookPago(request);

            return ResponseEntity.ok().build();
        } finally {
            MDC.clear();
        }
    }
}
