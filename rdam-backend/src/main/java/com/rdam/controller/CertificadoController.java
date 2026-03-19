package com.rdam.controller;

import com.rdam.domain.entity.Certificado;
import com.rdam.dto.VerificacionResponse;
import com.rdam.security.service.CustomUserDetails;
import com.rdam.service.CertificadoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Certificados", description = "Descarga y verificacion de certificados digitales")
public class CertificadoController {

    private final CertificadoService certificadoService;

    public CertificadoController(CertificadoService certificadoService) {
        this.certificadoService = certificadoService;
    }

    @Operation(summary = "Descargar certificado en PDF")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF del certificado"),
            @ApiResponse(responseCode = "404", description = "Certificado no encontrado"),
            @ApiResponse(responseCode = "403", description = "Sin acceso al certificado")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('CIUDADANO', 'INTERNO', 'ADMIN')")
    @GetMapping("/api/certificados/{id}/download")
    public ResponseEntity<byte[]> descargar(
            @PathVariable Integer id,
            @AuthenticationPrincipal CustomUserDetails user) {

        byte[] pdf = certificadoService.descargarCertificado(id, user.getUserId(), user.getRol());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"certificado-" + id + ".pdf\"")
                .body(pdf);
    }

    @Operation(summary = "Verificar autenticidad de un certificado por token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultado de verificacion")
    })
    @GetMapping("/api/verificar/{token}")
    public ResponseEntity<VerificacionResponse> verificar(@PathVariable String token) {
        VerificacionResponse response = certificadoService.verificarCertificado(token);
        return ResponseEntity.ok(response);
    }
}
