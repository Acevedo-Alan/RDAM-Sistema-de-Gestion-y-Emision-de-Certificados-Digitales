package com.rdam.service.impl;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.rdam.domain.entity.Certificado;
import com.rdam.domain.entity.Empleado;
import com.rdam.domain.entity.EstadoSolicitud;
import com.rdam.domain.entity.RolUsuario;
import com.rdam.domain.entity.Solicitud;
import com.rdam.domain.entity.Usuario;
import com.rdam.repository.CertificadoRepository;
import com.rdam.repository.EmpleadoRepository;
import com.rdam.repository.SolicitudRepository;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class CertificadoServiceImpl implements CertificadoService {

    private static final Logger log = LoggerFactory.getLogger(CertificadoServiceImpl.class);

    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final SolicitudRepository solicitudRepository;
    private final CertificadoRepository certificadoRepository;
    private final EmpleadoRepository empleadoRepository;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher eventPublisher;

    public CertificadoServiceImpl(SolicitudRepository solicitudRepository,
                                   CertificadoRepository certificadoRepository,
                                   EmpleadoRepository empleadoRepository,
                                   EntityManager entityManager,
                                   ApplicationEventPublisher eventPublisher) {
        this.solicitudRepository = solicitudRepository;
        this.certificadoRepository = certificadoRepository;
        this.empleadoRepository = empleadoRepository;
        this.entityManager = entityManager;
        this.eventPublisher = eventPublisher;
    }

    // -- Emision de certificado (Fase 4) --

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

        if (solicitud.getEstado() != EstadoSolicitud.PAGADA) {
            throw new EstadoInvalidoException(
                    "La solicitud no esta en estado PAGADA. Estado actual: " + solicitud.getEstado());
        }

        if (certificadoRepository.existsBySolicitudId(solicitudId)) {
            throw new EstadoInvalidoException("Certificado ya emitido para solicitud: " + solicitudId);
        }

        String rutaPdf = "certificados/" + UUID.randomUUID() + ".pdf";
        String hashPdf = generarHashSha256("MOCK-PDF-CONTENT-" + solicitudId + "-" + System.currentTimeMillis());

        Certificado certificado = new Certificado();
        certificado.setSolicitud(solicitud);
        certificado.setRutaPdf(rutaPdf);
        certificado.setHashPdf(hashPdf);
        certificado.setPlantillaVer(1);
        certificado.setEmitidoPor(empleado);
        certificado.setFechaEmision(OffsetDateTime.now());

        // RLS ANTES de modificar la entidad managed (Hibernate auto-flush)
        setRlsContext(empleado.getUsuario().getId(), RolUsuario.interno);

        solicitud.setEstado(EstadoSolicitud.EMITIDA);
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

    // -- Generacion de PDF (Fase 5) --

    @Override
    @Transactional(readOnly = true)
    public byte[] generarCertificadoPdf(Long solicitudId, Long ciudadanoId) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId.intValue())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro la solicitud con ID: " + solicitudId));

        // Validar propiedad
        if (!solicitud.getCiudadano().getId().equals(ciudadanoId.intValue())) {
            log.warn("action=ACCESO_DENEGADO_PDF solicitudId={} ciudadanoId={} propietarioId={}",
                    solicitudId, ciudadanoId, solicitud.getCiudadano().getId());
            throw new AccesoDenegadoException(
                    "No tiene permiso para descargar el certificado de la solicitud " + solicitudId);
        }

        // Validar estado FSM
        EstadoSolicitud estado = solicitud.getEstado();
        if (estado != EstadoSolicitud.PAGADA && estado != EstadoSolicitud.EMITIDA) {
            throw new EstadoInvalidoException(
                    "El certificado solo puede descargarse en estado PAGADA o EMITIDA. Estado actual: " + estado);
        }

        String codigoVerificacion = UUID.randomUUID().toString();
        Usuario ciudadano = solicitud.getCiudadano();
        String nombreCompleto = ciudadano.getNombre() + " " + ciudadano.getApellido();

        byte[] pdfBytes = construirPdf(solicitud, nombreCompleto, codigoVerificacion);

        log.info("action=CERTIFICADO_GENERADO solicitudId={} ciudadanoId={} bytes={}",
                solicitudId, ciudadanoId, pdfBytes.length);

        return pdfBytes;
    }

    // -- Construccion del documento PDF con OpenPDF --

    private byte[] construirPdf(Solicitud solicitud, String nombreCiudadano, String codigoVerificacion) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            Document document = new Document(PageSize.A4, 50, 50, 60, 50);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.DARK_GRAY);
            Font fontSubtitulo = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.GRAY);
            Font fontCuerpo = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);
            Font fontNegrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
            Font fontPequena = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);

            // Titulo
            Paragraph titulo = new Paragraph("Certificado Digital Oficial - RDAM", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(5f);
            document.add(titulo);

            Paragraph subtitulo = new Paragraph("Sistema de Certificados Digitales", fontSubtitulo);
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            subtitulo.setSpacingAfter(30f);
            document.add(subtitulo);

            // Linea separadora
            document.add(crearLineaSeparadora());

            // Tabla de datos
            PdfPTable tabla = new PdfPTable(2);
            tabla.setWidthPercentage(90);
            tabla.setWidths(new float[]{35f, 65f});
            tabla.setSpacingAfter(25f);

            agregarFilaTabla(tabla, "Numero de Solicitud:", String.valueOf(solicitud.getId()), fontNegrita, fontCuerpo);
            agregarFilaTabla(tabla, "Tipo de Certificado:", solicitud.getTipoCertificado().getNombre(), fontNegrita, fontCuerpo);
            agregarFilaTabla(tabla, "Circunscripcion:", solicitud.getCircunscripcion().getNombre(), fontNegrita, fontCuerpo);
            agregarFilaTabla(tabla, "Ciudadano:", nombreCiudadano, fontNegrita, fontCuerpo);
            agregarFilaTabla(tabla, "CUIL:", solicitud.getCiudadano().getCuil(), fontNegrita, fontCuerpo);
            agregarFilaTabla(tabla, "Fecha de Emision:", OffsetDateTime.now().format(FECHA_FMT), fontNegrita, fontCuerpo);
            agregarFilaTabla(tabla, "Estado:", solicitud.getEstado().name(), fontNegrita, fontCuerpo);

            document.add(tabla);

            // Cuerpo
            Paragraph cuerpo = new Paragraph(
                    "Por la presente se certifica que la solicitud ha sido procesada y aprobada correctamente. "
                    + "Este documento constituye constancia oficial emitida por el sistema RDAM — "
                    + "Sistema de Certificados Digitales de la i2T Software Factory.",
                    fontCuerpo);
            cuerpo.setAlignment(Element.ALIGN_JUSTIFIED);
            cuerpo.setSpacingAfter(40f);
            document.add(cuerpo);

            // Segunda linea separadora
            document.add(crearLineaSeparadora());

            // Codigo de verificacion
            Paragraph verificacion = new Paragraph("Codigo de Verificacion: " + codigoVerificacion, fontPequena);
            verificacion.setAlignment(Element.ALIGN_CENTER);
            verificacion.setSpacingAfter(5f);
            document.add(verificacion);

            Paragraph nota = new Paragraph(
                    "Este certificado fue generado electronicamente y no requiere firma manuscrita.",
                    fontPequena);
            nota.setAlignment(Element.ALIGN_CENTER);
            document.add(nota);

            document.close();

        } catch (DocumentException e) {
            throw new PdfGenerationException(
                    "Error al generar el PDF para solicitud " + solicitud.getId(), e);
        }

        return baos.toByteArray();
    }

    private PdfPTable crearLineaSeparadora() {
        PdfPTable lineaSep = new PdfPTable(1);
        lineaSep.setWidthPercentage(100);
        PdfPCell lineaCell = new PdfPCell();
        lineaCell.setBorderWidthBottom(2f);
        lineaCell.setBorderWidthTop(0);
        lineaCell.setBorderWidthLeft(0);
        lineaCell.setBorderWidthRight(0);
        lineaCell.setBorderColorBottom(new Color(0, 102, 153));
        lineaCell.setFixedHeight(3f);
        lineaSep.addCell(lineaCell);
        lineaSep.setSpacingAfter(25f);
        return lineaSep;
    }

    private void agregarFilaTabla(PdfPTable tabla, String etiqueta, String valor, Font fontEtiqueta, Font fontValor) {
        PdfPCell celdaEtiqueta = new PdfPCell(new Phrase(etiqueta, fontEtiqueta));
        celdaEtiqueta.setBorder(0);
        celdaEtiqueta.setPaddingBottom(8f);

        PdfPCell celdaValor = new PdfPCell(new Phrase(valor != null ? valor : "-", fontValor));
        celdaValor.setBorder(0);
        celdaValor.setPaddingBottom(8f);

        tabla.addCell(celdaEtiqueta);
        tabla.addCell(celdaValor);
    }

    // -- Metodos privados reutilizados --

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
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
