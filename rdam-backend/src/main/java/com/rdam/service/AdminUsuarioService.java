package com.rdam.service;

import com.rdam.dto.CrearUsuarioRequest;
import com.rdam.dto.EditarUsuarioRequest;
import com.rdam.dto.UsuarioAdminResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUsuarioService {

    Page<UsuarioAdminResponse> listarUsuarios(Pageable pageable);

    UsuarioAdminResponse obtenerUsuario(Integer id);

    UsuarioAdminResponse crearUsuario(CrearUsuarioRequest request);

    UsuarioAdminResponse editarUsuario(Integer id, EditarUsuarioRequest request);

    void eliminarUsuario(Integer id);

    void desactivarUsuario(Integer id);

    void activarUsuario(Integer id);

    void resetBloqueo(String cuil);
}
