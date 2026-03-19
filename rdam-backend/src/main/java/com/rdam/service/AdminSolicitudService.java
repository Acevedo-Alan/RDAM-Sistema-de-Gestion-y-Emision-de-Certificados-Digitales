package com.rdam.service;

import com.rdam.domain.entity.EstadoSolicitud;
import com.rdam.dto.SolicitudAdminResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminSolicitudService {

    Page<SolicitudAdminResponse> listarSolicitudes(EstadoSolicitud estado, Integer circunscripcionId, Pageable pageable);

    SolicitudAdminResponse obtenerSolicitud(Integer id);
}
