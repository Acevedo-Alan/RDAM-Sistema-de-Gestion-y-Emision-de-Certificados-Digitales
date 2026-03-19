package com.rdam.repository;

import com.rdam.domain.entity.Circunscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CircunscripcionRepository extends JpaRepository<Circunscripcion, Integer> {

    List<Circunscripcion> findByActivoTrue();

    Optional<Circunscripcion> findByCodigo(String codigo);
}
