package com.rdam.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/permisos")
public class PermisosController {

    // 1. Zona Exclusiva para Ciudadanos
    @PreAuthorize("hasAuthority('ROLE_CIUDADANO')")
    @GetMapping("/ciudadano")
    public ResponseEntity<String> soloCiudadanos(Authentication auth) {
        return ResponseEntity.ok("✅ ¡Bienvenido Ciudadano " + auth.getName() + "! Estás en tu área exclusiva.");
    }

    // 2. Zona Exclusiva para Empleados (Internos)
    @PreAuthorize("hasAuthority('ROLE_INTERNO')")
    @GetMapping("/interno")
    public ResponseEntity<String> soloInternos(Authentication auth) {
        return ResponseEntity.ok("🏢 ¡Bienvenido Empleado " + auth.getName() + "! Acceso concedido a la intranet.");
    }

    // 3. Zona Compartida
    @PreAuthorize("hasAnyAuthority('ROLE_CIUDADANO', 'ROLE_INTERNO')")
    @GetMapping("/compartido")
    public ResponseEntity<String> zonaCompartida(Authentication auth) {
        return ResponseEntity.ok("🤝 ¡Hola " + auth.getName() + "! Esta es un área pública para todos los usuarios autenticados.");
    }
}