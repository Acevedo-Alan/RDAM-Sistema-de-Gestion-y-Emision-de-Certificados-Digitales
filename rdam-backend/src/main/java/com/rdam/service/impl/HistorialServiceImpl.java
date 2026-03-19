package com.rdam.service.impl;

import com.rdam.domain.entity.HistorialEstado;
import com.rdam.domain.entity.Solicitud;
import com.rdam.domain.entity.Usuario;
import com.rdam.dto.HistorialEstadoResponse;
import com.rdam.repository.HistorialEstadoRepository;
import com.rdam.repository.SolicitudRepository;
import com.rdam.repository.UsuarioRepository;
import com.rdam.service.HistorialService;
import com.rdam.service.exception.AccesoDenegadoException;
import com.rdam.service.exception.RecursoNoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class HistorialServiceImpl implements HistorialService {

    private static final Logger log = LoggerFactory.getLogger(HistorialServiceImpl.class);

    private final SolicitudRepository solicitudRepository;
    private final HistorialEstadoRepository historialEstadoRepository;
    private final UsuarioRepository usuarioRepository;

    public HistorialServiceImpl(SolicitudRepository solicitudRepository,
                                HistorialEstadoRepository historialEstadoRepository,
                                UsuarioRepository usuarioRepository) {
        this.solicitudRepository = solicitudRepository;
        this.historialEstadoRepository = historialEstadoRepository;
        this.usuarioRepository = usuarioRepository;
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

        Map<Integer, Usuario> usuariosMap = buildUsuariosMap(historial);

        return historial.stream()
                .map(h -> mapToResponse(h, usuariosMap))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistorialEstadoResponse> obtenerHistorialBandeja(Integer userId, String rol, Integer circunscripcionId) {
        List<HistorialEstado> historial;

        if ("admin".equalsIgnoreCase(rol)) {
            historial = historialEstadoRepository.findAllByOrderByCreatedAtDesc();
        } else {
            historial = historialEstadoRepository
                    .findBySolicitud_Circunscripcion_IdOrderByCreatedAtDesc(circunscripcionId);
        }

        log.info("action=OBTENER_HISTORIAL_BANDEJA userId={} rol={} circunscripcionId={} cantidad={}",
                userId, rol, circunscripcionId, historial.size());

        Map<Integer, Usuario> usuariosMap = buildUsuariosMap(historial);

        return historial.stream()
                .map(h -> mapToResponse(h, usuariosMap))
                .toList();
    }

    private Map<Integer, Usuario> buildUsuariosMap(List<HistorialEstado> historial) {
        Set<Integer> usuarioIds = historial.stream()
                .map(HistorialEstado::getUsuarioId)
                .collect(Collectors.toSet());

        return usuarioRepository.findAllById(usuarioIds).stream()
                .collect(Collectors.toMap(Usuario::getId, Function.identity()));
    }

    private String resolveUsuarioNombre(Integer usuarioId, Map<Integer, Usuario> usuariosMap) {
        if (usuarioId == null) {
            return "Sistema";
        }
        Usuario usuario = usuariosMap.get(usuarioId);
        if (usuario == null) {
            return "Sistema";
        }
        return usuario.getApellido() + ", " + usuario.getNombre();
    }

    private HistorialEstadoResponse mapToResponse(HistorialEstado h, Map<Integer, Usuario> usuariosMap) {
        return new HistorialEstadoResponse(
                h.getId(),
                h.getEstadoAnterior() != null ? h.getEstadoAnterior().name() : null,
                h.getEstadoNuevo().name(),
                h.getUsuarioId(),
                resolveUsuarioNombre(h.getUsuarioId(), usuariosMap),
                h.getEmpleadoId(),
                h.getComentario(),
                h.getIpOrigen(),
                h.getCreatedAt()
        );
    }
}
