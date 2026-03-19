package com.rdam.service.impl;

import com.rdam.domain.entity.Adjunto;
import com.rdam.domain.entity.EstadoSolicitud;
import com.rdam.domain.entity.Solicitud;
import com.rdam.dto.AdjuntoResponse;
import com.rdam.repository.AdjuntoRepository;
import com.rdam.repository.SolicitudRepository;
import com.rdam.service.AdjuntoService;
import com.rdam.service.exception.AccesoDenegadoException;
import com.rdam.service.exception.EstadoInvalidoException;
import com.rdam.service.exception.RecursoNoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AdjuntoServiceImpl implements AdjuntoService {

    private static final Logger log = LoggerFactory.getLogger(AdjuntoServiceImpl.class);

    private static final Set<String> MIME_PERMITIDOS = Set.of(
            "application/pdf", "image/jpeg", "image/png"
    );

    private static final Map<String, String> MIME_TO_EXTENSION = Map.of(
            "application/pdf", "pdf",
            "image/jpeg", "jpg",
            "image/png", "png"
    );

    private static final int MAX_TAMANO_BYTES = 5_242_880;
    private static final int MAX_ADJUNTOS = 3;

    private static final Set<EstadoSolicitud> ESTADOS_PERMITIDOS_UPLOAD = Set.of(
            EstadoSolicitud.PENDIENTE
    );

    private final AdjuntoRepository adjuntoRepository;
    private final SolicitudRepository solicitudRepository;
    private final Path storagePath;

    public AdjuntoServiceImpl(AdjuntoRepository adjuntoRepository,
                              SolicitudRepository solicitudRepository,
                              @Value("${adjuntos.storage-path}") String storagePath) {
        this.adjuntoRepository = adjuntoRepository;
        this.solicitudRepository = solicitudRepository;
        this.storagePath = Paths.get(storagePath);
    }

    @Override
    @Transactional
    public AdjuntoResponse subirAdjunto(Integer solicitudId, Integer ciudadanoId, MultipartFile archivo) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada con id=" + solicitudId));

        validarPropietario(solicitud, ciudadanoId);

        if (!ESTADOS_PERMITIDOS_UPLOAD.contains(solicitud.getEstado())) {
            throw new EstadoInvalidoException("Solo se pueden subir adjuntos en estado PENDIENTE");
        }

        String mimeType = archivo.getContentType();
        if (mimeType == null || !MIME_PERMITIDOS.contains(mimeType)) {
            throw new IllegalArgumentException("Tipo de archivo no permitido. Tipos aceptados: PDF, JPEG, PNG");
        }

        if (archivo.getSize() > MAX_TAMANO_BYTES) {
            throw new IllegalArgumentException("El archivo excede el tamaño máximo de 5 MB");
        }

        long count = adjuntoRepository.countBySolicitudId(solicitudId);
        if (count >= MAX_ADJUNTOS) {
            throw new EstadoInvalidoException("No se pueden subir más de 3 adjuntos por solicitud");
        }

        String extension = MIME_TO_EXTENSION.get(mimeType);
        String nombreStorage = UUID.randomUUID() + "." + extension;
        Path directorio = storagePath.resolve(String.valueOf(solicitudId));
        Path rutaArchivo = directorio.resolve(nombreStorage);

        try {
            Files.createDirectories(directorio);
            Files.write(rutaArchivo, archivo.getBytes());
        } catch (IOException e) {
            log.error("action=SUBIR_ADJUNTO error=escritura_disco solicitudId={} mensaje={}", solicitudId, e.getMessage());
            throw new RuntimeException("Error al guardar el archivo en disco", e);
        }

        Adjunto adjunto = new Adjunto();
        adjunto.setSolicitudId(solicitudId);
        adjunto.setNombreOriginal(archivo.getOriginalFilename());
        adjunto.setNombreStorage(nombreStorage);
        adjunto.setRutaStorage(rutaArchivo.toString());
        adjunto.setMimeType(mimeType);
        adjunto.setTamanoBytes((int) archivo.getSize());

        try {
            adjunto = adjuntoRepository.save(adjunto);
        } catch (DataIntegrityViolationException ex) {
            String msg = ex.getMostSpecificCause().getMessage();
            if (msg != null && msg.contains("3 adjuntos")) {
                throw new EstadoInvalidoException("No se pueden subir más de 3 adjuntos por solicitud");
            }
            throw ex;
        }

        log.info("action=SUBIR_ADJUNTO solicitudId={} adjuntoId={} nombreOriginal={} mimeType={} tamanoBytes={}",
                solicitudId, adjunto.getId(), adjunto.getNombreOriginal(), mimeType, adjunto.getTamanoBytes());

        return mapToResponse(adjunto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdjuntoResponse> listarAdjuntos(Integer solicitudId, Integer userId, String rol) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada con id=" + solicitudId));

        validarAccesoLectura(solicitud, userId, rol);

        List<Adjunto> adjuntos = adjuntoRepository.findBySolicitudIdOrderByCreatedAtAsc(solicitudId);
        return adjuntos.stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] descargarAdjunto(Integer solicitudId, Integer adjuntoId, Integer userId, String rol) {
        Adjunto adjunto = buscarAdjunto(solicitudId, adjuntoId, userId, rol);
        Path rutaArchivo = Paths.get(adjunto.getRutaStorage());

        if (!Files.exists(rutaArchivo)) {
            throw new RecursoNoEncontradoException("El archivo no existe en disco para adjuntoId=" + adjuntoId);
        }

        try {
            return Files.readAllBytes(rutaArchivo);
        } catch (IOException e) {
            log.error("action=DESCARGAR_ADJUNTO error=lectura_disco adjuntoId={} mensaje={}", adjuntoId, e.getMessage());
            throw new RuntimeException("Error al leer el archivo del disco", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String obtenerNombreOriginal(Integer solicitudId, Integer adjuntoId, Integer userId, String rol) {
        Adjunto adjunto = buscarAdjunto(solicitudId, adjuntoId, userId, rol);
        return adjunto.getNombreOriginal();
    }

    @Override
    @Transactional(readOnly = true)
    public String obtenerMimeType(Integer solicitudId, Integer adjuntoId, Integer userId, String rol) {
        Adjunto adjunto = buscarAdjunto(solicitudId, adjuntoId, userId, rol);
        return adjunto.getMimeType();
    }

    @Override
    @Transactional
    public void eliminarAdjunto(Integer solicitudId, Integer adjuntoId, Integer ciudadanoId) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada con id=" + solicitudId));

        validarPropietario(solicitud, ciudadanoId);

        if (!ESTADOS_PERMITIDOS_UPLOAD.contains(solicitud.getEstado())) {
            throw new EstadoInvalidoException("Solo se pueden eliminar adjuntos en estado PENDIENTE");
        }

        Adjunto adjunto = adjuntoRepository.findById(adjuntoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Adjunto no encontrado con id=" + adjuntoId));

        if (!adjunto.getSolicitudId().equals(solicitudId)) {
            throw new RecursoNoEncontradoException("Adjunto no pertenece a la solicitud indicada");
        }

        String rutaStorage = adjunto.getRutaStorage();
        adjuntoRepository.delete(adjunto);

        try {
            Path rutaArchivo = Paths.get(rutaStorage);
            Files.deleteIfExists(rutaArchivo);
            log.info("action=ELIMINAR_ADJUNTO adjuntoId={} solicitudId={} archivoEliminado=true", adjuntoId, solicitudId);
        } catch (IOException e) {
            log.warn("action=ELIMINAR_ADJUNTO adjuntoId={} solicitudId={} archivoEliminado=false mensaje={}",
                    adjuntoId, solicitudId, e.getMessage());
        }
    }

    private Adjunto buscarAdjunto(Integer solicitudId, Integer adjuntoId, Integer userId, String rol) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada con id=" + solicitudId));

        validarAccesoLectura(solicitud, userId, rol);

        Adjunto adjunto = adjuntoRepository.findById(adjuntoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Adjunto no encontrado con id=" + adjuntoId));

        if (!adjunto.getSolicitudId().equals(solicitudId)) {
            throw new RecursoNoEncontradoException("Adjunto no pertenece a la solicitud indicada");
        }

        return adjunto;
    }

    private void validarPropietario(Solicitud solicitud, Integer ciudadanoId) {
        if (!solicitud.getCiudadano().getId().equals(ciudadanoId)) {
            throw new AccesoDenegadoException("La solicitud no pertenece al ciudadano autenticado");
        }
    }

    private void validarAccesoLectura(Solicitud solicitud, Integer userId, String rol) {
        if ("ciudadano".equals(rol)) {
            if (!solicitud.getCiudadano().getId().equals(userId)) {
                throw new AccesoDenegadoException("No tiene acceso a los adjuntos de esta solicitud");
            }
        }
    }

    private AdjuntoResponse mapToResponse(Adjunto adjunto) {
        return new AdjuntoResponse(
                adjunto.getId(),
                adjunto.getSolicitudId(),
                adjunto.getNombreOriginal(),
                adjunto.getMimeType(),
                adjunto.getTamanoBytes(),
                adjunto.getCreatedAt()
        );
    }
}
