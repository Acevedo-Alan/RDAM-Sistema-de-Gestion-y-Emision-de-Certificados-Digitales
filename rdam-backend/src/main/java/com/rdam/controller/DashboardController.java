package com.rdam.controller;

import com.rdam.dto.DashboardAdminResponse;
import com.rdam.dto.DashboardInternoResponse;
import com.rdam.security.service.CustomUserDetails;
import com.rdam.service.DashboardCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Indicadores y metricas para paneles de administracion e internos")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardCacheService dashboardCacheService;

    public DashboardController(DashboardCacheService dashboardCacheService) {
        this.dashboardCacheService = dashboardCacheService;
    }

    @Operation(summary = "Obtener dashboard de administrador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Metricas del dashboard admin"),
            @ApiResponse(responseCode = "204", description = "Datos aun no disponibles"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN")
    })
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardAdminResponse> getAdminDashboard(
            @AuthenticationPrincipal CustomUserDetails principal) {
        log.info("action=DASHBOARD_ADMIN_REQUEST userId={}", principal.getUserId());
        DashboardAdminResponse snapshot = dashboardCacheService.getAdminSnapshot();
        if (snapshot == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(snapshot);
    }

    @Operation(summary = "Obtener dashboard de empleado interno")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Metricas del dashboard interno"),
            @ApiResponse(responseCode = "204", description = "Datos aun no disponibles"),
            @ApiResponse(responseCode = "400", description = "Empleado sin circunscripcion asignada"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Solo INTERNO o ADMIN")
    })
    @GetMapping("/interno")
    @PreAuthorize("hasAnyRole('INTERNO', 'ADMIN')")
    public ResponseEntity<DashboardInternoResponse> getInternoDashboard(
            @AuthenticationPrincipal CustomUserDetails principal) {
        Integer circunscripcionId = principal.getCircunscripcionId();
        log.info("action=DASHBOARD_INTERNO_REQUEST userId={} circunscripcionId={}",
                principal.getUserId(), circunscripcionId);
        if (circunscripcionId == null) {
            return ResponseEntity.badRequest().build();
        }
        DashboardInternoResponse snapshot = dashboardCacheService.getInternoSnapshot(circunscripcionId);
        if (snapshot == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(snapshot);
    }
}
