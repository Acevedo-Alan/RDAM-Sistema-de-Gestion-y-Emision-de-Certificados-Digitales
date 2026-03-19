package com.rdam.controller;

import com.rdam.domain.entity.Solicitud;
import com.rdam.dto.HistorialEstadoResponse;
import com.rdam.dto.ReasignacionRequest;
import com.rdam.dto.RechazoRequest;
import com.rdam.dto.SolicitudPagoResponse;
import com.rdam.dto.SolicitudRequest;
import com.rdam.dto.SolicitudResponse;
import com.rdam.security.service.CustomUserDetails;
import com.rdam.service.CertificadoService;
import com.rdam.service.HistorialService;
import com.rdam.service.PagoService;
import com.rdam.service.SolicitudService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/solicitudes")
@Tag(name = "Solicitudes", description = "Gestion de solicitudes de certificados — ciudadanos e internos/admin")
@SecurityRequirement(name = "bearerAuth")
public class SolicitudController {

    private final SolicitudService solicitudService;
    private final PagoService pagoService;
    private final CertificadoService certificadoService;
    private final HistorialService historialService;

    public SolicitudController(SolicitudService solicitudService,
                               PagoService pagoService,
                               CertificadoService certificadoService,
                               HistorialService historialService) {
        this.solicitudService = solicitudService;
        this.pagoService = pagoService;
        this.certificadoService = certificadoService;
        this.historialService = historialService;
    }

    // -- Endpoints Ciudadano --

    @Operation(summary = "Crear nueva solicitud de certificado")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Solicitud creada"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado"),
            @ApiResponse(responseCode = "422", description = "Tipo de certificado o circunscripcion inexistente")
    })
    @PreAuthorize("hasRole('CIUDADANO')")
    @PostMapping
    public ResponseEntity<SolicitudResponse> crear(@Valid @RequestBody SolicitudRequest request,
                                                    @AuthenticationPrincipal CustomUserDetails user) {
        Solicitud solicitud = solicitudService.crearSolicitud(
                user.getUserId().longValue(),
                request.tipoCertificadoId(),
                request.circunscripcionId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(solicitud));
    }

    @Operation(summary = "Listar solicitudes del ciudadano autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de solicitudes"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @PreAuthorize("hasRole('CIUDADANO')")
    @GetMapping("/mis-solicitudes")
    public ResponseEntity<List<SolicitudResponse>> misSolicitudes(
            @AuthenticationPrincipal CustomUserDetails user) {
        List<SolicitudResponse> solicitudes = solicitudService.misSolicitudes(user.getUserId());
        return ResponseEntity.ok(solicitudes);
    }

    @Operation(summary = "Obtener datos de pago para una solicitud aprobada")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos de pago"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
            @ApiResponse(responseCode = "422", description = "Estado no permite pago")
    })
    @PreAuthorize("hasRole('CIUDADANO')")
    @GetMapping("/{id}/pago-datos")
    public ResponseEntity<SolicitudPagoResponse> obtenerDatosPago(
            @PathVariable Integer id,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(pagoService.generarDatosPago(id, user.getUserId()));
    }

    @Operation(summary = "Descargar certificado emitido en formato PDF")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF del certificado"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
            @ApiResponse(responseCode = "422", description = "Certificado aun no emitido")
    })
    @PreAuthorize("hasRole('CIUDADANO')")
    @GetMapping("/{id}/certificado/pdf")
    public ResponseEntity<byte[]> descargarPdf(
            @PathVariable Integer id,
            @AuthenticationPrincipal CustomUserDetails user) {
        byte[] pdf = certificadoService.generarCertificadoPdf(id.longValue(), user.getUserId().longValue());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"certificado-" + id + ".pdf\"")
                .body(pdf);
    }

    @Operation(summary = "Cancelar una solicitud pendiente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud cancelada"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
            @ApiResponse(responseCode = "422", description = "Estado no permite cancelacion")
    })
    @PreAuthorize("hasRole('CIUDADANO')")
    @PostMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable Integer id,
                                          @AuthenticationPrincipal CustomUserDetails user) {
        solicitudService.cancelarSolicitud(id, user.getUserId());
        return ResponseEntity.ok().build();
    }

    // -- Endpoints Interno/Admin --

    @Operation(summary = "Obtener bandeja de solicitudes para revision")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de solicitudes en bandeja"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado")
    })
    @PreAuthorize("hasAnyRole('INTERNO', 'ADMIN')")
    @GetMapping("/bandeja")
    public ResponseEntity<List<SolicitudResponse>> bandeja(
            @AuthenticationPrincipal CustomUserDetails user) {
        List<SolicitudResponse> solicitudes = solicitudService.bandeja(
                user.getCircunscripcionId(), user.getRol(), user.getUserId());
        return ResponseEntity.ok(solicitudes);
    }

    @Operation(summary = "Tomar solicitud para revision")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud tomada"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
            @ApiResponse(responseCode = "422", description = "Transicion de estado invalida")
    })
    @PreAuthorize("hasRole('INTERNO')")
    @PatchMapping("/{id}/tomar")
    public ResponseEntity<SolicitudResponse> tomar(@PathVariable Integer id,
                                                    @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(solicitudService.tomarSolicitudParaRevision(
                id, user.getUserId(), user.getRol()));
    }

    @Operation(summary = "Aprobar solicitud en revision")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud aprobada"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
            @ApiResponse(responseCode = "422", description = "Transicion de estado invalida")
    })
    @PreAuthorize("hasAnyRole('INTERNO', 'ADMIN')")
    @PatchMapping("/{id}/aprobar")
    public ResponseEntity<Void> aprobar(@PathVariable Integer id,
                                         @AuthenticationPrincipal CustomUserDetails user) {
        solicitudService.aprobarSolicitud(
                id.longValue(),
                user.getUserId().longValue(),
                user.getCircunscripcionId().longValue()
        );
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Rechazar solicitud en revision")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud rechazada"),
            @ApiResponse(responseCode = "400", description = "Motivo de rechazo requerido"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
            @ApiResponse(responseCode = "422", description = "Transicion de estado invalida")
    })
    @PreAuthorize("hasAnyRole('INTERNO', 'ADMIN')")
    @PatchMapping("/{id}/rechazar")
    public ResponseEntity<Void> rechazar(@PathVariable Integer id,
                                          @Valid @RequestBody RechazoRequest request,
                                          @AuthenticationPrincipal CustomUserDetails user) {
        solicitudService.rechazarSolicitud(
                id.longValue(),
                user.getUserId().longValue(),
                user.getCircunscripcionId().longValue(),
                request.motivo()
        );
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Emitir certificado de solicitud pagada")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificado emitido"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
            @ApiResponse(responseCode = "422", description = "Solicitud no esta en estado PAGADA")
    })
    @PreAuthorize("hasAnyRole('INTERNO', 'ADMIN')")
    @PostMapping("/{id}/emitir")
    public ResponseEntity<Void> emitir(@PathVariable Integer id,
                                        @AuthenticationPrincipal CustomUserDetails user) {
        certificadoService.emitirCertificado(
                id,
                user.getUserId(),
                user.getCircunscripcionId().longValue()
        );
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Publicar certificado: genera PDF y transiciona PAGADO a PUBLICADO")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificado publicado"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
            @ApiResponse(responseCode = "422", description = "Solicitud no esta en estado PAGADO")
    })
    @PreAuthorize("hasAnyRole('INTERNO', 'ADMIN')")
    @PostMapping("/{id}/publicar")
    public ResponseEntity<Void> publicar(@PathVariable Integer id,
                                          @AuthenticationPrincipal CustomUserDetails user) {
        certificadoService.publicarCertificado(
                id,
                user.getUserId(),
                user.getCircunscripcionId().longValue()
        );
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Reasignar solicitud a otro empleado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud reasignada"),
            @ApiResponse(responseCode = "404", description = "Solicitud o empleado no encontrado"),
            @ApiResponse(responseCode = "422", description = "Estado invalido o circunscripcion no coincide")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/reasignar")
    public ResponseEntity<Void> reasignar(
            @PathVariable Integer id,
            @Valid @RequestBody ReasignacionRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        solicitudService.reasignarSolicitud(id, request.nuevoEmpleadoId(), user.getUserId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Obtener historial de solicitudes del usuario autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial de estados"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado")
    })
    @PreAuthorize("hasAnyRole('INTERNO', 'ADMIN')")
    @GetMapping("/historial")
    public ResponseEntity<List<HistorialEstadoResponse>> obtenerHistorialBandeja(
            @AuthenticationPrincipal CustomUserDetails user) {
        List<HistorialEstadoResponse> historial = historialService.obtenerHistorialBandeja(
                user.getUserId(), user.getRol(), user.getCircunscripcionId());
        return ResponseEntity.ok(historial);
    }

    // -- Endpoint compartido (Ciudadano + Interno + Admin) --

    @Operation(summary = "Obtener detalle de una solicitud por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle de la solicitud"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
            @ApiResponse(responseCode = "403", description = "Sin acceso a esta solicitud")
    })
    @PreAuthorize("hasAnyRole('CIUDADANO', 'INTERNO', 'ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<SolicitudResponse> obtenerPorId(
            @PathVariable Integer id,
            @AuthenticationPrincipal CustomUserDetails user) {
        SolicitudResponse response = solicitudService.obtenerPorId(
                id, user.getUserId(), user.getRol(), user.getCircunscripcionId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener historial de estados de una solicitud")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial de estados"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada")
    })
    @PreAuthorize("hasAnyRole('CIUDADANO', 'INTERNO', 'ADMIN')")
    @GetMapping("/{id}/historial")
    public ResponseEntity<List<HistorialEstadoResponse>> obtenerHistorial(
            @PathVariable Integer id,
            @AuthenticationPrincipal CustomUserDetails user) {
        List<HistorialEstadoResponse> historial = historialService.obtenerHistorial(
                id, user.getUserId(), user.getRol());
        return ResponseEntity.ok(historial);
    }

    // -- Mapper --

    private SolicitudResponse mapToResponse(Solicitud s) {
        String tipoCertificado = s.getTipoCertificado() != null
                ? s.getTipoCertificado().getNombre() : null;
        String circunscripcion = s.getCircunscripcion() != null
                ? s.getCircunscripcion().getNombre() : null;
        String ciudadanoNombre = s.getCiudadano() != null
                ? s.getCiudadano().getNombre() + " " + s.getCiudadano().getApellido() : null;
        String empleadoAsignado = s.getEmpleadoAsignado() != null
                ? s.getEmpleadoAsignado().getLegajo() : null;

        return new SolicitudResponse(
                s.getId(),
                s.getNumeroTramite(),
                s.getEstado().name(),
                tipoCertificado,
                circunscripcion,
                ciudadanoNombre,
                s.getMotivoRechazo(),
                empleadoAsignado,
                s.getMontoArancel(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}
