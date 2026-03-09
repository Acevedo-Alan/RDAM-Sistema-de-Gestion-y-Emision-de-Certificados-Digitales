package com.rdam.service.impl;

import com.rdam.domain.entity.EstadoPago;
import com.rdam.domain.entity.EstadoSolicitud;
import com.rdam.domain.entity.Pago;
import com.rdam.domain.entity.RolUsuario;
import com.rdam.domain.entity.Solicitud;
import com.rdam.dto.SolicitudPagoResponse;
import com.rdam.dto.WebhookPagoRequest;
import com.rdam.repository.PagoRepository;
import com.rdam.repository.SolicitudRepository;
import com.rdam.service.PagoService;
import com.rdam.service.PlusPagosCryptoService;
import com.rdam.service.event.PagoAprobadoEvent;
import com.rdam.service.exception.AccesoDenegadoException;
import com.rdam.service.exception.EstadoInvalidoException;
import com.rdam.service.exception.RecursoNoEncontradoException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Service
public class PagoServiceImpl implements PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoServiceImpl.class);

    private final SolicitudRepository solicitudRepository;
    private final PagoRepository pagoRepository;
    private final PlusPagosCryptoService cryptoService;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final String merchantId;
    private final String plusPagosUrl;

    public PagoServiceImpl(SolicitudRepository solicitudRepository,
                           PagoRepository pagoRepository,
                           PlusPagosCryptoService cryptoService,
                           EntityManager entityManager,
                           ApplicationEventPublisher eventPublisher,
                           ObjectMapper objectMapper,
                           @Value("${pluspagos.merchant-id}") String merchantId,
                           @Value("${pluspagos.url}") String plusPagosUrl) {
        this.solicitudRepository = solicitudRepository;
        this.pagoRepository = pagoRepository;
        this.cryptoService = cryptoService;
        this.entityManager = entityManager;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.merchantId = merchantId;
        this.plusPagosUrl = plusPagosUrl;
    }

    @Override
    @Transactional(readOnly = true)
    public SolicitudPagoResponse generarDatosPago(Integer solicitudId, Integer userId) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro la solicitud con ID: " + solicitudId));

        // Validar que la solicitud pertenece al ciudadano
        if (!solicitud.getCiudadano().getId().equals(userId)) {
            throw new AccesoDenegadoException("No tiene permiso para acceder a esta solicitud");
        }

        if (solicitud.getEstado() != EstadoSolicitud.APROBADA) {
            throw new EstadoInvalidoException(
                    "La solicitud no esta en estado APROBADA. Estado actual: " + solicitud.getEstado());
        }

        // Monto en centavos (como espera el mock PlusPagos)
        int montoCentavos = solicitud.getMontoArancel().multiply(new BigDecimal("100")).intValue();

        String transaccionComercioId = "SOL-" + solicitud.getId();

        // Encriptar datos para el mock PlusPagos
        String montoEncriptado = cryptoService.encrypt(String.valueOf(montoCentavos));
        String urlSuccess = cryptoService.encrypt("http://localhost:8080/api/pagos/resultado?status=success&solicitud=" + solicitudId);
        String urlError = cryptoService.encrypt("http://localhost:8080/api/pagos/resultado?status=error&solicitud=" + solicitudId);
        String callbackSuccess = cryptoService.encrypt("http://localhost:8080/api/pagos/webhook");
        String callbackCancel = cryptoService.encrypt("http://localhost:8080/api/pagos/webhook");
        String informacion = cryptoService.encrypt("Pago solicitud certificado #" + solicitudId);

        return new SolicitudPagoResponse(
                merchantId,
                transaccionComercioId,
                montoEncriptado,
                informacion,
                callbackSuccess,
                callbackCancel,
                urlSuccess,
                urlError,
                plusPagosUrl
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void procesarWebhookPago(WebhookPagoRequest request) {
        log.info("Procesando webhook - TransaccionComercioId: {}, Estado: {}",
                request.TransaccionComercioId(), request.Estado());

        // Extraer solicitudId del TransaccionComercioId (formato: SOL-{id})
        Integer solicitudId = extraerSolicitudId(request.TransaccionComercioId());

        // Idempotencia: si ya existe pago aprobado, retornar silenciosamente
        var pagoExistente = pagoRepository.findBySolicitudId(solicitudId);
        if (pagoExistente.isPresent() && pagoExistente.get().getEstadoPago() == EstadoPago.APROBADO) {
            log.info("Webhook duplicado ignorado - Solicitud {} ya tiene pago aprobado", solicitudId);
            return;
        }

        // Solo procesar pagos con estado REALIZADA (EstadoId = 3)
        if (!"REALIZADA".equals(request.Estado())) {
            log.info("Webhook con estado no exitoso: {} - ignorando", request.Estado());
            return;
        }

        // Bloquear solicitud
        Solicitud solicitud = buscarSolicitudConLock(solicitudId);

        // Validar estado
        if (solicitud.getEstado() == EstadoSolicitud.PAGADA || solicitud.getEstado() == EstadoSolicitud.EMITIDA) {
            log.info("Solicitud {} ya en estado {} - webhook idempotente", solicitudId, solicitud.getEstado());
            return;
        }

        if (solicitud.getEstado() != EstadoSolicitud.APROBADA) {
            throw new EstadoInvalidoException(
                    "La solicitud no esta en estado APROBADA. Estado actual: " + solicitud.getEstado());
        }

        // Validar monto
        BigDecimal montoWebhook = new BigDecimal(request.Monto());
        if (solicitud.getMontoArancel().compareTo(montoWebhook) != 0) {
            throw new EstadoInvalidoException(
                    "Monto del webhook (" + montoWebhook + ") no coincide con el monto de la solicitud ("
                    + solicitud.getMontoArancel() + ")");
        }

        // Crear entidad Pago
        Pago pago = new Pago();
        pago.setSolicitud(solicitud);
        pago.setMonto(montoWebhook);
        pago.setMoneda("ARS");
        pago.setEstadoPago(EstadoPago.APROBADO);
        pago.setProveedorPago("PlusPagos");
        pago.setIdExterno(request.TransaccionPlataformaId());
        pago.setFechaIntento(OffsetDateTime.now());
        pago.setFechaConfirmacion(OffsetDateTime.now());

        // Guardar payload completo como JSON para auditoria
        try {
            pago.setDatosRespuesta(objectMapper.writeValueAsString(
                    Map.of(
                            "Tipo", request.Tipo(),
                            "TransaccionPlataformaId", request.TransaccionPlataformaId(),
                            "TransaccionComercioId", request.TransaccionComercioId(),
                            "Monto", request.Monto(),
                            "EstadoId", request.EstadoId(),
                            "Estado", request.Estado(),
                            "FechaProcesamiento", request.FechaProcesamiento()
                    )));
        } catch (JsonProcessingException e) {
            log.warn("Error serializando datos_respuesta del webhook", e);
        }

        // RLS ANTES de modificar la entidad managed (Hibernate auto-flush)
        setRlsContext(solicitud.getCiudadano().getId(), RolUsuario.ciudadano);

        // Transicionar solicitud a PAGADA
        solicitud.setEstado(EstadoSolicitud.PAGADA);
        solicitud.setFechaPago(OffsetDateTime.now());

        // Guardar con manejo de errores
        guardarConManejodeErrores(pago, solicitud);

        // Publicar evento (se ejecutara post-commit via @TransactionalEventListener)
        eventPublisher.publishEvent(new PagoAprobadoEvent(
                solicitudId,
                solicitud.getCiudadano().getEmail()
        ));

        log.info("Pago procesado exitosamente - Solicitud: {}", solicitudId);
    }

    // -- Metodos privados --

    private Integer extraerSolicitudId(String transaccionComercioId) {
        if (transaccionComercioId == null || !transaccionComercioId.startsWith("SOL-")) {
            throw new IllegalArgumentException(
                    "TransaccionComercioId invalido: " + transaccionComercioId);
        }
        try {
            return Integer.parseInt(transaccionComercioId.substring(4));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "TransaccionComercioId invalido: " + transaccionComercioId, e);
        }
    }

    private Solicitud buscarSolicitudConLock(Integer solicitudId) {
        try {
            return solicitudRepository.findByIdForUpdate(solicitudId)
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No se encontro la solicitud con ID: " + solicitudId));
        } catch (jakarta.persistence.PessimisticLockException ex) {
            throw new com.rdam.service.exception.ConcurrenciaException(
                    "La solicitud ID " + solicitudId
                    + " esta siendo procesada por otro usuario. Intente nuevamente.", ex);
        } catch (LockTimeoutException ex) {
            throw new com.rdam.service.exception.ConcurrenciaException(
                    "Timeout al intentar bloquear la solicitud ID " + solicitudId, ex);
        }
    }

    private void setRlsContext(Integer userId, RolUsuario rol) {
        entityManager.createNativeQuery(
                "SELECT rdam_set_session_user(:userId, CAST(:rol AS text)::rol_usuario)")
                .setParameter("userId", userId)
                .setParameter("rol", rol.name())
                .getSingleResult();
    }

    private void guardarConManejodeErrores(Pago pago, Solicitud solicitud) {
        try {
            pagoRepository.save(pago);
            solicitudRepository.save(solicitud);
            entityManager.flush();
        } catch (DataIntegrityViolationException ex) {
            // UNIQUE constraint en solicitud_id → pago duplicado (idempotencia)
            log.info("Pago duplicado detectado por constraint - solicitud: {}", solicitud.getId());
        } catch (PersistenceException ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : "";
            if (message.contains("AUTH_VIOLATION") || message.contains("RLS")) {
                throw new AccesoDenegadoException(
                        "Acceso denegado por politica de seguridad: "
                        + (ex.getCause() != null ? ex.getCause().getMessage() : message), ex);
            }
            throw ex;
        }
    }
}
