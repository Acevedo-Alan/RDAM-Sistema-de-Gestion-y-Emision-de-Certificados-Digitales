package com.rdam.service;

import com.rdam.domain.entity.Solicitud;
import com.rdam.dto.SolicitudResponse;

import java.util.List;

/**
 * Servicio de negocio que gobierna el ciclo de vida de las solicitudes.
 * La validación final de transiciones de estado la realiza el trigger en PostgreSQL.
 */
public interface SolicitudService {

    /**
     * Crea una nueva solicitud con estado PENDIENTE_REVISION.
     *
     * @param ciudadanoId       ID del usuario ciudadano que solicita
     * @param tipoCertificadoId ID del tipo de certificado solicitado
     * @param circunscripcionId ID de la circunscripción responsable
     * @return la solicitud creada
     */
    Solicitud crearSolicitud(Long ciudadanoId, Long tipoCertificadoId, Long circunscripcionId);

    /**
     * Transición: PENDIENTE_REVISION → EN_REVISION.
     * El interno toma la solicitud para revisarla.
     *
     * @param solicitudId            ID de la solicitud
     * @param internoId              ID del empleado (tabla empleados) que toma la solicitud
     * @param circunscripcionInternoId ID de la circunscripción del empleado
     */
    void tomarSolicitudParaRevision(Long solicitudId, Long internoId, Long circunscripcionInternoId);

    /**
     * Transición: EN_REVISION → APROBADA.
     *
     * @param solicitudId            ID de la solicitud
     * @param internoId              ID del empleado que aprueba
     * @param circunscripcionInternoId ID de la circunscripción del empleado
     */
    void aprobarSolicitud(Long solicitudId, Long internoId, Long circunscripcionInternoId);

    /**
     * Transición: EN_REVISION → RECHAZADA.
     *
     * @param solicitudId            ID de la solicitud
     * @param internoId              ID del empleado que rechaza
     * @param circunscripcionInternoId ID de la circunscripción del empleado
     * @param motivo                 motivo obligatorio del rechazo
     */
    void rechazarSolicitud(Long solicitudId, Long internoId, Long circunscripcionInternoId, String motivo);

    /**
     * Obtiene el detalle de una solicitud por ID con control de acceso.
     *
     * @param solicitudId      ID de la solicitud
     * @param userId           ID del usuario autenticado
     * @param rol              rol del usuario ("ciudadano", "interno", "admin")
     * @param circunscripcionId circunscripción del empleado (null para ciudadanos)
     * @return DTO con el detalle de la solicitud
     */
    SolicitudResponse obtenerPorId(Integer solicitudId, Integer userId, String rol, Integer circunscripcionId);

    /**
     * Lista todas las solicitudes de un ciudadano, ordenadas por fecha de creación descendente.
     *
     * @param ciudadanoId ID del ciudadano
     * @return lista de DTOs de solicitudes
     */
    List<SolicitudResponse> misSolicitudes(Integer ciudadanoId);

    /**
     * Bandeja de trabajo para empleados internos/admin.
     * Retorna solicitudes en estados PENDIENTE_REVISION y EN_REVISION.
     *
     * @param circunscripcionId circunscripción del empleado (null para admin)
     * @param rol               rol del usuario ("interno" o "admin")
     * @return lista de DTOs de solicitudes
     */
    List<SolicitudResponse> bandeja(Integer circunscripcionId, String rol);

    /**
     * Transición: PENDIENTE_REVISION → CANCELADA.
     * Solo el ciudadano dueño puede cancelar su solicitud.
     *
     * @param solicitudId ID de la solicitud
     * @param ciudadanoId ID del ciudadano que cancela
     */
    void cancelarSolicitud(Integer solicitudId, Integer ciudadanoId);
}
