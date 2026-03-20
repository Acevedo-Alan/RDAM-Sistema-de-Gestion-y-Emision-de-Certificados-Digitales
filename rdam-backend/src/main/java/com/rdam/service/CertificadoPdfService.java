package com.rdam.service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
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
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class CertificadoPdfService {

    private static final Logger log = LoggerFactory.getLogger(CertificadoPdfService.class);
    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DeviceRgb AZUL_JUDICIAL = new DeviceRgb(0, 94, 162);
    private static final DeviceRgb BLANCO = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb TEXTO_OSCURO = new DeviceRgb(27, 27, 27);
    private static final DeviceRgb GRIS = new DeviceRgb(113, 118, 122);

    public byte[] generar(Solicitud solicitud, String token) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(40, 50, 40, 50);

            agregarEncabezado(document);
            document.add(new Paragraph("").setMarginBottom(12));
            agregarTitulo(document, solicitud.getTipoCertificado().getNombre());
            agregarLineaSeparadora(document);
            agregarTablaDatos(document, solicitud, token);
            agregarSeccionQrYLegal(document, token);
            agregarPieDePagina(document);

            agregarWatermark(pdfDoc);

            document.close();
        } catch (Exception e) {
            throw new PdfGenerationException(
                    "Error al generar el PDF para solicitud " + solicitud.getId(), e);
        }

        log.info("action=PDF_GENERADO solicitudId={} bytes={}", solicitud.getId(), baos.size());
        return baos.toByteArray();
    }

    private void agregarEncabezado(Document document) {
        Table header = new Table(UnitValue.createPercentArray(new float[]{25, 75}))
                .useAllAvailableWidth()
                .setMarginTop(-50)
                .setMarginLeft(-50)
                .setMarginRight(-50);

        // Logo cell
        Cell logoCell = new Cell()
                .setBackgroundColor(AZUL_JUDICIAL)
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPaddingTop(10)
                .setPaddingBottom(10)
                .setPaddingLeft(15)
                .setPaddingRight(10);
        try {
            InputStream is = getClass().getResourceAsStream("/static/logo-poder-judicial.png");
            if (is != null) {
                byte[] logoBytes = is.readAllBytes();
                ImageData imageData = ImageDataFactory.create(logoBytes);
                Image logo = new Image(imageData).setHeight(75).setAutoScaleWidth(true);
                logoCell.add(logo);
            }
        } catch (Exception e) {
            log.warn("No se pudo cargar el logo: {}", e.getMessage());
            logoCell.add(new Paragraph(""));
        }
        header.addCell(logoCell);

        // Text cell
        Cell textCell = new Cell()
                .setBackgroundColor(AZUL_JUDICIAL)
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPaddingTop(10)
                .setPaddingBottom(10)
                .setPaddingLeft(15);
        textCell.add(new Paragraph("PODER JUDICIAL")
                .setBold()
                .setFontSize(14)
                .setFontColor(BLANCO)
                .setMarginBottom(4));
        textCell.add(new Paragraph("PROVINCIA DE SANTA FE")
                .setFontSize(10)
                .setFontColor(BLANCO));
        header.addCell(textCell);

        document.add(header);
    }

    private void agregarTitulo(Document document, String tipoCertificado) {
        Paragraph titulo = new Paragraph("CERTIFICADO DIGITAL OFICIAL")
                .setFontSize(22)
                .setBold()
                .setFontColor(AZUL_JUDICIAL)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(0)
                .setMarginBottom(5);
        document.add(titulo);

        Paragraph subtitulo = new Paragraph(tipoCertificado)
                .setFontSize(13)
                .setFontColor(GRIS)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(15);
        document.add(subtitulo);
    }

    private void agregarLineaSeparadora(Document document) {
        Table linea = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        Cell cell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(AZUL_JUDICIAL, 1.5f))
                .setHeight(1);
        linea.addCell(cell);
        linea.setMarginBottom(15);
        document.add(linea);
    }

    private void agregarTablaDatos(Document document, Solicitud solicitud, String token) {
        Paragraph intro = new Paragraph("Por medio del presente se certifica que:")
                .setFontSize(10)
                .setFontColor(GRIS)
                .setMarginBottom(10);
        document.add(intro);

        Table tabla = new Table(UnitValue.createPercentArray(new float[]{40, 60}))
                .useAllAvailableWidth()
                .setMarginBottom(12);

        String cuil = solicitud.getCiudadano().getCuil();
        String cuilFormateado = formatearCuil(cuil);
        String titular = solicitud.getCiudadano().getApellido() + ", " + solicitud.getCiudadano().getNombre();
        String fechaEmision = OffsetDateTime.now().format(FECHA_FMT);

        agregarFilaTabla(tabla, "N\u00B0 de Tr\u00E1mite:", solicitud.getNumeroTramite());
        agregarFilaTabla(tabla, "Titular:", titular);
        agregarFilaTabla(tabla, "CUIL:", cuilFormateado);
        agregarFilaTabla(tabla, "Circunscripci\u00F3n:", solicitud.getCircunscripcion().getNombre());
        agregarFilaTabla(tabla, "Fecha de Emisi\u00F3n:", fechaEmision);
        agregarFilaTabla(tabla, "N\u00B0 Verificaci\u00F3n:", token.substring(0, 8));

        document.add(tabla);
    }

    private void agregarFilaTabla(Table tabla, String etiqueta, String valor) {
        Cell celdaEtiqueta = new Cell()
                .add(new Paragraph(etiqueta).setBold().setFontSize(11).setFontColor(TEXTO_OSCURO))
                .setBorder(Border.NO_BORDER)
                .setPaddingBottom(10);

        Cell celdaValor = new Cell()
                .add(new Paragraph(valor != null ? valor : "-").setFontSize(11).setFontColor(TEXTO_OSCURO))
                .setBorder(Border.NO_BORDER)
                .setPaddingBottom(10);

        tabla.addCell(celdaEtiqueta);
        tabla.addCell(celdaValor);
    }

    private void agregarSeccionQrYLegal(Document document, String token) {
        agregarLineaSeparadora(document);

        String url = "http://192.168.0.25/verificar/" + token;

        Table seccion = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                .useAllAvailableWidth()
                .setMarginBottom(12);

        // Legal text cell (left 70%)
        Cell legalCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPaddingRight(10);
        Paragraph parrafoLegal = new Paragraph()
                .add(new Text("El presente certificado ha sido emitido en forma digital y tiene plena validez legal " +
                        "conforme a la normativa vigente de la Provincia de Santa Fe. " +
                        "Para verificar su autenticidad escanee el c\u00F3digo QR o ingrese a:\n")
                        .setFontSize(9).setFontColor(GRIS))
                .add(new Text(url)
                        .setFontSize(8).setBold().setFontColor(AZUL_JUDICIAL))
                .setTextAlignment(TextAlignment.JUSTIFIED);
        legalCell.add(parrafoLegal);
        seccion.addCell(legalCell);

        // QR cell (right 30%)
        Cell qrCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, 200, 200);
            ByteArrayOutputStream qrBaos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", qrBaos);
            Image qrImage = new Image(ImageDataFactory.create(qrBaos.toByteArray()))
                    .setWidth(100)
                    .setHeight(100)
                    .setHorizontalAlignment(HorizontalAlignment.RIGHT);
            qrCell.add(qrImage);
        } catch (WriterException | IOException e) {
            log.warn("No se pudo generar el QR para token={}: {}", token, e.getMessage());
            qrCell.add(new Paragraph(""));
        }
        seccion.addCell(qrCell);

        document.add(seccion);
    }

    private void agregarPieDePagina(Document document) {
        agregarLineaSeparadora(document);

        int anio = OffsetDateTime.now().getYear();

        document.add(new Paragraph("DOCUMENTO EMITIDO DIGITALMENTE \u2014 V\u00C1LIDO SIN FIRMA OL\u00D3GRAFA")
                .setFontSize(8)
                .setFontColor(GRIS)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(3));

        document.add(new Paragraph(
                "Poder Judicial de la Provincia de Santa Fe | Sistema RDAM | " + anio)
                .setFontSize(8)
                .setFontColor(GRIS)
                .setTextAlignment(TextAlignment.CENTER));
    }

    private void agregarWatermark(PdfDocument pdfDoc) {
        PdfPage page = pdfDoc.getPage(1);
        PdfCanvas canvas = new PdfCanvas(page.newContentStreamBefore(),
                                          page.getResources(), pdfDoc);
        try {
            PdfExtGState gs = new PdfExtGState();
            gs.setFillOpacity(0.07f);
            canvas.saveState()
                  .setExtGState(gs)
                  .setFillColor(new DeviceRgb(0, 94, 162))
                  .beginText()
                  .setFontAndSize(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD), 52)
                  .setTextMatrix(
                      (float) Math.cos(Math.toRadians(40)),
                      (float) Math.sin(Math.toRadians(40)),
                      -(float) Math.sin(Math.toRadians(40)),
                      (float) Math.cos(Math.toRadians(40)),
                      80, 250)
                  .showText("DOCUMENTO OFICIAL")
                  .endText()
                  .restoreState();
        } catch (Exception e) {
            log.warn("No se pudo agregar watermark: {}", e.getMessage());
        } finally {
            canvas.release();
        }
    }

    private String formatearCuil(String cuil) {
        if (cuil == null || cuil.length() != 11) {
            return cuil != null ? cuil : "-";
        }
        return cuil.substring(0, 2) + "-" + cuil.substring(2, 10) + "-" + cuil.substring(10);
    }
}
