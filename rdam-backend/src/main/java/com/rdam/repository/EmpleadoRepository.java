package com.rdam.repository;

import com.rdam.domain.entity.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Integer> {

    Optional<Empleado> findByLegajo(String legajo);

    Optional<Empleado> findByUsuarioId(Integer usuarioId);
}
