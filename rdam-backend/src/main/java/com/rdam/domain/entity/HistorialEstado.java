package com.rdam.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * Entidad JPA de solo lectura que mapea la tabla historial_estados del DDL.
 * Log inmutable de auditoría de todos los cambios de estado de una solicitud.
 *
 * NUNCA se modifica ni elimina desde la aplicación.
 * Los registros son insertados automáticamente por el trigger trg_solicitudes_auto_sync_historial.
 * La inmutabilidad está enforciada por triggers en PostgreSQL (trg_historial_estados_no_update/no_delete).
 */
@Entity
@Immutable
@Table(name = "historial_estados")
public class HistorialEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false, updatable = false)
    private Solicitud solicitud;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado_anterior")
    private EstadoSolicitud estadoAnterior;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado_nuevo", nullable = false)
    private EstadoSolicitud estadoNuevo;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    @Column(name = "empleado_id")
    private Integer empleadoId;

    @Column(name = "comentario", columnDefinition = "TEXT")
    private String comentario;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "ip_origen", columnDefinition = "inet")
    private String ipOrigen;

    @Column(name = "session_id", length = 128)
    private String sessionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // ── Constructor ──

    protected HistorialEstado() {
    }

    // ── Solo Getters (entidad inmutable, sin setters públicos) ──

    public Integer getId() {
        return id;
    }

    public Solicitud getSolicitud() {
        return solicitud;
    }

    public EstadoSolicitud getEstadoAnterior() {
        return estadoAnterior;
    }

    public EstadoSolicitud getEstadoNuevo() {
        return estadoNuevo;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public Integer getEmpleadoId() {
        return empleadoId;
    }

    public String getComentario() {
        return comentario;
    }

    public String getMetadata() {
        return metadata;
    }

    public String getIpOrigen() {
        return ipOrigen;
    }

    public String getSessionId() {
        return sessionId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
