package com.rdam.service;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.rdam.domain.entity.Solicitud;
import com.rdam.service.exception.PdfGenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class CertificadoPdfService {

    private static final Logger log = LoggerFactory.getLogger(CertificadoPdfService.class);
    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DeviceRgb AZUL_JUDICIAL = new DeviceRgb(0, 94, 162);
    private static final DeviceRgb GRIS_OSCURO = new DeviceRgb(27, 27, 27);

    public byte[] generar(Solicitud solicitud, String token) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(50, 50, 50, 50);

            agregarEncabezado(document);
            agregarTitulo(document, solicitud.getTipoCertificado().getNombre());
            agregarLineaSeparadora(document);
            agregarTablaDatos(document, solicitud, token);
            agregarPieDeVerificacion(document, token);
            agregarQrCode(document, token);
            agregarSelloDigital(document);

            document.close();
        } catch (Exception e) {
            throw new PdfGenerationException(
                    "Error al generar el PDF para solicitud " + solicitud.getId(), e);
        }

        log.info("action=PDF_GENERADO solicitudId={} bytes={}", solicitud.getId(), baos.size());
        return baos.toByteArray();
    }

    private void agregarEncabezado(Document document) {
        Paragraph encabezado = new Paragraph("PODER JUDICIAL \u2014 PROVINCIA DE SANTA FE")
                .setFontSize(14)
                .setBold()
                .setFontColor(AZUL_JUDICIAL)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);
        document.add(encabezado);

        Table lineaEncabezado = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        Cell cell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(AZUL_JUDICIAL, 2))
                .setHeight(1);
        lineaEncabezado.addCell(cell);
        document.add(lineaEncabezado);
    }

    private void agregarTitulo(Document document, String tipoCertificado) {
        Paragraph titulo = new Paragraph("CERTIFICADO DIGITAL")
                .setFontSize(22)
                .setBold()
                .setFontColor(GRIS_OSCURO)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(25)
                .setMarginBottom(5);
        document.add(titulo);

        Paragraph subtitulo = new Paragraph(tipoCertificado)
                .setFontSize(13)
                .setFontColor(new DeviceRgb(113, 118, 122))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(25);
        document.add(subtitulo);
    }

    private void agregarLineaSeparadora(Document document) {
        Table linea = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        Cell cell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(new DeviceRgb(0, 102, 153), 1.5f))
                .setHeight(1);
        linea.addCell(cell);
        linea.setMarginBottom(20);
        document.add(linea);
    }

    private void agregarTablaDatos(Document document, Solicitud solicitud, String token) {
        Table tabla = new Table(UnitValue.createPercentArray(new float[]{35, 65}));
        tabla.setWidth(UnitValue.createPercentValue(90));
        tabla.setHorizontalAlignment(HorizontalAlignment.CENTER);
        tabla.setMarginBottom(25);

        String cuil = solicitud.getCiudadano().getCuil();
        String cuilFormateado = formatearCuil(cuil);
        String titular = solicitud.getCiudadano().getApellido() + ", " + solicitud.getCiudadano().getNombre();
        String fechaEmision = OffsetDateTime.now().format(FECHA_FMT);
        String codigoVerificacion = token.substring(0, 8);

        agregarFilaTabla(tabla, "N\u00B0 de Tramite:", solicitud.getNumeroTramite());
        agregarFilaTabla(tabla, "Titular:", titular);
        agregarFilaTabla(tabla, "CUIL:", cuilFormateado);
        agregarFilaTabla(tabla, "Circunscripcion:", solicitud.getCircunscripcion().getNombre());
        agregarFilaTabla(tabla, "Fecha de emision:", fechaEmision);
        agregarFilaTabla(tabla, "N\u00B0 de verificacion:", codigoVerificacion);

        document.add(tabla);
    }

    private void agregarFilaTabla(Table tabla, String etiqueta, String valor) {
        Cell celdaEtiqueta = new Cell()
                .add(new Paragraph(etiqueta).setBold().setFontSize(11).setFontColor(GRIS_OSCURO))
                .setBorder(Border.NO_BORDER)
                .setPaddingBottom(8);

        Cell celdaValor = new Cell()
                .add(new Paragraph(valor != null ? valor : "-").setFontSize(11).setFontColor(GRIS_OSCURO))
                .setBorder(Border.NO_BORDER)
                .setPaddingBottom(8);

        tabla.addCell(celdaEtiqueta);
        tabla.addCell(celdaValor);
    }

    private void agregarPieDeVerificacion(Document document, String token) {
        agregarLineaSeparadora(document);

        Paragraph pie = new Paragraph(
                "Este documento tiene validez oficial. Verifica su autenticidad en: rdam.gob.ar/verificar/" + token)
                .setFontSize(9)
                .setFontColor(new DeviceRgb(113, 118, 122))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(pie);
    }

    private void agregarQrCode(Document document, String token) {
        try {
            String url = "http://localhost:5173/verificar/" + token;
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, 150, 150);

            ByteArrayOutputStream qrBaos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", qrBaos);
            byte[] qrBytes = qrBaos.toByteArray();

            Image qrImage = new Image(ImageDataFactory.create(qrBytes));
            qrImage.setWidth(100);
            qrImage.setHeight(100);
            qrImage.setHorizontalAlignment(HorizontalAlignment.RIGHT);
            qrImage.setMarginBottom(15);
            document.add(qrImage);
        } catch (WriterException | IOException e) {
            log.warn("No se pudo generar el QR para token={}: {}", token, e.getMessage());
        }
    }

    private void agregarSelloDigital(Document document) {
        Table sello = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        Cell cell = new Cell()
                .add(new Paragraph("DOCUMENTO EMITIDO DIGITALMENTE \u2014 VALIDO SIN FIRMA OLOGRAFA")
                        .setFontSize(10)
                        .setBold()
                        .setFontColor(AZUL_JUDICIAL)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorder(new SolidBorder(AZUL_JUDICIAL, 1.5f))
                .setPadding(10);
        sello.addCell(cell);
        sello.setHorizontalAlignment(HorizontalAlignment.CENTER);
        document.add(sello);
    }

    private String formatearCuil(String cuil) {
        if (cuil == null || cuil.length() != 11) {
            return cuil != null ? cuil : "-";
        }
        return cuil.substring(0, 2) + "-" + cuil.substring(2, 10) + "-" + cuil.substring(10);
    }
}
