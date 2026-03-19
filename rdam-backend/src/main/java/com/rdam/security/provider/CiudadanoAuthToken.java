package com.rdam.security.provider;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * Token de autenticación específico para ciudadanos.
 * Permite al AuthenticationManager enrutar al CiudadanoAuthProvider
 * mediante el método supports().
 */
public class CiudadanoAuthToken extends UsernamePasswordAuthenticationToken {

    public CiudadanoAuthToken(Object principal, Object credentials) {
        super(principal, credentials);
    }
}
