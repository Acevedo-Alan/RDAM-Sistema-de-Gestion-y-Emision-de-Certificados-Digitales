package com.rdam.repository;

import com.rdam.domain.entity.EstadoSolicitud;
import com.rdam.domain.entity.Solicitud;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

// TODO (Producción): Índice compuesto recomendado sobre (estado, circunscripcion_id)
// para optimizar consultas de bandeja de revisión de empleados.
// No alterar la base de datos en esta fase académica.
@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")})
    @Query("SELECT s FROM Solicitud s WHERE s.id = :id")
    Optional<Solicitud> findByIdForUpdate(@Param("id") Integer id);

    @Query("SELECT s.estado, COUNT(s) FROM Solicitud s GROUP BY s.estado")
    List<Object[]> countGroupByEstado();

    @Query("SELECT s.estado, COUNT(s) FROM Solicitud s WHERE s.circunscripcion.id = :circId GROUP BY s.estado")
    List<Object[]> countGroupByEstadoByCircunscripcion(@Param("circId") Integer circunscripcionId);

    @Query("SELECT s.circunscripcion.nombre, COUNT(s) FROM Solicitud s GROUP BY s.circunscripcion.nombre")
    List<Object[]> countGroupByCircunscripcion();

    @Query("SELECT COALESCE(SUM(s.montoArancel), 0) FROM Solicitud s WHERE s.estado IN :estados")
    BigDecimal sumMontoByEstados(@Param("estados") List<EstadoSolicitud> estados);

    @Query("SELECT COUNT(s) FROM Solicitud s WHERE CAST(s.createdAt AS date) = CURRENT_DATE")
    Long countHoy();

    @Query("SELECT COUNT(s) FROM Solicitud s WHERE CAST(s.createdAt AS date) = CURRENT_DATE AND s.circunscripcion.id = :circId")
    Long countHoyByCircunscripcion(@Param("circId") Integer circunscripcionId);

    @Query("SELECT COUNT(s) FROM Solicitud s WHERE s.createdAt >= :inicioSemana")
    Long countDesde(@Param("inicioSemana") OffsetDateTime inicioSemana);

    // ── Métodos para panel de administración ──

    Page<Solicitud> findByEstado(EstadoSolicitud estado, Pageable pageable);

    Page<Solicitud> findByCircunscripcionId(Integer circunscripcionId, Pageable pageable);

    Page<Solicitud> findByEstadoAndCircunscripcionId(EstadoSolicitud estado, Integer circunscripcionId, Pageable pageable);

    List<Solicitud> findByCreatedAtBetween(OffsetDateTime desde, OffsetDateTime hasta);

    @Query("SELECT s FROM Solicitud s WHERE "
            + "(:desde IS NULL OR s.createdAt >= :desde) AND "
            + "(:hasta IS NULL OR s.createdAt <= :hasta) AND "
            + "(:estado IS NULL OR s.estado = :estado) AND "
            + "(:circId IS NULL OR s.circunscripcion.id = :circId)")
    List<Solicitud> findAllWithFilters(@Param("desde") OffsetDateTime desde,
                                       @Param("hasta") OffsetDateTime hasta,
                                       @Param("estado") EstadoSolicitud estado,
                                       @Param("circId") Integer circunscripcionId);

    // ── Métodos para endpoints de solicitudes ──

    List<Solicitud> findByCiudadanoIdOrderByCreatedAtDesc(Integer ciudadanoId);

    List<Solicitud> findByCircunscripcionIdAndEstadoInOrderByCreatedAtAsc(
            Integer circunscripcionId, List<EstadoSolicitud> estados);

    List<Solicitud> findByEstadoInOrderByCreatedAtAsc(List<EstadoSolicitud> estados);
}
