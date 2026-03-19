package com.rdam.repository;

import com.rdam.domain.entity.TipoCertificado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoCertificadoRepository extends JpaRepository<TipoCertificado, Integer> {

    List<TipoCertificado> findByActivoTrue();

    Optional<TipoCertificado> findByNombreAndEliminadoEnIsNull(String nombre);
}
