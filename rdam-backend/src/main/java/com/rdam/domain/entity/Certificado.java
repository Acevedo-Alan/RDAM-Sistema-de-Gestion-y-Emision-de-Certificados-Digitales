package com.rdam.domain.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Entidad JPA que mapea la tabla certificados del DDL.
 * Relacion 1:1 con solicitudes. numero_certificado generado por
 * fn_numero_certificado() en DB.
 */
@Entity
@Table(name = "certificados")
public class Certificado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false, unique = true)
    private Solicitud solicitud;

    @Column(name = "numero_certificado", nullable = false, length = 30, insertable = false, updatable = false)
    private String numeroCertificado;

    @Column(name = "token", nullable = false, length = 64, unique = true)
    private String token;

    @Column(name = "ruta_pdf", nullable = false, columnDefinition = "TEXT")
    private String rutaPdf;

    @Column(name = "hash_pdf", nullable = false, columnDefinition = "CHAR(64)")
    @JdbcTypeCode(SqlTypes.CHAR) // <--- ¡ESTE ES EL ESCUDO ANTI-HIBERNATE!
    private String hashPdf;

    @Column(name = "plantilla_ver", nullable = false)
    private Integer plantillaVer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emitido_por_id", nullable = false)
    private Empleado emitidoPor;

    @Column(name = "fecha_emision", nullable = false)
    private OffsetDateTime fechaEmision;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // -- Constructors --

    public Certificado() {
    }

    // -- Getters y Setters --

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Solicitud getSolicitud() {
        return solicitud;
    }

    public void setSolicitud(Solicitud solicitud) {
        this.solicitud = solicitud;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNumeroCertificado() {
        return numeroCertificado;
    }

    public String getRutaPdf() {
        return rutaPdf;
    }

    public void setRutaPdf(String rutaPdf) {
        this.rutaPdf = rutaPdf;
    }

    public String getHashPdf() {
        return hashPdf;
    }

    public void setHashPdf(String hashPdf) {
        this.hashPdf = hashPdf;
    }

    public Integer getPlantillaVer() {
        return plantillaVer;
    }

    public void setPlantillaVer(Integer plantillaVer) {
        this.plantillaVer = plantillaVer;
    }

    public Empleado getEmitidoPor() {
        return emitidoPor;
    }

    public void setEmitidoPor(Empleado emitidoPor) {
        this.emitidoPor = emitidoPor;
    }

    public OffsetDateTime getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(OffsetDateTime fechaEmision) {
        this.fechaEmision = fechaEmision;
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
