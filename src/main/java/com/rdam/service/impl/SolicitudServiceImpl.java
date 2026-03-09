package com.rdam.service.impl;

import com.rdam.domain.entity.Circunscripcion;
import com.rdam.domain.entity.Empleado;
import com.rdam.domain.entity.EstadoSolicitud;
import com.rdam.domain.entity.RolUsuario;
import com.rdam.domain.entity.Solicitud;
import com.rdam.domain.entity.TipoCertificado;
import com.rdam.domain.entity.Usuario;
import com.rdam.dto.SolicitudResponse;
import com.rdam.repository.CircunscripcionRepository;
import com.rdam.repository.EmpleadoRepository;
import com.rdam.repository.SolicitudRepository;
import com.rdam.repository.TipoCertificadoRepository;
import com.rdam.repository.UsuarioRepository;
import com.rdam.service.SolicitudService;
import com.rdam.service.event.SolicitudAprobadaEvent;
import com.rdam.service.event.SolicitudCanceladaEvent;
import com.rdam.service.event.SolicitudRechazadaEvent;
import com.rdam.service.exception.AccesoDenegadoException;
import com.rdam.service.exception.ConcurrenciaException;
import com.rdam.service.exception.EstadoInvalidoException;
import com.rdam.service.exception.RecursoNoEncontradoException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Implementación del servicio de negocio para solicitudes.
 *
 * Principios clave:
 * - La validación final de la FSM la realiza el trigger trg_solicitudes_state_machine en PostgreSQL.
 * - El historial de estados lo registra automáticamente el trigger trg_solicitudes_auto_sync_historial.
 * - RLS se activa con rdam_set_session_user() antes de cada save().
 * - Bloqueo pesimista (SELECT FOR UPDATE) en transiciones para evitar condiciones de carrera.
 * - Transacciones cortas: sin I/O externo ni lógica pesada dentro del @Transactional.
 */
@Service
public class SolicitudServiceImpl implements SolicitudService {

    private static final Logger log = LoggerFactory.getLogger(SolicitudServiceImpl.class);

    private final SolicitudRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final TipoCertificadoRepository tipoCertificadoRepository;
    private final CircunscripcionRepository circunscripcionRepository;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher eventPublisher;

    public SolicitudServiceImpl(SolicitudRepository solicitudRepository,
                                UsuarioRepository usuarioRepository,
                                EmpleadoRepository empleadoRepository,
                                TipoCertificadoRepository tipoCertificadoRepository,
                                CircunscripcionRepository circunscripcionRepository,
                                EntityManager entityManager,
                                ApplicationEventPublisher eventPublisher) {
        this.solicitudRepository = solicitudRepository;
        this.usuarioRepository = usuarioRepository;
        this.empleadoRepository = empleadoRepository;
        this.tipoCertificadoRepository = tipoCertificadoRepository;
        this.circunscripcionRepository = circunscripcionRepository;
        this.entityManager = entityManager;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public Solicitud crearSolicitud(Long ciudadanoId, Long tipoCertificadoId, Long circunscripcionId) {
        Usuario ciudadano = usuarioRepository.findById(ciudadanoId.intValue())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró el ciudadano con ID: " + ciudadanoId));

        TipoCertificado tipoCertificado = tipoCertificadoRepository.findById(tipoCertificadoId.intValue())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró el tipo de certificado con ID: " + tipoCertificadoId));

        Circunscripcion circunscripcion = circunscripcionRepository.findById(circunscripcionId.intValue())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la circunscripción con ID: " + circunscripcionId));

        Solicitud solicitud = new Solicitud();
        solicitud.setCiudadano(ciudadano);
        solicitud.setTipoCertificado(tipoCertificado);
        solicitud.setCircunscripcion(circunscripcion);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE_REVISION);
        solicitud.setMontoArancel(new BigDecimal("5000.00"));

        // Setear contexto RLS antes del save
        setRlsContext(ciudadano.getId(), RolUsuario.ciudadano);

        try {
            return solicitudRepository.save(solicitud);
        } catch (PersistenceException ex) {
            throw new AccesoDenegadoException(
                    "Acceso denegado al crear solicitud para ciudadano ID: " + ciudadanoId, ex);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void tomarSolicitudParaRevision(Long solicitudId, Long internoId, Long circunscripcionInternoId) {
        Solicitud solicitud = buscarSolicitudConLock(solicitudId);

        Empleado empleado = empleadoRepository.findByUsuarioId(internoId.intValue())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró el empleado con usuario ID: " + internoId));

        // Validación de circunscripción: el interno solo opera en su circunscripción
        if (!solicitud.getCircunscripcion().getId().equals(circunscripcionInternoId.intValue())) {
            throw new AccesoDenegadoException(
                    "El empleado no pertenece a la circunscripción de la solicitud. "
                    + "Circunscripción solicitud: " + solicitud.getCircunscripcion().getId()
                    + ", circunscripción empleado: " + circunscripcionInternoId);
        }

        // Validación previa simple: verificar estado actual antes de delegar al trigger
        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE_REVISION) {
            throw new EstadoInvalidoException(
                    "La solicitud no está en estado PENDIENTE_REVISION. Estado actual: "
                    + solicitud.getEstado());
        }

        // Setear contexto RLS ANTES de modificar la entidad managed
        // (Hibernate auto-flush dirty entities antes de native queries)
        setRlsContext(empleado.getUsuario().getId(), RolUsuario.interno);

        solicitud.setEstado(EstadoSolicitud.EN_REVISION);
        solicitud.setEmpleadoAsignado(empleado);
        solicitud.setFechaAsignacion(OffsetDateTime.now());

        guardarSolicitudConManejodeErrores(solicitud);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void aprobarSolicitud(Long solicitudId, Long internoId, Long circunscripcionInternoId) {
        Solicitud solicitud = buscarSolicitudConLock(solicitudId);

        Empleado empleado = empleadoRepository.findByUsuarioId(internoId.intValue())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró el empleado con usuario ID: " + internoId));

        // Validación de circunscripción
        if (!solicitud.getCircunscripcion().getId().equals(circunscripcionInternoId.intValue())) {
            throw new AccesoDenegadoException(
                    "El empleado no pertenece a la circunscripción de la solicitud. "
                    + "Circunscripción solicitud: " + solicitud.getCircunscripcion().getId()
                    + ", circunscripción empleado: " + circunscripcionInternoId);
        }

        // Validación previa simple
        if (solicitud.getEstado() != EstadoSolicitud.EN_REVISION) {
            throw new EstadoInvalidoException(
                    "La solicitud no está en estado EN_REVISION. Estado actual: "
                    + solicitud.getEstado());
        }

        // Setear contexto RLS ANTES de modificar la entidad managed
        setRlsContext(empleado.getUsuario().getId(), RolUsuario.interno);

        solicitud.setEstado(EstadoSolicitud.APROBADA);
        solicitud.setFechaAprobacion(OffsetDateTime.now());

        guardarSolicitudConManejodeErrores(solicitud);

        // Publicar evento (se ejecutará post-commit via @TransactionalEventListener)
        eventPublisher.publishEvent(new SolicitudAprobadaEvent(
                solicitud.getId(),
                solicitud.getCiudadano().getEmail(),
                solicitud.getTipoCertificado().getNombre(),
                solicitud.getMontoArancel()
        ));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void rechazarSolicitud(Long solicitudId, Long internoId, Long circunscripcionInternoId, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo de rechazo es obligatorio");
        }

        Solicitud solicitud = buscarSolicitudConLock(solicitudId);

        Empleado empleado = empleadoRepository.findByUsuarioId(internoId.intValue())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró el empleado con usuario ID: " + internoId));

        // Validación de circunscripción
        if (!solicitud.getCircunscripcion().getId().equals(circunscripcionInternoId.intValue())) {
            throw new AccesoDenegadoException(
                    "El empleado no pertenece a la circunscripción de la solicitud. "
                    + "Circunscripción solicitud: " + solicitud.getCircunscripcion().getId()
                    + ", circunscripción empleado: " + circunscripcionInternoId);
        }

        // Validación previa simple
        if (solicitud.getEstado() != EstadoSolicitud.EN_REVISION) {
            throw new EstadoInvalidoException(
                    "La solicitud no está en estado EN_REVISION. Estado actual: "
                    + solicitud.getEstado());
        }

        // Setear contexto RLS ANTES de modificar la entidad managed
        setRlsContext(empleado.getUsuario().getId(), RolUsuario.interno);

        solicitud.setMotivoRechazo(motivo);
        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        solicitud.setFechaRechazo(OffsetDateTime.now());

        guardarSolicitudConManejodeErrores(solicitud);

        // Publicar evento (se ejecutará post-commit via @TransactionalEventListener)
        eventPublisher.publishEvent(new SolicitudRechazadaEvent(
                solicitud.getId(),
                solicitud.getCiudadano().getEmail(),
                solicitud.getTipoCertificado().getNombre(),
                motivo
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public SolicitudResponse obtenerPorId(Integer solicitudId, Integer userId, String rol, Integer circunscripcionId) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la solicitud con ID: " + solicitudId));

        // Control de acceso según rol
        if ("ciudadano".equals(rol)) {
            if (!solicitud.getCiudadano().getId().equals(userId)) {
                throw new AccesoDenegadoException(
                        "El ciudadano no tiene acceso a la solicitud ID: " + solicitudId);
            }
        } else if ("interno".equals(rol)) {
            if (!solicitud.getCircunscripcion().getId().equals(circunscripcionId)) {
                throw new AccesoDenegadoException(
                        "El empleado no pertenece a la circunscripción de la solicitud ID: " + solicitudId);
            }
        }
        // admin: sin restricción

        log.info("action=OBTENER_SOLICITUD solicitudId={} userId={} rol={}", solicitudId, userId, rol);

        return mapToResponse(solicitud);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudResponse> misSolicitudes(Integer ciudadanoId) {
        List<Solicitud> solicitudes = solicitudRepository.findByCiudadanoIdOrderByCreatedAtDesc(ciudadanoId);

        log.info("action=MIS_SOLICITUDES ciudadanoId={} cantidad={}", ciudadanoId, solicitudes.size());

        return solicitudes.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudResponse> bandeja(Integer circunscripcionId, String rol) {
        List<EstadoSolicitud> estados = List.of(
                EstadoSolicitud.PENDIENTE_REVISION,
                EstadoSolicitud.EN_REVISION
        );

        List<Solicitud> solicitudes;
        if ("admin".equals(rol)) {
            solicitudes = solicitudRepository.findByEstadoInOrderByCreatedAtAsc(estados);
        } else {
            solicitudes = solicitudRepository.findByCircunscripcionIdAndEstadoInOrderByCreatedAtAsc(
                    circunscripcionId, estados);
        }

        log.info("action=BANDEJA rol={} circunscripcionId={} cantidad={}", rol, circunscripcionId, solicitudes.size());

        return solicitudes.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void cancelarSolicitud(Integer solicitudId, Integer ciudadanoId) {
        Solicitud solicitud = buscarSolicitudConLock(solicitudId.longValue());

        // Validar propiedad
        if (!solicitud.getCiudadano().getId().equals(ciudadanoId)) {
            throw new AccesoDenegadoException(
                    "El ciudadano no tiene acceso a la solicitud ID: " + solicitudId);
        }

        // Solo se puede cancelar en PENDIENTE_REVISION
        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE_REVISION) {
            throw new EstadoInvalidoException(
                    "Solo se puede cancelar una solicitud en estado PENDIENTE_REVISION");
        }

        // Setear contexto RLS ANTES de modificar la entidad managed
        setRlsContext(ciudadanoId, RolUsuario.ciudadano);

        solicitud.setEstado(EstadoSolicitud.CANCELADA);
        solicitud.setFechaCancelacion(OffsetDateTime.now());

        guardarSolicitudConManejodeErrores(solicitud);

        log.info("action=CANCELAR_SOLICITUD solicitudId={} ciudadanoId={}", solicitudId, ciudadanoId);

        // Publicar evento post-commit para posibles notificaciones futuras
        eventPublisher.publishEvent(new SolicitudCanceladaEvent(
                solicitud.getId(),
                solicitud.getCiudadano().getEmail()
        ));
    }

    // ── Métodos privados ──

    /**
     * Busca una solicitud con bloqueo pesimista (SELECT FOR UPDATE).
     * OBLIGATORIO para todas las transiciones de estado.
     */
    private Solicitud buscarSolicitudConLock(Long solicitudId) {
        try {
            return solicitudRepository.findByIdForUpdate(solicitudId.intValue())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No se encontró la solicitud con ID: " + solicitudId));
        } catch (jakarta.persistence.PessimisticLockException ex) {
            throw new ConcurrenciaException(
                    "La solicitud ID " + solicitudId
                    + " está siendo procesada por otro usuario. Intente nuevamente.", ex);
        } catch (LockTimeoutException ex) {
            throw new ConcurrenciaException(
                    "Timeout al intentar bloquear la solicitud ID " + solicitudId
                    + ". Otro usuario la tiene bloqueada.", ex);
        }
    }

    /**
     * Setea el contexto de sesión RLS en PostgreSQL.
     * PRIVADO: debe ejecutarse dentro del mismo método @Transactional que hace el save().
     * NO tiene @Transactional propio para no abrir una transacción separada.
     */
    private void setRlsContext(Integer userId, RolUsuario rol) {
        entityManager.createNativeQuery(
                "SELECT rdam_set_session_user(:userId, CAST(:rol AS text)::rol_usuario)")
                .setParameter("userId", userId)
                .setParameter("rol", rol.name())
                .getSingleResult();
    }

    /**
     * Guarda la solicitud y mapea excepciones de la base de datos a excepciones de negocio.
     */
    private void guardarSolicitudConManejodeErrores(Solicitud solicitud) {
        try {
            solicitudRepository.save(solicitud);
            // Forzar flush para que los triggers se ejecuten dentro de esta transacción
            entityManager.flush();
        } catch (DataIntegrityViolationException ex) {
            // Transición inválida detectada por el trigger trg_solicitudes_state_machine
            throw new EstadoInvalidoException(
                    "Transición de estado rechazada por la base de datos: " + ex.getMostSpecificCause().getMessage(), ex);
        } catch (PersistenceException ex) {
            // Violación de RLS u otra restricción de persistencia
            String message = ex.getMessage() != null ? ex.getMessage() : "";
            if (message.contains("AUTH_VIOLATION") || message.contains("RLS")) {
                throw new AccesoDenegadoException(
                        "Acceso denegado por política de seguridad de la base de datos: "
                        + (ex.getCause() != null ? ex.getCause().getMessage() : message), ex);
            }
            // Cualquier otra PersistenceException se re-lanza sin suprimir
            throw ex;
        }
    }

    /**
     * Mapea una entidad Solicitud a su DTO de respuesta.
     */
    private SolicitudResponse mapToResponse(Solicitud s) {
        String tipoCertificado = s.getTipoCertificado() != null
                ? s.getTipoCertificado().getNombre() : null;
        String circunscripcion = s.getCircunscripcion() != null
                ? s.getCircunscripcion().getNombre() : null;
        String ciudadanoNombre = s.getCiudadano() != null
                ? s.getCiudadano().getNombre() + " " + s.getCiudadano().getApellido() : null;
        String empleadoAsignado = s.getEmpleadoAsignado() != null
                ? s.getEmpleadoAsignado().getLegajo() : null;

        return new SolicitudResponse(
                s.getId(),
                s.getEstado().name(),
                tipoCertificado,
                circunscripcion,
                ciudadanoNombre,
                s.getMotivoRechazo(),
                empleadoAsignado,
                s.getMontoArancel(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}
