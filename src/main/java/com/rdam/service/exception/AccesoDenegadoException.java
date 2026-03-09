package com.rdam.service.exception;

/**
 * Excepción lanzada cuando un usuario intenta operar sobre un recurso
 * fuera de su circunscripción o sin los permisos necesarios.
 * También mapeada desde violaciones de RLS en PostgreSQL.
 */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String message) {
        super(message);
    }

    public AccesoDenegadoException(String message, Throwable cause) {
        super(message, cause);
    }
}
