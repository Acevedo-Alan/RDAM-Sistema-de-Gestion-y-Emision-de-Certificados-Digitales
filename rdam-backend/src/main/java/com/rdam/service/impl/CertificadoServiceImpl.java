package com.rdam.service.impl;

import com.rdam.domain.entity.Certificado;
import com.rdam.domain.entity.Empleado;
import com.rdam.domain.entity.EstadoSolicitud;
import com.rdam.domain.entity.RolUsuario;
import com.rdam.domain.entity.Solicitud;
import com.rdam.domain.entity.Usuario;
import com.rdam.dto.VerificacionResponse;
import com.rdam.repository.CertificadoRepository;
import com.rdam.repository.EmpleadoRepository;
import com.rdam.repository.SolicitudRepository;
import com.rdam.service.CertificadoPdfService;
import com.rdam.service.CertificadoService;
import com.rdam.service.event.CertificadoEmitidoEvent;
import com.rdam.service.exception.AccesoDenegadoException;
import com.rdam.service.exception.ConcurrenciaException;
import com.rdam.service.exception.EstadoInvalidoException;
import com.rdam.service.exception.PdfGenerationException;
import com.rdam.service.exception.RecursoNoEncontradoException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Service
public class CertificadoServiceImpl implements CertificadoService {

    private static final Logger log = LoggerFactory.getLogger(CertificadoServiceImpl.class);
    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SolicitudRepository solicitudRepository;
    private final CertificadoRepository certificadoRepository;
    private final EmpleadoRepository empleadoRepository;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher eventPublisher;
    private final CertificadoPdfService certificadoPdfService;

    @Value("${adjuntos.storage-path:./adjuntos}")
    private String storagePath;

    public CertificadoServiceImpl(SolicitudRepository solicitudRepository,
                                   CertificadoRepository certificadoRepository,
                                   EmpleadoRepository empleadoRepository,
                                   EntityManager entityManager,
                                   ApplicationEventPublisher eventPublisher,
                                   CertificadoPdfService certificadoPdfService) {
        this.solicitudRepository = solicitudRepository;
        this.certificadoRepository = certificadoRepository;
        this.empleadoRepository = empleadoRepository;
        this.entityManager = entityManager;
        this.eventPublisher = eventPublisher;
        this.certificadoPdfService = certificadoPdfService;
    }

    // -- Emision de certificado (Fase 4 — legacy) --

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public Certificado emitirCertificado(Integer solicitudId, Integer empleadoId, Long circunscripcionId) {
        Solicitud solicitud = buscarSolicitudConLock(solicitudId);

        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro el empleado con ID: " + empleadoId));

        if (!solicitud.getCircunscripcion().getId().equals(circunscripcionId.intValue())) {
            throw new AccesoDenegadoException(
                    "El empleado no pertenece a la circunscripcion de la solicitud. "
                    + "Circunscripcion solicitud: " + solicitud.getCircunscripcion().getId()
                    + ", circunscripcion empleado: " + circunscripcionId);
        }

        if (solicitud.getEstado() != EstadoSolicitud.PAGADO) {
            throw new EstadoInvalidoException(
                    "La solicitud no esta en estado PAGADO. Estado actual: " + solicitud.getEstado());
        }

        if (certificadoRepository.existsBySolicitudId(solicitudId)) {
            throw new EstadoInvalidoException("Certificado ya emitido para solicitud: " + solicitudId);
        }

        String token = generarTokenCriptografico();
        String rutaPdf = "certificados/" + UUID.randomUUID() + ".pdf";
        String hashPdf = generarHashSha256("MOCK-PDF-CONTENT-" + solicitudId + "-" + System.currentTimeMillis());

        Certificado certificado = new Certificado();
        certificado.setSolicitud(solicitud);
        certificado.setToken(token);
        certificado.setRutaPdf(rutaPdf);
        certificado.setHashPdf(hashPdf);
        certificado.setPlantillaVer(1);
        certificado.setEmitidoPor(empleado);
        certificado.setFechaEmision(OffsetDateTime.now());

        setRlsContext(empleado.getUsuario().getId(), RolUsuario.interno);

        solicitud.setEstado(EstadoSolicitud.PUBLICADO);
        solicitud.setFechaEmision(OffsetDateTime.now());

        guardarConManejodeErrores(certificado, solicitud);
        entityManager.refresh(certificado);

        eventPublisher.publishEvent(new CertificadoEmitidoEvent(
                solicitudId,
                solicitud.getCiudadano().getEmail(),
                certificado.getNumeroCertificado()
        ));

        log.info("action=CERTIFICADO_EMITIDO solicitudId={} numero={}",
                solicitudId, certificado.getNumeroCertificado());

        return certificado;
    }

    // -- Generacion de PDF en memoria (Fase 5 — legacy) --

    @Override
    @Transactional(readOnly = true)
    public byte[] generarCertificadoPdf(Long solicitudId, Long ciudadanoId) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId.intValue())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro la solicitud con ID: " + solicitudId));

        if (!solicitud.getCiudadano().getId().equals(ciudadanoId.intValue())) {
            log.warn("action=ACCESO_DENEGADO_PDF solicitudId={} ciudadanoId={} propietarioId={}",
                    solicitudId, ciudadanoId, solicitud.getCiudadano().getId());
            throw new AccesoDenegadoException(
                    "No tiene permiso para descargar el certificado de la solicitud " + solicitudId);
        }

        EstadoSolicitud estado = solicitud.getEstado();
        if (estado != EstadoSolicitud.PAGADO && estado != EstadoSolicitud.PUBLICADO) {
            throw new EstadoInvalidoException(
                    "El certificado solo puede descargarse en estado PAGADO o PUBLICADO. Estado actual: " + estado);
        }

        String tokenFallback = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        byte[] pdfBytes = certificadoPdfService.generar(solicitud, tokenFallback);

        log.info("action=CERTIFICADO_GENERADO solicitudId={} ciudadanoId={} bytes={}",
                solicitudId, ciudadanoId, pdfBytes.length);

        return pdfBytes;
    }

    // -- Publicacion de certificado (Fase nueva) --

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public Certificado publicarCertificado(Integer solicitudId, Integer empleadoId, Long circunscripcionId) {
        Solicitud solicitud = buscarSolicitudConLock(solicitudId);

        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro el empleado con ID: " + empleadoId));

        if (!solicitud.getCircunscripcion().getId().equals(circunscripcionId.intValue())) {
            throw new AccesoDenegadoException(
                    "El empleado no pertenece a la circunscripcion de la solicitud. "
                    + "Circunscripcion solicitud: " + solicitud.getCircunscripcion().getId()
                    + ", circunscripcion empleado: " + circunscripcionId);
        }

        if (solicitud.getEstado() != EstadoSolicitud.PAGADO) {
            throw new EstadoInvalidoException(
                    "La solicitud no esta en estado PAGADO. Estado actual: " + solicitud.getEstado());
        }

        if (certificadoRepository.existsBySolicitudId(solicitudId)) {
            throw new EstadoInvalidoException("Certificado ya emitido para solicitud: " + solicitudId);
        }

        // Generar token criptografico de 64 caracteres hex
        String token = generarTokenCriptografico();

        // Generar PDF con iText 7
        byte[] pdfBytes = certificadoPdfService.generar(solicitud, token);

        // Guardar PDF en disco
        String rutaRelativa = "certificados/" + solicitud.getNumeroTramite() + ".pdf";
        guardarPdfEnDisco(pdfBytes, rutaRelativa);

        // Hash del contenido PDF real
        String hashPdf = generarHashSha256Bytes(pdfBytes);

        // Crear certificado
        Certificado certificado = new Certificado();
        certificado.setSolicitud(solicitud);
        certificado.setToken(token);
        certificado.setRutaPdf(rutaRelativa);
        certificado.setHashPdf(hashPdf);
        certificado.setPlantillaVer(1);
        certificado.setEmitidoPor(empleado);
        certificado.setFechaEmision(OffsetDateTime.now());

        // RLS antes de flush
        setRlsContext(empleado.getUsuario().getId(), RolUsuario.interno);

        // Transicion de estado
        solicitud.setEstado(EstadoSolicitud.PUBLICADO);
        solicitud.setFechaEmision(OffsetDateTime.now());

        guardarConManejodeErrores(certificado, solicitud);
        entityManager.refresh(certificado);

        // Publicar evento para email
        eventPublisher.publishEvent(new CertificadoEmitidoEvent(
                solicitudId,
                solicitud.getCiudadano().getEmail(),
                certificado.getNumeroCertificado()
        ));

        log.info("action=CERTIFICADO_PUBLICADO solicitudId={} numero={} token={}",
                solicitudId, certificado.getNumeroCertificado(), token.substring(0, 8) + "...");

        return certificado;
    }

    // -- Descarga de certificado desde disco --

    @Override
    @Transactional(readOnly = true)
    public byte[] descargarCertificado(Integer certificadoId, Integer userId, String rol) {
        Certificado certificado = certificadoRepository.findById(certificadoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro el certificado con ID: " + certificadoId));

        Solicitud solicitud = certificado.getSolicitud();

        // Validar acceso: ciudadano dueno o interno/admin
        boolean esDueno = solicitud.getCiudadano().getId().equals(userId);
        boolean esInternoOAdmin = "interno".equals(rol) || "admin".equals(rol);

        if (!esDueno && !esInternoOAdmin) {
            throw new AccesoDenegadoException(
                    "No tiene permiso para descargar el certificado " + certificadoId);
        }

        Path rutaArchivo = Paths.get(storagePath).resolve(certificado.getRutaPdf());
        if (!Files.exists(rutaArchivo)) {
            throw new RecursoNoEncontradoException(
                    "El archivo PDF del certificado no se encuentra en disco: " + certificado.getRutaPdf());
        }

        try {
            return Files.readAllBytes(rutaArchivo);
        } catch (IOException e) {
            throw new PdfGenerationException("Error al leer el PDF del certificado " + certificadoId, e);
        }
    }

    // -- Verificacion publica por token --

    @Override
    @Transactional(readOnly = true)
    public VerificacionResponse verificarCertificado(String token) {
        Optional<Certificado> opt = certificadoRepository.findByToken(token);

        if (opt.isEmpty()) {
            return VerificacionResponse.invalido();
        }

        Certificado certificado = opt.get();
        Solicitud solicitud = certificado.getSolicitud();

        // Si esta vencido, devolver invalido
        if (solicitud.getEstado() == EstadoSolicitud.PUBLICADO_VENCIDO) {
            return VerificacionResponse.invalido();
        }

        Usuario ciudadano = solicitud.getCiudadano();
        String titular = ciudadano.getApellido() + ", " + ciudadano.getNombre();
        String fechaEmision = certificado.getFechaEmision().format(FECHA_FMT);

        return new VerificacionResponse(
                true,
                solicitud.getNumeroTramite(),
                titular,
                solicitud.getTipoCertificado().getNombre(),
                solicitud.getCircunscripcion().getNombre(),
                fechaEmision
        );
    }

    // -- Metodos privados --

    private String generarTokenCriptografico() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        StringBuilder hex = new StringBuilder(64);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private void guardarPdfEnDisco(byte[] pdfBytes, String rutaRelativa) {
        Path rutaCompleta = Paths.get(storagePath).resolve(rutaRelativa);
        try {
            Files.createDirectories(rutaCompleta.getParent());
            Files.write(rutaCompleta, pdfBytes);
        } catch (IOException e) {
            throw new PdfGenerationException(
                    "Error al guardar el PDF en disco: " + rutaRelativa, e);
        }
    }

    private Solicitud buscarSolicitudConLock(Integer solicitudId) {
        try {
            return solicitudRepository.findByIdForUpdate(solicitudId)
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No se encontro la solicitud con ID: " + solicitudId));
        } catch (jakarta.persistence.PessimisticLockException ex) {
            throw new ConcurrenciaException(
                    "La solicitud ID " + solicitudId
                    + " esta siendo procesada por otro usuario. Intente nuevamente.", ex);
        } catch (LockTimeoutException ex) {
            throw new ConcurrenciaException(
                    "Timeout al intentar bloquear la solicitud ID " + solicitudId, ex);
        }
    }

    private void setRlsContext(Integer userId, RolUsuario rol) {
        entityManager.createNativeQuery(
                "SELECT rdam_set_session_user(:userId, CAST(:rol AS text)::rol_usuario)")
                .setParameter("userId", userId)
                .setParameter("rol", rol.name())
                .getSingleResult();
    }

    private void guardarConManejodeErrores(Certificado certificado, Solicitud solicitud) {
        try {
            certificadoRepository.save(certificado);
            solicitudRepository.save(solicitud);
            entityManager.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new EstadoInvalidoException(
                    "Error de integridad al emitir certificado: " + ex.getMostSpecificCause().getMessage(), ex);
        } catch (PersistenceException ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : "";
            if (message.contains("AUTH_VIOLATION") || message.contains("RLS")) {
                throw new AccesoDenegadoException(
                        "Acceso denegado por politica de seguridad: "
                        + (ex.getCause() != null ? ex.getCause().getMessage() : message), ex);
            }
            throw ex;
        }
    }

    private String generarHashSha256(String data) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    private String generarHashSha256Bytes(byte[] data) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
