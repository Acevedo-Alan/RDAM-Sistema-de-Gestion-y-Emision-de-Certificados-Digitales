package com.rdam.service;

import com.rdam.dto.AdjuntoResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AdjuntoService {

    AdjuntoResponse subirAdjunto(Integer solicitudId, Integer ciudadanoId, MultipartFile archivo);

    List<AdjuntoResponse> listarAdjuntos(Integer solicitudId, Integer userId, String rol);

    byte[] descargarAdjunto(Integer solicitudId, Integer adjuntoId, Integer userId, String rol);

    String obtenerNombreOriginal(Integer solicitudId, Integer adjuntoId, Integer userId, String rol);

    String obtenerMimeType(Integer solicitudId, Integer adjuntoId, Integer userId, String rol);

    void eliminarAdjunto(Integer solicitudId, Integer adjuntoId, Integer ciudadanoId);
}
