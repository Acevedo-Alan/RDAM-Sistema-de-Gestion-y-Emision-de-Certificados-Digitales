package com.rdam.service.impl;

import com.rdam.domain.entity.EstadoSolicitud;
import com.rdam.domain.entity.Solicitud;
import com.rdam.domain.entity.Usuario;
import com.rdam.repository.SolicitudRepository;
import com.rdam.repository.UsuarioRepository;
import com.rdam.service.ReporteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReporteServiceImpl implements ReporteService {

    private static final Logger log = LoggerFactory.getLogger(ReporteServiceImpl.class);
    private static final DateTimeFormatter CSV_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final SolicitudRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;

    public ReporteServiceImpl(SolicitudRepository solicitudRepository,
                              UsuarioRepository usuarioRepository) {
        this.solicitudRepository = solicitudRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generarSolicitudesCsv(OffsetDateTime desde, OffsetDateTime hasta,
                                         EstadoSolicitud estado, Integer circunscripcionId) {
        log.info("action=ADMIN_REPORTE_SOLICITUDES_CSV desde={} hasta={} estado={} circunscripcionId={}",
                desde, hasta, estado, circunscripcionId);

        List<Solicitud> solicitudes = solicitudRepository.findAllWithFilters(desde, hasta, estado, circunscripcionId);

        StringBuilder sb = new StringBuilder();
        sb.append("id,estado,tipoCertificado,circunscripcion,ciudadano,empleadoAsignado,montoArancel,createdAt\n");

        for (Solicitud s : solicitudes) {
            sb.append(s.getId()).append(',');
            sb.append(s.getEstado().name()).append(',');
            sb.append(escapeCsv(s.getTipoCertificado().getNombre())).append(',');
            sb.append(escapeCsv(s.getCircunscripcion().getNombre())).append(',');
            sb.append(escapeCsv(s.getCiudadano().getNombre() + " " + s.getCiudadano().getApellido())).append(',');
            if (s.getEmpleadoAsignado() != null) {
                sb.append(escapeCsv(s.getEmpleadoAsignado().getUsuario().getNombre() + " "
                        + s.getEmpleadoAsignado().getUsuario().getApellido()));
            }
            sb.append(',');
            sb.append(s.getMontoArancel()).append(',');
            sb.append(s.getCreatedAt() != null ? s.getCreatedAt().format(CSV_DATE_FORMAT) : "");
            sb.append('\n');
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generarUsuariosCsv() {
        log.info("action=ADMIN_REPORTE_USUARIOS_CSV");

        List<Usuario> usuarios = usuarioRepository.findByActivoTrue();

        StringBuilder sb = new StringBuilder();
        sb.append("id,nombre,apellido,email,cuil,rol,activo,createdAt\n");

        for (Usuario u : usuarios) {
            sb.append(u.getId()).append(',');
            sb.append(escapeCsv(u.getNombre())).append(',');
            sb.append(escapeCsv(u.getApellido())).append(',');
            sb.append(escapeCsv(u.getEmail())).append(',');
            sb.append(u.getCuil() != null ? u.getCuil() : "").append(',');
            sb.append(u.getRol().name()).append(',');
            sb.append(u.getActivo()).append(',');
            sb.append(u.getCreatedAt() != null ? u.getCreatedAt().format(CSV_DATE_FORMAT) : "");
            sb.append('\n');
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
