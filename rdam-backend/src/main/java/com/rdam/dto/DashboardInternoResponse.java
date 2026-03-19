package com.rdam.dto;

import java.util.Map;

public record DashboardInternoResponse(
        String circunscripcion,
        long totalSolicitudes,
        Map<String, Long> solicitudesPorEstado,
        long solicitudesHoy,
        long pendientesAsignacion,
        long enRevision
) {
}
