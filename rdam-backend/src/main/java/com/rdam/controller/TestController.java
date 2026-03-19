package com.rdam.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/seguro")
    public ResponseEntity<String> endpointSeguro(Authentication authentication) {
        // Extraemos quién es el usuario directamente del contexto de seguridad de Spring
        String username = authentication.getName();
        String roles = authentication.getAuthorities().toString();
        
        return ResponseEntity.ok("¡Hola " + username + "! Estás dentro del área segura. Tus roles son: " + roles);
    }
}