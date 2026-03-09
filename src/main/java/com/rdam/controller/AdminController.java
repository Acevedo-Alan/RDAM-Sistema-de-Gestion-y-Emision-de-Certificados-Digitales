package com.rdam.controller;

import com.rdam.domain.entity.EstadoSolicitud;
import com.rdam.dto.CrearUsuarioRequest;
import com.rdam.dto.EditarUsuarioRequest;
import com.rdam.dto.SolicitudAdminResponse;
import com.rdam.dto.UsuarioAdminResponse;
import com.rdam.service.AdminSolicitudService;
import com.rdam.service.AdminUsuarioService;
import com.rdam.service.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administracion", description = "Gestion de usuarios, solicitudes y reportes — solo ADMIN")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminUsuarioService adminUsuarioService;
    private final AdminSolicitudService adminSolicitudService;
    private final ReporteService reporteService;

    public AdminController(AdminUsuarioService adminUsuarioService,
                           AdminSolicitudService adminSolicitudService,
                           ReporteService reporteService) {
        this.adminUsuarioService = adminUsuarioService;
        this.adminSolicitudService = adminSolicitudService;
        this.reporteService = reporteService;
    }

    // ── Gestión de usuarios ──

    @Operation(summary = "Listar usuarios con paginacion")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de usuarios"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN")
    })
    @GetMapping("/usuarios")
    public ResponseEntity<Page<UsuarioAdminResponse>> listarUsuarios(Pageable pageable) {
        return ResponseEntity.ok(adminUsuarioService.listarUsuarios(pageable));
    }

    @Operation(summary = "Obtener usuario por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @GetMapping("/usuarios/{id}")
    public ResponseEntity<UsuarioAdminResponse> obtenerUsuario(@PathVariable Integer id) {
        return ResponseEntity.ok(adminUsuarioService.obtenerUsuario(id));
    }

    @Operation(summary = "Crear nuevo usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario creado"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "422", description = "CUIL o legajo duplicado")
    })
    @PostMapping("/usuarios")
    public ResponseEntity<UsuarioAdminResponse> crearUsuario(@Valid @RequestBody CrearUsuarioRequest request) {
        UsuarioAdminResponse response = adminUsuarioService.crearUsuario(request);
        return ResponseEntity.status(201).body(response);
    }

    @Operation(summary = "Editar usuario existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @PutMapping("/usuarios/{id}")
    public ResponseEntity<UsuarioAdminResponse> editarUsuario(@PathVariable Integer id,
                                                               @Valid @RequestBody EditarUsuarioRequest request) {
        return ResponseEntity.ok(adminUsuarioService.editarUsuario(id, request));
    }

    @Operation(summary = "Eliminar usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuario eliminado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Integer id) {
        adminUsuarioService.eliminarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Desactivar usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuario desactivado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @PostMapping("/usuarios/{id}/desactivar")
    public ResponseEntity<Void> desactivarUsuario(@PathVariable Integer id) {
        adminUsuarioService.desactivarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Activar usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuario activado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @PostMapping("/usuarios/{id}/activar")
    public ResponseEntity<Void> activarUsuario(@PathVariable Integer id) {
        adminUsuarioService.activarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Resetear bloqueo de login para un CUIL")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Bloqueo reseteado"),
            @ApiResponse(responseCode = "404", description = "CUIL no encontrado")
    })
    @PostMapping("/usuarios/{cuil}/reset-bloqueo")
    public ResponseEntity<Void> resetBloqueo(@PathVariable String cuil) {
        adminUsuarioService.resetBloqueo(cuil);
        return ResponseEntity.noContent().build();
    }

    // ── Gestión de solicitudes ──

    @Operation(summary = "Listar solicitudes con filtros y paginacion")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de solicitudes")
    })
    @GetMapping("/solicitudes")
    public ResponseEntity<Page<SolicitudAdminResponse>> listarSolicitudes(
            @RequestParam(required = false) EstadoSolicitud estado,
            @RequestParam(required = false) Integer circunscripcionId,
            Pageable pageable) {
        return ResponseEntity.ok(adminSolicitudService.listarSolicitudes(estado, circunscripcionId, pageable));
    }

    @Operation(summary = "Obtener detalle de solicitud por ID (vista admin)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud encontrada"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada")
    })
    @GetMapping("/solicitudes/{id}")
    public ResponseEntity<SolicitudAdminResponse> obtenerSolicitud(@PathVariable Integer id) {
        return ResponseEntity.ok(adminSolicitudService.obtenerSolicitud(id));
    }

    // ── Reportes CSV ──

    @Operation(summary = "Exportar reporte de solicitudes en CSV")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Archivo CSV generado")
    })
    @GetMapping("/reportes/solicitudes.csv")
    public ResponseEntity<byte[]> reporteSolicitudesCsv(
            @RequestParam(required = false) OffsetDateTime desde,
            @RequestParam(required = false) OffsetDateTime hasta,
            @RequestParam(required = false) EstadoSolicitud estado,
            @RequestParam(required = false) Integer circunscripcionId) {

        byte[] csv = reporteService.generarSolicitudesCsv(desde, hasta, estado, circunscripcionId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"solicitudes.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @Operation(summary = "Exportar reporte de usuarios en CSV")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Archivo CSV generado")
    })
    @GetMapping("/reportes/usuarios.csv")
    public ResponseEntity<byte[]> reporteUsuariosCsv() {
        byte[] csv = reporteService.generarUsuariosCsv();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"usuarios.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
