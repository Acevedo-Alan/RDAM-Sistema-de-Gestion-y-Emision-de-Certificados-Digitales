package com.rdam.service.impl;

import com.rdam.domain.entity.Circunscripcion;
import com.rdam.domain.entity.TipoCertificado;
import com.rdam.dto.CircunscripcionRequest;
import com.rdam.dto.CircunscripcionResponse;
import com.rdam.dto.TipoCertificadoRequest;
import com.rdam.dto.TipoCertificadoResponse;
import com.rdam.repository.CircunscripcionRepository;
import com.rdam.repository.TipoCertificadoRepository;
import com.rdam.service.CatalogoService;
import com.rdam.service.exception.RecursoNoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class CatalogoServiceImpl implements CatalogoService {

    private static final Logger log = LoggerFactory.getLogger(CatalogoServiceImpl.class);

    private final TipoCertificadoRepository tipoCertificadoRepository;
    private final CircunscripcionRepository circunscripcionRepository;

    public CatalogoServiceImpl(TipoCertificadoRepository tipoCertificadoRepository,
                               CircunscripcionRepository circunscripcionRepository) {
        this.tipoCertificadoRepository = tipoCertificadoRepository;
        this.circunscripcionRepository = circunscripcionRepository;
    }

    // ── Tipos de certificado ──

    @Override
    @Transactional(readOnly = true)
    public List<TipoCertificadoResponse> listarTiposCertificadoActivos() {
        log.info("action=LISTAR_TIPOS_CERTIFICADO_ACTIVOS");
        return tipoCertificadoRepository.findByActivoTrue()
                .stream()
                .map(this::toTipoCertificadoResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TipoCertificadoResponse> listarTodosTiposCertificado() {
        log.info("action=LISTAR_TODOS_TIPOS_CERTIFICADO");
        return tipoCertificadoRepository.findAll()
                .stream()
                .map(this::toTipoCertificadoResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TipoCertificadoResponse obtenerTipoCertificado(Integer id) {
        log.info("action=OBTENER_TIPO_CERTIFICADO id={}", id);
        TipoCertificado tipo = tipoCertificadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de certificado no encontrado con id=" + id));
        return toTipoCertificadoResponse(tipo);
    }

    @Override
    @Transactional
    public TipoCertificadoResponse crearTipoCertificado(TipoCertificadoRequest request) {
        log.info("action=CREAR_TIPO_CERTIFICADO nombre={}", request.nombre());

        tipoCertificadoRepository.findByNombreAndEliminadoEnIsNull(request.nombre()).ifPresent(t -> {
            throw new IllegalArgumentException("Ya existe un tipo de certificado activo con nombre=" + request.nombre());
        });

        OffsetDateTime now = OffsetDateTime.now();

        TipoCertificado tipo = new TipoCertificado();
        tipo.setNombre(request.nombre());
        tipo.setDescripcion(request.descripcion());
        tipo.setActivo(true);
        tipo.setCreatedAt(now);
        tipo.setUpdatedAt(now);

        tipo = tipoCertificadoRepository.save(tipo);

        log.info("action=TIPO_CERTIFICADO_CREADO id={}", tipo.getId());
        return toTipoCertificadoResponse(tipo);
    }

    @Override
    @Transactional
    public TipoCertificadoResponse editarTipoCertificado(Integer id, TipoCertificadoRequest request) {
        log.info("action=EDITAR_TIPO_CERTIFICADO id={}", id);

        TipoCertificado tipo = tipoCertificadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de certificado no encontrado con id=" + id));

        tipoCertificadoRepository.findByNombreAndEliminadoEnIsNull(request.nombre()).ifPresent(t -> {
            if (!t.getId().equals(id)) {
                throw new IllegalArgumentException("Ya existe un tipo de certificado activo con nombre=" + request.nombre());
            }
        });

        tipo.setNombre(request.nombre());
        tipo.setDescripcion(request.descripcion());
        tipo.setUpdatedAt(OffsetDateTime.now());

        tipo = tipoCertificadoRepository.save(tipo);

        log.info("action=TIPO_CERTIFICADO_EDITADO id={}", id);
        return toTipoCertificadoResponse(tipo);
    }

    @Override
    @Transactional
    public void desactivarTipoCertificado(Integer id) {
        log.info("action=DESACTIVAR_TIPO_CERTIFICADO id={}", id);

        TipoCertificado tipo = tipoCertificadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de certificado no encontrado con id=" + id));

        OffsetDateTime now = OffsetDateTime.now();
        tipo.setEliminadoEn(now);
        tipo.setActivo(false);
        tipo.setUpdatedAt(now);
        tipoCertificadoRepository.save(tipo);

        log.info("action=TIPO_CERTIFICADO_DESACTIVADO id={}", id);
    }

    @Override
    @Transactional
    public void activarTipoCertificado(Integer id) {
        log.info("action=ACTIVAR_TIPO_CERTIFICADO id={}", id);

        TipoCertificado tipo = tipoCertificadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de certificado no encontrado con id=" + id));

        tipo.setEliminadoEn(null);
        tipo.setActivo(true);
        tipo.setUpdatedAt(OffsetDateTime.now());
        tipoCertificadoRepository.save(tipo);

        log.info("action=TIPO_CERTIFICADO_ACTIVADO id={}", id);
    }

    // ── Circunscripciones ──

    @Override
    @Transactional(readOnly = true)
    public List<CircunscripcionResponse> listarCircunscripcionesActivas() {
        log.info("action=LISTAR_CIRCUNSCRIPCIONES_ACTIVAS");
        return circunscripcionRepository.findByActivoTrue()
                .stream()
                .map(this::toCircunscripcionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CircunscripcionResponse> listarTodasCircunscripciones() {
        log.info("action=LISTAR_TODAS_CIRCUNSCRIPCIONES");
        return circunscripcionRepository.findAll()
                .stream()
                .map(this::toCircunscripcionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CircunscripcionResponse obtenerCircunscripcion(Integer id) {
        log.info("action=OBTENER_CIRCUNSCRIPCION id={}", id);
        Circunscripcion circ = circunscripcionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Circunscripción no encontrada con id=" + id));
        return toCircunscripcionResponse(circ);
    }

    @Override
    @Transactional
    public CircunscripcionResponse crearCircunscripcion(CircunscripcionRequest request) {
        log.info("action=CREAR_CIRCUNSCRIPCION codigo={}", request.codigo());

        circunscripcionRepository.findByCodigo(request.codigo()).ifPresent(c -> {
            throw new IllegalArgumentException("Ya existe una circunscripción con codigo=" + request.codigo());
        });

        OffsetDateTime now = OffsetDateTime.now();

        Circunscripcion circ = new Circunscripcion();
        circ.setNombre(request.nombre());
        circ.setCodigo(request.codigo());
        circ.setDescripcion(request.descripcion());
        circ.setActivo(true);
        circ.setCreatedAt(now);
        circ.setUpdatedAt(now);

        circ = circunscripcionRepository.save(circ);

        log.info("action=CIRCUNSCRIPCION_CREADA id={}", circ.getId());
        return toCircunscripcionResponse(circ);
    }

    @Override
    @Transactional
    public CircunscripcionResponse editarCircunscripcion(Integer id, CircunscripcionRequest request) {
        log.info("action=EDITAR_CIRCUNSCRIPCION id={}", id);

        Circunscripcion circ = circunscripcionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Circunscripción no encontrada con id=" + id));

        circ.setNombre(request.nombre());
        circ.setDescripcion(request.descripcion());
        circ.setUpdatedAt(OffsetDateTime.now());

        circ = circunscripcionRepository.save(circ);

        log.info("action=CIRCUNSCRIPCION_EDITADA id={}", id);
        return toCircunscripcionResponse(circ);
    }

    @Override
    @Transactional
    public void desactivarCircunscripcion(Integer id) {
        log.info("action=DESACTIVAR_CIRCUNSCRIPCION id={}", id);

        Circunscripcion circ = circunscripcionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Circunscripción no encontrada con id=" + id));

        circ.setActivo(false);
        circ.setUpdatedAt(OffsetDateTime.now());
        circunscripcionRepository.save(circ);

        log.info("action=CIRCUNSCRIPCION_DESACTIVADA id={}", id);
    }

    @Override
    @Transactional
    public void activarCircunscripcion(Integer id) {
        log.info("action=ACTIVAR_CIRCUNSCRIPCION id={}", id);

        Circunscripcion circ = circunscripcionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Circunscripción no encontrada con id=" + id));

        circ.setActivo(true);
        circ.setUpdatedAt(OffsetDateTime.now());
        circunscripcionRepository.save(circ);

        log.info("action=CIRCUNSCRIPCION_ACTIVADA id={}", id);
    }

    // ── Mappers ──

    private TipoCertificadoResponse toTipoCertificadoResponse(TipoCertificado tipo) {
        return new TipoCertificadoResponse(
                tipo.getId(),
                tipo.getNombre(),
                tipo.getDescripcion(),
                tipo.getActivo(),
                tipo.getCreatedAt(),
                tipo.getEliminadoEn()
        );
    }

    private CircunscripcionResponse toCircunscripcionResponse(Circunscripcion circ) {
        return new CircunscripcionResponse(
                circ.getId(),
                circ.getNombre(),
                circ.getCodigo(),
                circ.getDescripcion(),
                circ.getActivo(),
                circ.getCreatedAt()
        );
    }
}
