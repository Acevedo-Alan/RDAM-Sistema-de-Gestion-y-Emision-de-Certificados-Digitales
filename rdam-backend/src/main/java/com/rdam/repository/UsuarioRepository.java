package com.rdam.repository;

import com.rdam.domain.entity.RolUsuario;
import com.rdam.domain.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByCuilAndActivoTrue(String cuil);

    Optional<Usuario> findByEmailAndActivoTrue(String email);

    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.rol = :rol AND u.activo = true")
    Long countByRolAndActivoTrue(@Param("rol") RolUsuario rol);

    List<Usuario> findByActivoTrue();

    Optional<Usuario> findByIdAndEliminadoEnIsNull(Integer id);

    boolean existsByEmail(String email);

    boolean existsByCuil(String cuil);
}
