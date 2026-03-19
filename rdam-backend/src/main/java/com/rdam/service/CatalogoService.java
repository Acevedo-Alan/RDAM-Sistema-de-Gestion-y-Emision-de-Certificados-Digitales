package com.rdam.service;

import com.rdam.dto.CircunscripcionRequest;
import com.rdam.dto.CircunscripcionResponse;
import com.rdam.dto.TipoCertificadoRequest;
import com.rdam.dto.TipoCertificadoResponse;

import java.util.List;

public interface CatalogoService {

    // ── Tipos de certificado ──

    List<TipoCertificadoResponse> listarTiposCertificadoActivos();

    List<TipoCertificadoResponse> listarTodosTiposCertificado();

    TipoCertificadoResponse obtenerTipoCertificado(Integer id);

    TipoCertificadoResponse crearTipoCertificado(TipoCertificadoRequest request);

    TipoCertificadoResponse editarTipoCertificado(Integer id, TipoCertificadoRequest request);

    void desactivarTipoCertificado(Integer id);

    void activarTipoCertificado(Integer id);

    // ── Circunscripciones ──

    List<CircunscripcionResponse> listarCircunscripcionesActivas();

    List<CircunscripcionResponse> listarTodasCircunscripciones();

    CircunscripcionResponse obtenerCircunscripcion(Integer id);

    CircunscripcionResponse crearCircunscripcion(CircunscripcionRequest request);

    CircunscripcionResponse editarCircunscripcion(Integer id, CircunscripcionRequest request);

    void desactivarCircunscripcion(Integer id);

    void activarCircunscripcion(Integer id);
}
