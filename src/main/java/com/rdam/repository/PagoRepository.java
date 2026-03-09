package com.rdam.repository;

import com.rdam.domain.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Integer> {

    Optional<Pago> findBySolicitudId(Integer solicitudId);

    boolean existsBySolicitudId(Integer solicitudId);
}
