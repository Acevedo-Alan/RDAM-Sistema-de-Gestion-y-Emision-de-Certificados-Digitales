package com.rdam.security.provider;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * Token de autenticación específico para empleados (interno/admin).
 * Permite al AuthenticationManager enrutar al EmpleadoAuthProvider
 * mediante el método supports().
 */
public class EmpleadoAuthToken extends UsernamePasswordAuthenticationToken {

    public EmpleadoAuthToken(Object principal, Object credentials) {
        super(principal, credentials);
    }
}
