package com.rdam.security.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adaptador entre las entidades JPA y el contrato de Spring Security.
 * Desacopla el modelo de dominio de la infraestructura de seguridad.
 */
public class CustomUserDetails implements UserDetails {

    private final Integer userId;
    private final String username;
    private final String passwordHash;
    private final String rol;
    private final boolean activo;
    private final Integer circunscripcionId; // solo para interno/admin

    public CustomUserDetails(Integer userId, String username, String passwordHash,
                             String rol, boolean activo, Integer circunscripcionId) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.activo = activo;
        this.circunscripcionId = circunscripcionId;
    }

    /**
     * Factory para ciudadanos (sin circunscripcion).
     */
    public static CustomUserDetails fromCiudadano(Integer userId, String cuil,
                                                   String passwordHash, String rol, boolean activo) {
        return new CustomUserDetails(userId, cuil, passwordHash, rol, activo, null);
    }

    /**
     * Factory para empleados (con circunscripcion).
     */
    public static CustomUserDetails fromEmpleado(Integer userId, String legajo,
                                                  String passwordHash, String rol,
                                                  boolean activo, Integer circunscripcionId) {
        return new CustomUserDetails(userId, legajo, passwordHash, rol, activo, circunscripcionId);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Mapeo: ciudadano -> ROLE_CIUDADANO, interno -> ROLE_INTERNO, admin -> ROLE_ADMIN
        String springRole = "ROLE_" + rol.toUpperCase();
        return List.of(new SimpleGrantedAuthority(springRole));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return activo;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getRol() {
        return rol;
    }

    public Integer getCircunscripcionId() {
        return circunscripcionId;
    }
}
