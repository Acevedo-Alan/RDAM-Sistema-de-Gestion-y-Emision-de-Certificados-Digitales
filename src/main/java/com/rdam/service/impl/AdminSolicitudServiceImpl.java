package com.rdam.service.impl;

import com.rdam.domain.entity.EstadoSolicitud;
import com.rdam.domain.entity.Solicitud;
import com.rdam.dto.SolicitudAdminResponse;
import com.rdam.repository.SolicitudRepository;
import com.rdam.service.AdminSolicitudService;
import com.rdam.service.exception.RecursoNoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminSolicitudServiceImpl implements AdminSolicitudService {

    private static final Logger log = LoggerFactory.getLogger(AdminSolicitudServiceImpl.class);

    private final SolicitudRepository solicitudRepository;

    public AdminSolicitudServiceImpl(SolicitudRepository solicitudRepository) {
        this.solicitudRepository = solicitudRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SolicitudAdminResponse> listarSolicitudes(EstadoSolicitud estado,
                                                           Integer circunscripcionId,
                                                           Pageable pageable) {
        log.info("action=ADMIN_LISTAR_SOLICITUDES estado={} circunscripcionId={}", estado, circunscripcionId);

        Page<Solicitud> solicitudes;

        if (estado != null && circunscripcionId != null) {
            solicitudes = solicitudRepository.findByEstadoAndCircunscripcionId(estado, circunscripcionId, pageable);
        } else if (estado != null) {
            solicitudes = solicitudRepository.findByEstado(estado, pageable);
        } else if (circunscripcionId != null) {
            solicitudes = solicitudRepository.findByCircunscripcionId(circunscripcionId, pageable);
        } else {
            solicitudes = solicitudRepository.findAll(pageable);
        }

        return solicitudes.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SolicitudAdminResponse obtenerSolicitud(Integer id) {
        log.info("action=ADMIN_OBTENER_SOLICITUD id={}", id);
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada con id=" + id));
        return toResponse(solicitud);
    }

    private SolicitudAdminResponse toResponse(Solicitud s) {
        String ciudadanoNombre = s.getCiudadano().getNombre() + " " + s.getCiudadano().getApellido();
        String empleadoNombre = s.getEmpleadoAsignado() != null
                ? s.getEmpleadoAsignado().getUsuario().getNombre() + " " + s.getEmpleadoAsignado().getUsuario().getApellido()
                : null;

        return new SolicitudAdminResponse(
                s.getId(),
                s.getEstado().name(),
                s.getTipoCertificado().getNombre(),
                s.getCircunscripcion().getNombre(),
                ciudadanoNombre,
                s.getMotivoRechazo(),
                empleadoNombre,
                s.getMontoArancel(),
                s.getCreatedAt(),
                s.getUpdatedAt(),
                s.getCiudadano().getId(),
                s.getCircunscripcion().getId(),
                s.getEmpleadoAsignado() != null ? s.getEmpleadoAsignado().getId() : null
        );
    }
}
