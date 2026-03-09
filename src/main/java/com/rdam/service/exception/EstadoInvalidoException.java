package com.rdam.service.exception;

/**
 * Excepción lanzada cuando se intenta una transición de estado no permitida
 * por la máquina de estados del DDL (trigger trg_solicitudes_state_machine).
 * También usada para validaciones previas simples en el servicio.
 */
public class EstadoInvalidoException extends RuntimeException {

    public EstadoInvalidoException(String message) {
        super(message);
    }

    public EstadoInvalidoException(String message, Throwable cause) {
        super(message, cause);
    }
}
