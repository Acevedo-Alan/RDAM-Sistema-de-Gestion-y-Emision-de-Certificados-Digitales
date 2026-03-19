package com.rdam.service.exception;

/**
 * Excepción lanzada cuando se detecta un conflicto de concurrencia.
 * Típicamente ocurre cuando otro empleado ya está operando sobre la misma solicitud
 * y el timeout del bloqueo pesimista (PESSIMISTIC_WRITE) se excede.
 */
public class ConcurrenciaException extends RuntimeException {

    public ConcurrenciaException(String message) {
        super(message);
    }

    public ConcurrenciaException(String message, Throwable cause) {
        super(message, cause);
    }
}
