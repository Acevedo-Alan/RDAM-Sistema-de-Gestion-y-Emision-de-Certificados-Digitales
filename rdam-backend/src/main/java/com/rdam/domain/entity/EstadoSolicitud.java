package com.rdam.domain.entity;

/**
 * Enum que mapea el tipo PostgreSQL estado_solicitud.
 * Valores en UPPER_CASE tal como estan definidos en el DDL.
 */
public enum EstadoSolicitud {
    PENDIENTE,
    PAGADO,
    PUBLICADO,
    PUBLICADO_VENCIDO,
    VENCIDO,
    RECHAZADO,
    CANCELADO
}
