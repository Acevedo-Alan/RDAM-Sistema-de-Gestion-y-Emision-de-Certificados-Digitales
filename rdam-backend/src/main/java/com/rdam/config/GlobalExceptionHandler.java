package com.rdam.config;

import com.rdam.dto.ErrorResponse;
import com.rdam.service.exception.AccesoDenegadoException;
import com.rdam.service.exception.ConcurrenciaException;
import com.rdam.service.exception.EstadoInvalidoException;
import com.rdam.service.exception.RecursoNoEncontradoException;
import io.jsonwebtoken.ExpiredJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<ErrorResponse> handleAccesoDenegado(AccesoDenegadoException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "Acceso denegado", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Acceso denegado (Spring Security): {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "Acceso denegado", ex.getMessage());
    }

    @ExceptionHandler(EstadoInvalidoException.class)
    public ResponseEntity<ErrorResponse> handleEstadoInvalido(EstadoInvalidoException ex) {
        log.warn("Estado invalido: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, "Estado invalido", ex.getMessage());
    }

    @ExceptionHandler(ConcurrenciaException.class)
    public ResponseEntity<ErrorResponse> handleConcurrencia(ConcurrenciaException ex) {
        log.warn("Conflicto de concurrencia: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "Conflicto de concurrencia", ex.getMessage());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Argumento invalido: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Solicitud invalida", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Error de validacion");
        return buildResponse(HttpStatus.BAD_REQUEST, "Error de validacion", message);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleExpiredJwt(ExpiredJwtException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Token expirado", "El token JWT ha expirado");
    }

    @ExceptionHandler({BadCredentialsException.class, LockedException.class})
    public ResponseEntity<ErrorResponse> handleAuthenticationError(Exception ex) {
        log.warn("Error de autenticacion: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, "Error de autenticacion", ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        String rootMessage = ex.getMostSpecificCause().getMessage();
        if (rootMessage != null && rootMessage.contains("3 adjuntos")) {
            log.warn("Violacion de integridad (max adjuntos): {}", rootMessage);
            return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, "Estado invalido",
                    "No se pueden subir más de 3 adjuntos por solicitud");
        }
        log.error("Violacion de integridad de datos: {}", rootMessage);
        return buildResponse(HttpStatus.CONFLICT, "Conflicto de datos", "Violación de integridad de datos");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Error interno no manejado", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrio un error inesperado. Contacte al administrador.");
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String error, String message) {
        ErrorResponse body = new ErrorResponse(
                status.value(),
                error,
                message,
                OffsetDateTime.now()
        );
        return ResponseEntity.status(status).body(body);
    }
}
