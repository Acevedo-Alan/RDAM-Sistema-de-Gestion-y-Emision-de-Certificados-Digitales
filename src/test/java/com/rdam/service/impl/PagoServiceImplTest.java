package com.rdam.service.impl;

import com.rdam.domain.entity.EstadoPago;
import com.rdam.domain.entity.EstadoSolicitud;
import com.rdam.domain.entity.Pago;
import com.rdam.domain.entity.RolUsuario;
import com.rdam.domain.entity.Solicitud;
import com.rdam.domain.entity.TipoCertificado;
import com.rdam.domain.entity.Usuario;
import com.rdam.dto.WebhookPagoRequest;
import com.rdam.repository.PagoRepository;
import com.rdam.repository.SolicitudRepository;
import com.rdam.service.PlusPagosCryptoService;
import com.rdam.service.event.PagoAprobadoEvent;
import com.rdam.service.exception.EstadoInvalidoException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServiceImplTest {

    @Mock
    private SolicitudRepository solicitudRepository;

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private PlusPagosCryptoService cryptoService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ObjectMapper objectMapper;

    private PagoServiceImpl pagoService;

    private Solicitud solicitud;
    private Usuario ciudadano;

    @BeforeEach
    void setUp() {
        pagoService = new PagoServiceImpl(
                solicitudRepository, pagoRepository, cryptoService,
                entityManager, eventPublisher, objectMapper,
                "RDAM-MOCK-001", "http://localhost:8081/pluspagos");

        ciudadano = new Usuario();
        ciudadano.setId(1);
        ciudadano.setEmail("ciudadano@test.com");
        ciudadano.setRol(RolUsuario.ciudadano);

        TipoCertificado tipo = new TipoCertificado();
        tipo.setId(1);
        tipo.setNombre("Nacimiento");

        solicitud = new Solicitud();
        solicitud.setId(50);
        solicitud.setCiudadano(ciudadano);
        solicitud.setTipoCertificado(tipo);
        solicitud.setEstado(EstadoSolicitud.APROBADA);
        solicitud.setMontoArancel(new BigDecimal("5000.00"));
    }

    private void mockRlsContext() {
        Query nativeQuery = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
        when(nativeQuery.getSingleResult()).thenReturn(1);
    }

    private WebhookPagoRequest buildWebhookRequest(String estado, String monto,
                                                     String transaccionComercioId) {
        return new WebhookPagoRequest(
                "pago",
                "TXN-PLAT-001",
                transaccionComercioId,
                monto,
                "3",
                estado,
                "2026-03-08T10:00:00");
    }

    // ── procesarWebhookPago() ──

    @Test
    void procesarWebhookPago_pagoRealizadaEnSolicitudAprobada_transicionaAPagada() throws Exception {
        WebhookPagoRequest request = buildWebhookRequest("REALIZADA", "5000.00", "SOL-50");

        when(pagoRepository.findBySolicitudId(50)).thenReturn(Optional.empty());
        when(solicitudRepository.findByIdForUpdate(50)).thenReturn(Optional.of(solicitud));
        mockRlsContext();
        doNothing().when(entityManager).flush();
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        pagoService.procesarWebhookPago(request);

        assertEquals(EstadoSolicitud.PAGADA, solicitud.getEstado());
        verify(pagoRepository).save(any(Pago.class));
        verify(solicitudRepository).save(solicitud);

        ArgumentCaptor<PagoAprobadoEvent> captor =
                ArgumentCaptor.forClass(PagoAprobadoEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(50, captor.getValue().solicitudId());
        assertEquals("ciudadano@test.com", captor.getValue().ciudadanoEmail());
    }

    @Test
    void procesarWebhookPago_idempotencia_pagoYaAprobado_retornaSinProcesar() {
        WebhookPagoRequest request = buildWebhookRequest("REALIZADA", "5000.00", "SOL-50");

        Pago pagoExistente = new Pago();
        pagoExistente.setEstadoPago(EstadoPago.APROBADO);
        when(pagoRepository.findBySolicitudId(50)).thenReturn(Optional.of(pagoExistente));

        pagoService.procesarWebhookPago(request);

        verify(solicitudRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void procesarWebhookPago_estadoNoRealizada_retornaSinProcesar() {
        WebhookPagoRequest request = buildWebhookRequest("RECHAZADA", "5000.00", "SOL-50");

        when(pagoRepository.findBySolicitudId(50)).thenReturn(Optional.empty());

        pagoService.procesarWebhookPago(request);

        verify(solicitudRepository, never()).findByIdForUpdate(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void procesarWebhookPago_solicitudNoAprobada_lanzaEstadoInvalido() {
        WebhookPagoRequest request = buildWebhookRequest("REALIZADA", "5000.00", "SOL-50");
        solicitud.setEstado(EstadoSolicitud.PENDIENTE_REVISION);

        when(pagoRepository.findBySolicitudId(50)).thenReturn(Optional.empty());
        when(solicitudRepository.findByIdForUpdate(50)).thenReturn(Optional.of(solicitud));

        assertThrows(EstadoInvalidoException.class,
                () -> pagoService.procesarWebhookPago(request));
    }

    @Test
    void procesarWebhookPago_montoNoCoincide_lanzaEstadoInvalido() {
        WebhookPagoRequest request = buildWebhookRequest("REALIZADA", "9999.99", "SOL-50");

        when(pagoRepository.findBySolicitudId(50)).thenReturn(Optional.empty());
        when(solicitudRepository.findByIdForUpdate(50)).thenReturn(Optional.of(solicitud));

        assertThrows(EstadoInvalidoException.class,
                () -> pagoService.procesarWebhookPago(request));
    }

    @Test
    void procesarWebhookPago_transaccionComercioIdInvalido_lanzaIllegalArgument() {
        WebhookPagoRequest request = buildWebhookRequest("REALIZADA", "5000.00", "INVALID-ID");

        assertThrows(IllegalArgumentException.class,
                () -> pagoService.procesarWebhookPago(request));
    }
}
