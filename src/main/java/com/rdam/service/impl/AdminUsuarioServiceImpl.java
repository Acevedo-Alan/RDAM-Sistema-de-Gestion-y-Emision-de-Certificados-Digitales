package com.rdam.service.impl;

import com.rdam.domain.entity.Empleado;
import com.rdam.domain.entity.RolUsuario;
import com.rdam.domain.entity.Usuario;
import com.rdam.dto.CrearUsuarioRequest;
import com.rdam.dto.EditarUsuarioRequest;
import com.rdam.dto.UsuarioAdminResponse;
import com.rdam.repository.EmpleadoRepository;
import com.rdam.repository.UsuarioRepository;
import com.rdam.security.provider.CiudadanoAuthProvider;
import com.rdam.service.AdminUsuarioService;
import com.rdam.service.exception.RecursoNoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class AdminUsuarioServiceImpl implements AdminUsuarioService {

    private static final Logger log = LoggerFactory.getLogger(AdminUsuarioServiceImpl.class);

    private final UsuarioRepository usuarioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final PasswordEncoder passwordEncoder;
    private final CiudadanoAuthProvider ciudadanoAuthProvider;

    public AdminUsuarioServiceImpl(UsuarioRepository usuarioRepository,
                                   EmpleadoRepository empleadoRepository,
                                   PasswordEncoder passwordEncoder,
                                   CiudadanoAuthProvider ciudadanoAuthProvider) {
        this.usuarioRepository = usuarioRepository;
        this.empleadoRepository = empleadoRepository;
        this.passwordEncoder = passwordEncoder;
        this.ciudadanoAuthProvider = ciudadanoAuthProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioAdminResponse> listarUsuarios(Pageable pageable) {
        log.info("action=ADMIN_LISTAR_USUARIOS page={} size={}", pageable.getPageNumber(), pageable.getPageSize());
        return usuarioRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioAdminResponse obtenerUsuario(Integer id) {
        log.info("action=ADMIN_OBTENER_USUARIO id={}", id);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con id=" + id));
        return toResponse(usuario);
    }

    @Override
    @Transactional
    public UsuarioAdminResponse crearUsuario(CrearUsuarioRequest request) {
        log.info("action=ADMIN_CREAR_USUARIO email={} rol={}", request.email(), request.rol());

        // Validar unicidad de email
        usuarioRepository.findByEmailAndActivoTrue(request.email()).ifPresent(u -> {
            throw new IllegalArgumentException("Ya existe un usuario activo con email=" + request.email());
        });

        // Validar CUIL para ciudadanos
        if (request.rol() == RolUsuario.ciudadano) {
            if (request.cuil() == null || request.cuil().isBlank()) {
                throw new IllegalArgumentException("El CUIL es obligatorio para ciudadanos");
            }
            usuarioRepository.findByCuilAndActivoTrue(request.cuil()).ifPresent(u -> {
                throw new IllegalArgumentException("Ya existe un usuario activo con cuil=" + request.cuil());
            });
        }

        // Validar circunscripcion para empleados
        if (request.rol() != RolUsuario.ciudadano && request.circunscripcionId() == null) {
            throw new IllegalArgumentException("La circunscripción es obligatoria para empleados");
        }

        OffsetDateTime now = OffsetDateTime.now();

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setApellido(request.apellido());
        usuario.setEmail(request.email());
        usuario.setCuil(request.cuil());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(request.rol());
        usuario.setActivo(true);
        usuario.setCreatedAt(now);
        usuario.setUpdatedAt(now);

        usuario = usuarioRepository.save(usuario);

        // Si es interno o admin, crear perfil Empleado
        if (request.rol() != RolUsuario.ciudadano) {
            Empleado empleado = new Empleado();
            empleado.setUsuario(usuario);
            empleado.setCircunscripcionId(request.circunscripcionId());
            empleado.setLegajo("LEG-" + System.currentTimeMillis());
            empleado.setCargo(request.cargo());
            empleado.setCreatedAt(now);
            empleado.setUpdatedAt(now);
            empleadoRepository.save(empleado);
        }

        log.info("action=ADMIN_USUARIO_CREADO id={} rol={}", usuario.getId(), usuario.getRol());
        return toResponse(usuario);
    }

    @Override
    @Transactional
    public UsuarioAdminResponse editarUsuario(Integer id, EditarUsuarioRequest request) {
        log.info("action=ADMIN_EDITAR_USUARIO id={}", id);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con id=" + id));

        if (request.nombre() != null && !request.nombre().isBlank()) {
            usuario.setNombre(request.nombre());
        }
        if (request.apellido() != null && !request.apellido().isBlank()) {
            usuario.setApellido(request.apellido());
        }
        if (request.email() != null && !request.email().isBlank()) {
            // Validar unicidad del nuevo email
            usuarioRepository.findByEmailAndActivoTrue(request.email()).ifPresent(u -> {
                if (!u.getId().equals(id)) {
                    throw new IllegalArgumentException("Ya existe un usuario activo con email=" + request.email());
                }
            });
            usuario.setEmail(request.email());
        }

        usuario.setUpdatedAt(OffsetDateTime.now());
        usuario = usuarioRepository.save(usuario);

        // Si tiene perfil empleado y se envió cargo, actualizar
        if (request.cargo() != null) {
            empleadoRepository.findByUsuarioId(id).ifPresent(empleado -> {
                empleado.setCargo(request.cargo());
                empleado.setUpdatedAt(OffsetDateTime.now());
                empleadoRepository.save(empleado);
            });
        }

        log.info("action=ADMIN_USUARIO_EDITADO id={}", id);
        return toResponse(usuario);
    }

    @Override
    @Transactional
    public void eliminarUsuario(Integer id) {
        log.info("action=ADMIN_ELIMINAR_USUARIO id={}", id);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con id=" + id));

        OffsetDateTime now = OffsetDateTime.now();
        usuario.setEliminadoEn(now);
        usuario.setActivo(false);
        usuario.setUpdatedAt(now);
        usuarioRepository.save(usuario);

        log.info("action=ADMIN_USUARIO_ELIMINADO id={}", id);
    }

    @Override
    @Transactional
    public void desactivarUsuario(Integer id) {
        log.info("action=ADMIN_DESACTIVAR_USUARIO id={}", id);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con id=" + id));

        usuario.setActivo(false);
        usuario.setUpdatedAt(OffsetDateTime.now());
        usuarioRepository.save(usuario);

        log.info("action=ADMIN_USUARIO_DESACTIVADO id={}", id);
    }

    @Override
    @Transactional
    public void activarUsuario(Integer id) {
        log.info("action=ADMIN_ACTIVAR_USUARIO id={}", id);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con id=" + id));

        usuario.setActivo(true);
        usuario.setUpdatedAt(OffsetDateTime.now());
        usuarioRepository.save(usuario);

        log.info("action=ADMIN_USUARIO_ACTIVADO id={}", id);
    }

    @Override
    public void resetBloqueo(String cuil) {
        log.info("action=ADMIN_RESET_BLOQUEO cuil={}", cuil);
        ciudadanoAuthProvider.resetearBloqueo(cuil);
    }

    private UsuarioAdminResponse toResponse(Usuario usuario) {
        Empleado empleado = empleadoRepository.findByUsuarioId(usuario.getId()).orElse(null);

        return new UsuarioAdminResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getCuil(),
                usuario.getRol().name(),
                usuario.getActivo(),
                usuario.getCreatedAt(),
                usuario.getEliminadoEn(),
                empleado != null ? empleado.getCircunscripcionId() : null,
                empleado != null ? empleado.getLegajo() : null,
                empleado != null ? empleado.getCargo() : null
        );
    }
}
