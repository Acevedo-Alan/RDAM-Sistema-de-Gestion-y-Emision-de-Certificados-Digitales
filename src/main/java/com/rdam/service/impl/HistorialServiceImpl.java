package com.rdam.service.impl;

import com.rdam.domain.entity.HistorialEstado;
import com.rdam.domain.entity.Solicitud;
import com.rdam.dto.HistorialEstadoResponse;
import com.rdam.repository.HistorialEstadoRepository;
import com.rdam.repository.SolicitudRepository;
import com.rdam.service.HistorialService;
import com.rdam.service.exception.AccesoDenegadoException;
import com.rdam.service.exception.RecursoNoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HistorialServiceImpl implements HistorialService {

    private static final Logger log = LoggerFactory.getLogger(HistorialServiceImpl.class);

    private final SolicitudRepository solicitudRepository;
    private final HistorialEstadoRepository historialEstadoRepository;

    public HistorialServiceImpl(SolicitudRepository solicitudRepository,
                                HistorialEstadoRepository historialEstadoRepository) {
        this.solicitudRepository = solicitudRepository;
        this.historialEstadoRepository = historialEstadoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistorialEstadoResponse> obtenerHistorial(Integer solicitudId, Integer userId, String rol) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Solicitud no encontrada con id=" + solicitudId));

        if ("ciudadano".equalsIgnoreCase(rol)) {
            Integer ciudadanoId = solicitud.getCiudadano().getId();
            if (!userId.equals(ciudadanoId)) {
                log.warn("action=ACCESO_DENEGADO_HISTORIAL userId={} solicitudId={} propietarioId={}",
                        userId, solicitudId, ciudadanoId);
                throw new AccesoDenegadoException(
                        "No tiene permiso para ver el historial de la solicitud id=" + solicitudId);
            }
        }

        log.info("action=OBTENER_HISTORIAL solicitudId={} userId={} rol={}", solicitudId, userId, rol);

        List<HistorialEstado> historial = historialEstadoRepository
                .findBySolicitudIdOrderByCreatedAtDesc(solicitudId);

        return historial.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private HistorialEstadoResponse mapToResponse(HistorialEstado h) {
        return new HistorialEstadoResponse(
                h.getId(),
                h.getEstadoAnterior() != null ? h.getEstadoAnterior().name() : null,
                h.getEstadoNuevo().name(),
                h.getUsuarioId(),
                h.getEmpleadoId(),
                h.getComentario(),
                h.getIpOrigen(),
                h.getCreatedAt()
        );
    }
}
