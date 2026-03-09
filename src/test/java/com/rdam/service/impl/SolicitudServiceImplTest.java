package com.rdam.service.impl;

import com.rdam.domain.entity.Circunscripcion;
import com.rdam.domain.entity.Empleado;
import com.rdam.domain.entity.EstadoSolicitud;
import com.rdam.domain.entity.RolUsuario;
import com.rdam.domain.entity.Solicitud;
import com.rdam.domain.entity.TipoCertificado;
import com.rdam.domain.entity.Usuario;
import com.rdam.repository.CircunscripcionRepository;
import com.rdam.repository.EmpleadoRepository;
import com.rdam.repository.SolicitudRepository;
import com.rdam.repository.TipoCertificadoRepository;
import com.rdam.repository.UsuarioRepository;
import com.rdam.service.event.SolicitudAprobadaEvent;
import com.rdam.service.event.SolicitudCanceladaEvent;
import com.rdam.service.event.SolicitudRechazadaEvent;
import com.rdam.service.exception.AccesoDenegadoException;
import com.rdam.service.exception.EstadoInvalidoException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitudServiceImplTest {

    @Mock
    private SolicitudRepository solicitudRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EmpleadoRepository empleadoRepository;

    @Mock
    private TipoCertificadoRepository tipoCertificadoRepository;

    @Mock
    private CircunscripcionRepository circunscripcionRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SolicitudServiceImpl solicitudService;

    private Usuario ciudadano;
    private Empleado empleado;
    private Usuario empleadoUsuario;
    private TipoCertificado tipoCertificado;
    private Circunscripcion circunscripcion;
    private Solicitud solicitud;

    @BeforeEach
    void setUp() {
        ciudadano = new Usuario();
        ciudadano.setId(1);
        ciudadano.setCuil("20345678901");
        ciudadano.setEmail("ciudadano@test.com");
        ciudadano.setRol(RolUsuario.ciudadano);
        ciudadano.setActivo(true);
        ciudadano.setNombre("Juan");
        ciudadano.setApellido("Perez");

        empleadoUsuario = new Usuario();
        empleadoUsuario.setId(2);
        empleadoUsuario.setRol(RolUsuario.interno);
        empleadoUsuario.setActivo(true);

        empleado = new Empleado();
        empleado.setId(10);
        empleado.setUsuario(empleadoUsuario);
        empleado.setLegajo("LEG-001");
        empleado.setCircunscripcionId(1);

        tipoCertificado = new TipoCertificado();
        tipoCertificado.setId(1);
        tipoCertificado.setNombre("Nacimiento");

        circunscripcion = new Circunscripcion();
        circunscripcion.setId(1);
        circunscripcion.setNombre("Primera");

        solicitud = new Solicitud();
        solicitud.setId(100);
        solicitud.setCiudadano(ciudadano);
        solicitud.setTipoCertificado(tipoCertificado);
        solicitud.setCircunscripcion(circunscripcion);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE_REVISION);
        solicitud.setMontoArancel(new BigDecimal("5000.00"));
    }

    private void mockRlsContext() {
        Query nativeQuery = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
        when(nativeQuery.getSingleResult()).thenReturn(1);
    }

    // ── crearSolicitud() ──

    @Test
    void crearSolicitud_datosCorrectos_retornaSolicitudPendiente() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(ciudadano));
        when(tipoCertificadoRepository.findById(1)).thenReturn(Optional.of(tipoCertificado));
        when(circunscripcionRepository.findById(1)).thenReturn(Optional.of(circunscripcion));
        mockRlsContext();
        when(solicitudRepository.save(any(Solicitud.class))).thenAnswer(inv -> {
            Solicitud s = inv.getArgument(0);
            s.setId(100);
            return s;
        });

        Solicitud result = solicitudService.crearSolicitud(1L, 1L, 1L);

        assertNotNull(result);
        assertEquals(EstadoSolicitud.PENDIENTE_REVISION, result.getEstado());
        assertEquals(new BigDecimal("5000.00"), result.getMontoArancel());
        verify(solicitudRepository).save(any(Solicitud.class));
    }

    @Test
    void crearSolicitud_ciudadanoNoEncontrado_lanzaIllegalArgument() {
        when(usuarioRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> solicitudService.crearSolicitud(999L, 1L, 1L));
    }

    @Test
    void crearSolicitud_tipoCertificadoNoEncontrado_lanzaIllegalArgument() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(ciudadano));
        when(tipoCertificadoRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> solicitudService.crearSolicitud(1L, 999L, 1L));
    }

    // ── tomarSolicitudParaRevision() ──

    @Test
    void tomarSolicitudParaRevision_transicionCorrecta_cambiaEstadoYAsignaEmpleado() {
        when(solicitudRepository.findByIdForUpdate(100)).thenReturn(Optional.of(solicitud));
        when(empleadoRepository.findByUsuarioId(10)).thenReturn(Optional.of(empleado));
        mockRlsContext();
        doNothing().when(entityManager).flush();

        solicitudService.tomarSolicitudParaRevision(100L, 10L, 1L);

        assertEquals(EstadoSolicitud.EN_REVISION, solicitud.getEstado());
        assertEquals(empleado, solicitud.getEmpleadoAsignado());
        assertNotNull(solicitud.getFechaAsignacion());
        verify(solicitudRepository).save(solicitud);
    }

    @Test
    void tomarSolicitudParaRevision_estadoIncorrecto_lanzaEstadoInvalido() {
        solicitud.setEstado(EstadoSolicitud.APROBADA);
        when(solicitudRepository.findByIdForUpdate(100)).thenReturn(Optional.of(solicitud));
        when(empleadoRepository.findByUsuarioId(10)).thenReturn(Optional.of(empleado));

        assertThrows(EstadoInvalidoException.class,
                () -> solicitudService.tomarSolicitudParaRevision(100L, 10L, 1L));
    }

    @Test
    void tomarSolicitudParaRevision_circunscripcionDistinta_lanzaAccesoDenegado() {
        when(solicitudRepository.findByIdForUpdate(100)).thenReturn(Optional.of(solicitud));
        when(empleadoRepository.findByUsuarioId(10)).thenReturn(Optional.of(empleado));

        assertThrows(AccesoDenegadoException.class,
                () -> solicitudService.tomarSolicitudParaRevision(100L, 10L, 99L));
    }

    // ── aprobarSolicitud() ──

    @Test
    void aprobarSolicitud_transicionCorrecta_cambiaEstadoYPublicaEvento() {
        solicitud.setEstado(EstadoSolicitud.EN_REVISION);
        when(solicitudRepository.findByIdForUpdate(100)).thenReturn(Optional.of(solicitud));
        when(empleadoRepository.findByUsuarioId(10)).thenReturn(Optional.of(empleado));
        mockRlsContext();
        doNothing().when(entityManager).flush();

        solicitudService.aprobarSolicitud(100L, 10L, 1L);

        assertEquals(EstadoSolicitud.APROBADA, solicitud.getEstado());
        assertNotNull(solicitud.getFechaAprobacion());
        verify(solicitudRepository).save(solicitud);

        ArgumentCaptor<SolicitudAprobadaEvent> captor =
                ArgumentCaptor.forClass(SolicitudAprobadaEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(100, captor.getValue().solicitudId());
    }

    @Test
    void aprobarSolicitud_estadoIncorrecto_lanzaEstadoInvalido() {
        solicitud.setEstado(EstadoSolicitud.PENDIENTE_REVISION);
        when(solicitudRepository.findByIdForUpdate(100)).thenReturn(Optional.of(solicitud));
        when(empleadoRepository.findByUsuarioId(10)).thenReturn(Optional.of(empleado));

        assertThrows(EstadoInvalidoException.class,
                () -> solicitudService.aprobarSolicitud(100L, 10L, 1L));
    }

    // ── rechazarSolicitud() ──

    @Test
    void rechazarSolicitud_transicionCorrecta_cambiaEstadoConMotivoYPublicaEvento() {
        solicitud.setEstado(EstadoSolicitud.EN_REVISION);
        when(solicitudRepository.findByIdForUpdate(100)).thenReturn(Optional.of(solicitud));
        when(empleadoRepository.findByUsuarioId(10)).thenReturn(Optional.of(empleado));
        mockRlsContext();
        doNothing().when(entityManager).flush();

        solicitudService.rechazarSolicitud(100L, 10L, 1L, "Documentacion incompleta");

        assertEquals(EstadoSolicitud.RECHAZADA, solicitud.getEstado());
        assertEquals("Documentacion incompleta", solicitud.getMotivoRechazo());
        assertNotNull(solicitud.getFechaRechazo());

        ArgumentCaptor<SolicitudRechazadaEvent> captor =
                ArgumentCaptor.forClass(SolicitudRechazadaEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals("Documentacion incompleta", captor.getValue().motivo());
    }

    @Test
    void rechazarSolicitud_motivoVacio_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> solicitudService.rechazarSolicitud(100L, 10L, 1L, ""));
    }

    @Test
    void rechazarSolicitud_estadoIncorrecto_lanzaEstadoInvalido() {
        solicitud.setEstado(EstadoSolicitud.APROBADA);
        when(solicitudRepository.findByIdForUpdate(100)).thenReturn(Optional.of(solicitud));
        when(empleadoRepository.findByUsuarioId(10)).thenReturn(Optional.of(empleado));

        assertThrows(EstadoInvalidoException.class,
                () -> solicitudService.rechazarSolicitud(100L, 10L, 1L, "Motivo"));
    }

    // ── cancelarSolicitud() ──

    @Test
    void cancelarSolicitud_ciudadanoCancelaSuSolicitudPendiente_cambiaEstado() {
        when(solicitudRepository.findByIdForUpdate(100)).thenReturn(Optional.of(solicitud));
        mockRlsContext();
        doNothing().when(entityManager).flush();

        solicitudService.cancelarSolicitud(100, 1);

        assertEquals(EstadoSolicitud.CANCELADA, solicitud.getEstado());
        assertNotNull(solicitud.getFechaCancelacion());
        verify(solicitudRepository).save(solicitud);

        ArgumentCaptor<SolicitudCanceladaEvent> captor =
                ArgumentCaptor.forClass(SolicitudCanceladaEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(100, captor.getValue().solicitudId());
    }

    @Test
    void cancelarSolicitud_solicitudNoPerteneceAlCiudadano_lanzaAccesoDenegado() {
        when(solicitudRepository.findByIdForUpdate(100)).thenReturn(Optional.of(solicitud));

        assertThrows(AccesoDenegadoException.class,
                () -> solicitudService.cancelarSolicitud(100, 999));
    }

    @Test
    void cancelarSolicitud_estadoNoPendiente_lanzaEstadoInvalido() {
        solicitud.setEstado(EstadoSolicitud.EN_REVISION);
        when(solicitudRepository.findByIdForUpdate(100)).thenReturn(Optional.of(solicitud));

        assertThrows(EstadoInvalidoException.class,
                () -> solicitudService.cancelarSolicitud(100, 1));
    }
}
