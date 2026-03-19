package com.rdam.service;

import com.rdam.dto.HistorialEstadoResponse;

import java.util.List;

public interface HistorialService {

    List<HistorialEstadoResponse> obtenerHistorial(Integer solicitudId, Integer userId, String rol);

    List<HistorialEstadoResponse> obtenerHistorialBandeja(Integer userId, String rol, Integer circunscripcionId);
}
