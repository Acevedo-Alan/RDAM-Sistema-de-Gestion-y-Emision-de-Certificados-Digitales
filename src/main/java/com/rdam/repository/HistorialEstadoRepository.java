package com.rdam.repository;

import com.rdam.domain.entity.HistorialEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialEstadoRepository extends JpaRepository<HistorialEstado, Integer> {

    List<HistorialEstado> findBySolicitudIdOrderByCreatedAtDesc(Integer solicitudId);
}
