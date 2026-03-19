package com.rdam.service;

import com.rdam.domain.entity.Solicitud;
import com.rdam.dto.SolicitudResponse;

import java.util.List;

/**
 * Servicio de negocio que gobierna el ciclo de vida de las solicitudes.
 * La validacion final de transiciones de estado la realiza el trigger en PostgreSQL.
 */
public interface SolicitudService {

    /**
     * Crea una nueva solicitud con estado PENDIENTE.
     *
     * @param ciudadanoId       ID del usuario ciudadano que solicita
     * @param tipoCertificadoId ID del tipo de certificado solicitado
     * @param circunscripcionId ID de la circunscripcion responsable
     * @return la solicitud creada
     */
    Solicitud crearSolicitud(Long ciudadanoId, Long tipoCertificadoId, Long circunscripcionId);

    /**
     * Toma la solicitud para revision (interno).
     *
     * @param solicitudId ID de la solicitud
     * @param userId      ID del usuario autenticado
     * @param rol         rol del usuario
     * @return DTO con el detalle de la solicitud actualizada
     */
    SolicitudResponse tomarSolicitudParaRevision(Integer solicitudId, Integer userId, String rol);

    /**
     * Aprueba una solicitud PENDIENTE.
     *
     * @param solicitudId            ID de la solicitud
     * @param internoId              ID del empleado que aprueba
     * @param circunscripcionInternoId ID de la circunscripcion del empleado
     */
    void aprobarSolicitud(Long solicitudId, Long internoId, Long circunscripcionInternoId);

    /**
     * Transicion: PENDIENTE -> RECHAZADO.
     *
     * @param solicitudId            ID de la solicitud
     * @param internoId              ID del empleado que rechaza
     * @param circunscripcionInternoId ID de la circunscripcion del empleado
     * @param motivo                 motivo obligatorio del rechazo
     */
    void rechazarSolicitud(Long solicitudId, Long internoId, Long circunscripcionInternoId, String motivo);

    /**
     * Obtiene el detalle de una solicitud por ID con control de acceso.
     *
     * @param solicitudId      ID de la solicitud
     * @param userId           ID del usuario autenticado
     * @param rol              rol del usuario ("ciudadano", "interno", "admin")
     * @param circunscripcionId circunscripcion del empleado (null para ciudadanos)
     * @return DTO con el detalle de la solicitud
     */
    SolicitudResponse obtenerPorId(Integer solicitudId, Integer userId, String rol, Integer circunscripcionId);

    /**
     * Lista todas las solicitudes de un ciudadano, ordenadas por fecha de creacion descendente.
     *
     * @param ciudadanoId ID del ciudadano
     * @return lista de DTOs de solicitudes
     */
    List<SolicitudResponse> misSolicitudes(Integer ciudadanoId);

    /**
     * Bandeja de trabajo para empleados internos/admin.
     * Retorna solicitudes en estado PENDIENTE.
     *
     * @param circunscripcionId circunscripcion del empleado (null para admin)
     * @param rol               rol del usuario ("interno" o "admin")
     * @return lista de DTOs de solicitudes
     */
    List<SolicitudResponse> bandeja(Integer circunscripcionId, String rol, Integer userId);

    /**
     * Transicion: PENDIENTE -> CANCELADO.
     * Solo el ciudadano dueno puede cancelar su solicitud.
     *
     * @param solicitudId ID de la solicitud
     * @param ciudadanoId ID del ciudadano que cancela
     */
    void cancelarSolicitud(Integer solicitudId, Integer ciudadanoId);

    /**
     * Reasigna una solicitud PENDIENTE a otro empleado de la misma circunscripcion.
     * Solo ADMIN puede invocar este metodo.
     *
     * @param solicitudId     ID de la solicitud
     * @param nuevoEmpleadoId ID del empleado destino (usuario_id, no empleados.id)
     * @param adminId         ID del admin que ejecuta la accion
     */
    void reasignarSolicitud(Integer solicitudId, Integer nuevoEmpleadoId, Integer adminId);
}
