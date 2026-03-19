package com.rdam.service;

import com.rdam.domain.entity.EstadoSolicitud;

import java.time.OffsetDateTime;

public interface ReporteService {

    byte[] generarSolicitudesCsv(OffsetDateTime desde, OffsetDateTime hasta,
                                  EstadoSolicitud estado, Integer circunscripcionId);

    byte[] generarUsuariosCsv();
}
