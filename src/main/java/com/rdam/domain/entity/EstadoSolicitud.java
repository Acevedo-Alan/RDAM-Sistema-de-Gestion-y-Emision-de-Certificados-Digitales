package com.rdam.domain.entity;

/**
 * Enum que mapea el tipo PostgreSQL estado_solicitud.
 * Valores en UPPER_CASE tal como están definidos en el DDL.
 */
public enum EstadoSolicitud {
    PENDIENTE_REVISION,
    EN_REVISION,
    APROBADA,
    RECHAZADA,
    PAGADA,
    EMITIDA,
    CANCELADA
}
