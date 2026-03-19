package com.rdam.service;

import com.rdam.domain.entity.Circunscripcion;
import com.rdam.domain.entity.EstadoSolicitud;
import com.rdam.domain.entity.RolUsuario;
import com.rdam.dto.DashboardAdminResponse;
import com.rdam.dto.DashboardInternoResponse;
import com.rdam.repository.CircunscripcionRepository;
import com.rdam.repository.SolicitudRepository;
import com.rdam.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DashboardCacheService {

    private static final Logger log = LoggerFactory.getLogger(DashboardCacheService.class);

    private final SolicitudRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;
    private final CircunscripcionRepository circunscripcionRepository;

    private volatile DashboardAdminResponse adminSnapshot;
    private final ConcurrentHashMap<Integer, DashboardInternoResponse> internoSnapshots = new ConcurrentHashMap<>();

    public DashboardCacheService(SolicitudRepository solicitudRepository,
                                 UsuarioRepository usuarioRepository,
                                 CircunscripcionRepository circunscripcionRepository) {
        this.solicitudRepository = solicitudRepository;
        this.usuarioRepository = usuarioRepository;
        this.circunscripcionRepository = circunscripcionRepository;
    }

    @Scheduled(fixedDelay = 300_000)
    public void refrescarCache() {
        log.info("action=DASHBOARD_CACHE_REFRESH status=inicio");
        try {
            refrescarAdmin();
            refrescarInterno();
            log.info("action=DASHBOARD_CACHE_REFRESH status=completado");
        } catch (Exception e) {
            log.error("action=DASHBOARD_CACHE_REFRESH status=error mensaje={}", e.getMessage(), e);
        }
    }

    private void refrescarAdmin() {
        long totalSolicitudes = solicitudRepository.count();

        Map<String, Long> solicitudesPorEstado = buildEstadoMap(solicitudRepository.countGroupByEstado());

        Long solicitudesHoy = solicitudRepository.countHoy();

        OffsetDateTime inicioSemana = OffsetDateTime.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .toLocalDate()
                .atStartOfDay()
                .atOffset(OffsetDateTime.now().getOffset());
        Long solicitudesEstaSemana = solicitudRepository.countDesde(inicioSemana);

        Long totalCiudadanos = usuarioRepository.countByRolAndActivoTrue(RolUsuario.ciudadano);
        Long totalEmpleados = usuarioRepository.countByRolAndActivoTrue(RolUsuario.interno)
                + usuarioRepository.countByRolAndActivoTrue(RolUsuario.admin);

        BigDecimal montoRecaudado = solicitudRepository.sumMontoByEstados(
                List.of(EstadoSolicitud.PAGADO, EstadoSolicitud.PUBLICADO));

        Map<String, Long> solicitudesPorCircunscripcion = new LinkedHashMap<>();
        for (Object[] row : solicitudRepository.countGroupByCircunscripcion()) {
            solicitudesPorCircunscripcion.put((String) row[0], (Long) row[1]);
        }

        this.adminSnapshot = new DashboardAdminResponse(
                totalSolicitudes,
                solicitudesPorEstado,
                solicitudesHoy != null ? solicitudesHoy : 0L,
                solicitudesEstaSemana != null ? solicitudesEstaSemana : 0L,
                totalCiudadanos != null ? totalCiudadanos : 0L,
                totalEmpleados,
                montoRecaudado,
                solicitudesPorCircunscripcion
        );
    }

    private void refrescarInterno() {
        List<Circunscripcion> circunscripciones = circunscripcionRepository.findAll();
        for (Circunscripcion circ : circunscripciones) {
            Integer circId = circ.getId();

            Map<String, Long> estadoMap = buildEstadoMap(
                    solicitudRepository.countGroupByEstadoByCircunscripcion(circId));

            long total = estadoMap.values().stream().mapToLong(Long::longValue).sum();

            Long hoy = solicitudRepository.countHoyByCircunscripcion(circId);

            long pendientes = estadoMap.getOrDefault(EstadoSolicitud.PENDIENTE.name(), 0L);
            long enRevision = 0L;

            internoSnapshots.put(circId, new DashboardInternoResponse(
                    circ.getNombre(),
                    total,
                    estadoMap,
                    hoy != null ? hoy : 0L,
                    pendientes,
                    enRevision
            ));
        }
    }

    private Map<String, Long> buildEstadoMap(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (EstadoSolicitud estado : EstadoSolicitud.values()) {
            map.put(estado.name(), 0L);
        }
        for (Object[] row : rows) {
            EstadoSolicitud estado = (EstadoSolicitud) row[0];
            Long count = (Long) row[1];
            map.put(estado.name(), count);
        }
        return map;
    }

    public DashboardAdminResponse getAdminSnapshot() {
        return adminSnapshot;
    }

    public DashboardInternoResponse getInternoSnapshot(Integer circunscripcionId) {
        return internoSnapshots.get(circunscripcionId);
    }
}
