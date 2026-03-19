package com.rdam.repository;

import com.rdam.domain.entity.Adjunto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdjuntoRepository extends JpaRepository<Adjunto, Integer> {

    List<Adjunto> findBySolicitudIdOrderByCreatedAtAsc(Integer solicitudId);

    long countBySolicitudId(Integer solicitudId);
}
