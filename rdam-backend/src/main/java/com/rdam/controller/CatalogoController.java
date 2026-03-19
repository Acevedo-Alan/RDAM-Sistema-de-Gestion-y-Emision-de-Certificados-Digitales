package com.rdam.controller;

import com.rdam.dto.CircunscripcionRequest;
import com.rdam.dto.CircunscripcionResponse;
import com.rdam.dto.TipoCertificadoRequest;
import com.rdam.dto.TipoCertificadoResponse;
import com.rdam.service.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalogos")
@Tag(name = "Catalogos", description = "Tipos de certificado y circunscripciones — lectura publica, escritura admin")
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    // ── Tipos de certificado ──

    @Operation(summary = "Listar tipos de certificado activos")
    @ApiResponse(responseCode = "200", description = "Lista de tipos de certificado activos")
    @GetMapping("/tipos-certificado")
    public ResponseEntity<List<TipoCertificadoResponse>> listarTiposCertificadoActivos() {
        return ResponseEntity.ok(catalogoService.listarTiposCertificadoActivos());
    }

    @Operation(summary = "Listar todos los tipos de certificado (incluye inactivos)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista completa"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/tipos-certificado/todos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TipoCertificadoResponse>> listarTodosTiposCertificado() {
        return ResponseEntity.ok(catalogoService.listarTodosTiposCertificado());
    }

    @Operation(summary = "Obtener un tipo de certificado por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tipo de certificado encontrado"),
            @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    @GetMapping("/tipos-certificado/{id}")
    public ResponseEntity<TipoCertificadoResponse> obtenerTipoCertificado(@PathVariable Integer id) {
        return ResponseEntity.ok(catalogoService.obtenerTipoCertificado(id));
    }

    @Operation(summary = "Crear nuevo tipo de certificado")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tipo creado"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/tipos-certificado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TipoCertificadoResponse> crearTipoCertificado(
            @Valid @RequestBody TipoCertificadoRequest request) {
        TipoCertificadoResponse response = catalogoService.crearTipoCertificado(request);
        return ResponseEntity.status(201).body(response);
    }

    @Operation(summary = "Editar tipo de certificado existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tipo actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/tipos-certificado/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TipoCertificadoResponse> editarTipoCertificado(
            @PathVariable Integer id,
            @Valid @RequestBody TipoCertificadoRequest request) {
        return ResponseEntity.ok(catalogoService.editarTipoCertificado(id, request));
    }

    @Operation(summary = "Desactivar tipo de certificado")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Desactivado"),
            @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/tipos-certificado/{id}/desactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desactivarTipoCertificado(@PathVariable Integer id) {
        catalogoService.desactivarTipoCertificado(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Activar tipo de certificado")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Activado"),
            @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/tipos-certificado/{id}/activar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activarTipoCertificado(@PathVariable Integer id) {
        catalogoService.activarTipoCertificado(id);
        return ResponseEntity.noContent().build();
    }

    // ── Circunscripciones ──

    @Operation(summary = "Listar circunscripciones activas")
    @ApiResponse(responseCode = "200", description = "Lista de circunscripciones activas")
    @GetMapping("/circunscripciones")
    public ResponseEntity<List<CircunscripcionResponse>> listarCircunscripcionesActivas() {
        return ResponseEntity.ok(catalogoService.listarCircunscripcionesActivas());
    }

    @Operation(summary = "Listar todas las circunscripciones (incluye inactivas)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista completa"),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/circunscripciones/todas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CircunscripcionResponse>> listarTodasCircunscripciones() {
        return ResponseEntity.ok(catalogoService.listarTodasCircunscripciones());
    }

    @Operation(summary = "Obtener circunscripcion por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Circunscripcion encontrada"),
            @ApiResponse(responseCode = "404", description = "No encontrada")
    })
    @GetMapping("/circunscripciones/{id}")
    public ResponseEntity<CircunscripcionResponse> obtenerCircunscripcion(@PathVariable Integer id) {
        return ResponseEntity.ok(catalogoService.obtenerCircunscripcion(id));
    }

    @Operation(summary = "Crear nueva circunscripcion")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Circunscripcion creada"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/circunscripciones")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CircunscripcionResponse> crearCircunscripcion(
            @Valid @RequestBody CircunscripcionRequest request) {
        CircunscripcionResponse response = catalogoService.crearCircunscripcion(request);
        return ResponseEntity.status(201).body(response);
    }

    @Operation(summary = "Editar circunscripcion existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Circunscripcion actualizada"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "404", description = "No encontrada")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/circunscripciones/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CircunscripcionResponse> editarCircunscripcion(
            @PathVariable Integer id,
            @Valid @RequestBody CircunscripcionRequest request) {
        return ResponseEntity.ok(catalogoService.editarCircunscripcion(id, request));
    }

    @Operation(summary = "Desactivar circunscripcion")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Desactivada"),
            @ApiResponse(responseCode = "404", description = "No encontrada")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/circunscripciones/{id}/desactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desactivarCircunscripcion(@PathVariable Integer id) {
        catalogoService.desactivarCircunscripcion(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Activar circunscripcion")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Activada"),
            @ApiResponse(responseCode = "404", description = "No encontrada")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/circunscripciones/{id}/activar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activarCircunscripcion(@PathVariable Integer id) {
        catalogoService.activarCircunscripcion(id);
        return ResponseEntity.noContent().build();
    }
}
