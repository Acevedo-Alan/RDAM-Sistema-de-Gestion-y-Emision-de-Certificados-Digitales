package com.rdam.controller;

import com.rdam.dto.AdjuntoResponse;
import com.rdam.security.service.CustomUserDetails;
import com.rdam.service.AdjuntoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/solicitudes/{id}/adjuntos")
@Tag(name = "Adjuntos", description = "Carga, descarga y eliminacion de archivos adjuntos de solicitudes")
@SecurityRequirement(name = "bearerAuth")
public class AdjuntoController {

    private final AdjuntoService adjuntoService;

    public AdjuntoController(AdjuntoService adjuntoService) {
        this.adjuntoService = adjuntoService;
    }

    @Operation(summary = "Subir un archivo adjunto a la solicitud")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Adjunto subido exitosamente"),
            @ApiResponse(responseCode = "400", description = "Archivo invalido o ausente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada")
    })
    @PostMapping
    @PreAuthorize("hasRole('CIUDADANO')")
    public ResponseEntity<AdjuntoResponse> subirAdjunto(
            @PathVariable("id") Integer solicitudId,
            @RequestParam("archivo") MultipartFile archivo,
            @AuthenticationPrincipal CustomUserDetails user) {

        AdjuntoResponse response = adjuntoService.subirAdjunto(solicitudId, user.getUserId(), archivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar adjuntos de una solicitud")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de adjuntos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('CIUDADANO', 'INTERNO', 'ADMIN')")
    public ResponseEntity<List<AdjuntoResponse>> listarAdjuntos(
            @PathVariable("id") Integer solicitudId,
            @AuthenticationPrincipal CustomUserDetails user) {

        List<AdjuntoResponse> adjuntos = adjuntoService.listarAdjuntos(solicitudId, user.getUserId(), user.getRol());
        return ResponseEntity.ok(adjuntos);
    }

    @Operation(summary = "Descargar un archivo adjunto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contenido del archivo"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "404", description = "Adjunto o solicitud no encontrada")
    })
    @GetMapping("/{adjId}")
    @PreAuthorize("hasAnyRole('CIUDADANO', 'INTERNO', 'ADMIN')")
    public ResponseEntity<byte[]> descargarAdjunto(
            @PathVariable("id") Integer solicitudId,
            @PathVariable("adjId") Integer adjuntoId,
            @AuthenticationPrincipal CustomUserDetails user) {

        byte[] contenido = adjuntoService.descargarAdjunto(solicitudId, adjuntoId, user.getUserId(), user.getRol());
        String nombreOriginal = adjuntoService.obtenerNombreOriginal(solicitudId, adjuntoId, user.getUserId(), user.getRol());
        String mimeType = adjuntoService.obtenerMimeType(solicitudId, adjuntoId, user.getUserId(), user.getRol());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreOriginal + "\"")
                .body(contenido);
    }

    @Operation(summary = "Eliminar un archivo adjunto")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Adjunto eliminado"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado"),
            @ApiResponse(responseCode = "404", description = "Adjunto o solicitud no encontrada")
    })
    @DeleteMapping("/{adjId}")
    @PreAuthorize("hasRole('CIUDADANO')")
    public ResponseEntity<Void> eliminarAdjunto(
            @PathVariable("id") Integer solicitudId,
            @PathVariable("adjId") Integer adjuntoId,
            @AuthenticationPrincipal CustomUserDetails user) {

        adjuntoService.eliminarAdjunto(solicitudId, adjuntoId, user.getUserId());
        return ResponseEntity.noContent().build();
    }
}
