package com.rdam.repository;

import com.rdam.domain.entity.Certificado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CertificadoRepository extends JpaRepository<Certificado, Integer> {

    Optional<Certificado> findBySolicitudId(Integer solicitudId);

    boolean existsBySolicitudId(Integer solicitudId);
}
