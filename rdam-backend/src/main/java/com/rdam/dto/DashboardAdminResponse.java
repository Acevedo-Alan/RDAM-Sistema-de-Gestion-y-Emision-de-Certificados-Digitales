package com.rdam.dto;

import java.math.BigDecimal;
import java.util.Map;

public record DashboardAdminResponse(
        long totalSolicitudes,
        Map<String, Long> solicitudesPorEstado,
        long solicitudesHoy,
        long solicitudesEstaSemana,
        long totalCiudadanos,
        long totalEmpleados,
        BigDecimal montoRecaudadoTotal,
        Map<String, Long> solicitudesPorCircunscripcion
) {
}
