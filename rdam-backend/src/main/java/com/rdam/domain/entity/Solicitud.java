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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Entidad JPA que mapea la tabla solicitudes del DDL.
 * Entidad core del sistema: cada fila representa un trámite de solicitud de certificado.
 *
 * La máquina de estados es validada por el trigger trg_solicitudes_state_machine en PostgreSQL.
 * El historial de estados es registrado automáticamente por el trigger trg_solicitudes_auto_sync_historial.
 */
@Entity
@Table(name = "solicitudes")
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ciudadano_id", nullable = false)
    private Usuario ciudadano;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_certificado_id", nullable = false)
    private TipoCertificado tipoCertificado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "circunscripcion_id", nullable = false)
    private Circunscripcion circunscripcion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado", nullable = false)
    private EstadoSolicitud estado;

    @Column(name = "motivo_rechazo", columnDefinition = "TEXT")
    private String motivoRechazo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_asignado_id")
    private Empleado empleadoAsignado;

    @Column(name = "fecha_deseada")
    private LocalDate fechaDeseada;

    @Column(name = "fecha_asignacion")
    private OffsetDateTime fechaAsignacion;

    @Column(name = "fecha_aprobacion")
    private OffsetDateTime fechaAprobacion;

    @Column(name = "fecha_rechazo")
    private OffsetDateTime fechaRechazo;

    @Column(name = "fecha_pago")
    private OffsetDateTime fechaPago;

    @Column(name = "fecha_emision")
    private OffsetDateTime fechaEmision;

    @Column(name = "fecha_cancelacion")
    private OffsetDateTime fechaCancelacion;

    @Column(name = "numero_tramite", unique = true, nullable = false, insertable = false, updatable = false)
    private String numeroTramite;

    @Column(name = "monto_arancel", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoArancel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // ── Lifecycle hooks ──

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // ── Constructors ──

    public Solicitud() {
    }

    // ── Getters y Setters ──

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Usuario getCiudadano() {
        return ciudadano;
    }

    public void setCiudadano(Usuario ciudadano) {
        this.ciudadano = ciudadano;
    }

    public TipoCertificado getTipoCertificado() {
        return tipoCertificado;
    }

    public void setTipoCertificado(TipoCertificado tipoCertificado) {
        this.tipoCertificado = tipoCertificado;
    }

    public Circunscripcion getCircunscripcion() {
        return circunscripcion;
    }

    public void setCircunscripcion(Circunscripcion circunscripcion) {
        this.circunscripcion = circunscripcion;
    }

    public EstadoSolicitud getEstado() {
        return estado;
    }

    public void setEstado(EstadoSolicitud estado) {
        this.estado = estado;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public Empleado getEmpleadoAsignado() {
        return empleadoAsignado;
    }

    public void setEmpleadoAsignado(Empleado empleadoAsignado) {
        this.empleadoAsignado = empleadoAsignado;
    }

    public LocalDate getFechaDeseada() {
        return fechaDeseada;
    }

    public void setFechaDeseada(LocalDate fechaDeseada) {
        this.fechaDeseada = fechaDeseada;
    }

    public OffsetDateTime getFechaAsignacion() {
        return fechaAsignacion;
    }

    public void setFechaAsignacion(OffsetDateTime fechaAsignacion) {
        this.fechaAsignacion = fechaAsignacion;
    }

    public OffsetDateTime getFechaAprobacion() {
        return fechaAprobacion;
    }

    public void setFechaAprobacion(OffsetDateTime fechaAprobacion) {
        this.fechaAprobacion = fechaAprobacion;
    }

    public OffsetDateTime getFechaRechazo() {
        return fechaRechazo;
    }

    public void setFechaRechazo(OffsetDateTime fechaRechazo) {
        this.fechaRechazo = fechaRechazo;
    }

    public OffsetDateTime getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(OffsetDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }

    public OffsetDateTime getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(OffsetDateTime fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public OffsetDateTime getFechaCancelacion() {
        return fechaCancelacion;
    }

    public void setFechaCancelacion(OffsetDateTime fechaCancelacion) {
        this.fechaCancelacion = fechaCancelacion;
    }

    public String getNumeroTramite() {
        return numeroTramite;
    }

    public void setNumeroTramite(String numeroTramite) {
        this.numeroTramite = numeroTramite;
    }

    public BigDecimal getMontoArancel() {
        return montoArancel;
    }

    public void setMontoArancel(BigDecimal montoArancel) {
        this.montoArancel = montoArancel;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}