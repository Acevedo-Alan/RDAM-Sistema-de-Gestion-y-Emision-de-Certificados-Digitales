package com.rdam.service.impl;

import com.rdam.domain.entity.Empleado;
import com.rdam.domain.entity.RolUsuario;
import com.rdam.domain.entity.Usuario;
import com.rdam.dto.CrearUsuarioRequest;
import com.rdam.dto.UsuarioAdminResponse;
import com.rdam.repository.EmpleadoRepository;
import com.rdam.repository.UsuarioRepository;
import com.rdam.security.provider.CiudadanoAuthProvider;
import com.rdam.service.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EmpleadoRepository empleadoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CiudadanoAuthProvider ciudadanoAuthProvider;

    @InjectMocks
    private AdminUsuarioServiceImpl adminUsuarioService;

    private Usuario ciudadano;

    @BeforeEach
    void setUp() {
        ciudadano = new Usuario();
        ciudadano.setId(1);
        ciudadano.setNombre("Juan");
        ciudadano.setApellido("Perez");
        ciudadano.setEmail("juan@test.com");
        ciudadano.setCuil("20345678901");
        ciudadano.setRol(RolUsuario.ciudadano);
        ciudadano.setActivo(true);
    }

    // ── crearUsuario() — ciudadano ──

    @Test
    void crearUsuario_ciudadanoExitoso_hasheaPasswordYGuarda() {
        CrearUsuarioRequest request = new CrearUsuarioRequest(
                "20345678901", "Juan", "Perez", "juan@test.com",
                "password123", RolUsuario.ciudadano, null, null);

        when(usuarioRepository.findByEmailAndActivoTrue("juan@test.com"))
                .thenReturn(Optional.empty());
        when(usuarioRepository.findByCuilAndActivoTrue("20345678901"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashed");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(1);
            return u;
        });
        when(empleadoRepository.findByUsuarioId(1)).thenReturn(Optional.empty());

        UsuarioAdminResponse response = adminUsuarioService.crearUsuario(request);

        assertNotNull(response);
        assertEquals("Juan", response.nombre());
        assertEquals("ciudadano", response.rol());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("$2a$10$hashed", captor.getValue().getPasswordHash());
    }

    @Test
    void crearUsuario_emailYaEnUso_lanzaIllegalArgument() {
        CrearUsuarioRequest request = new CrearUsuarioRequest(
                "20345678901", "Juan", "Perez", "existente@test.com",
                "password123", RolUsuario.ciudadano, null, null);

        when(usuarioRepository.findByEmailAndActivoTrue("existente@test.com"))
                .thenReturn(Optional.of(ciudadano));

        assertThrows(IllegalArgumentException.class,
                () -> adminUsuarioService.crearUsuario(request));
    }

    @Test
    void crearUsuario_cuilYaEnUso_lanzaIllegalArgument() {
        CrearUsuarioRequest request = new CrearUsuarioRequest(
                "20345678901", "Juan", "Perez", "nuevo@test.com",
                "password123", RolUsuario.ciudadano, null, null);

        when(usuarioRepository.findByEmailAndActivoTrue("nuevo@test.com"))
                .thenReturn(Optional.empty());
        when(usuarioRepository.findByCuilAndActivoTrue("20345678901"))
                .thenReturn(Optional.of(ciudadano));

        assertThrows(IllegalArgumentException.class,
                () -> adminUsuarioService.crearUsuario(request));
    }

    // ── crearUsuario() — empleado/interno ──

    @Test
    void crearUsuario_internoExitoso_guardaUsuarioYEmpleado() {
        CrearUsuarioRequest request = new CrearUsuarioRequest(
                null, "Maria", "Garcia", "maria@test.com",
                "password123", RolUsuario.interno, 1, "Analista");

        when(usuarioRepository.findByEmailAndActivoTrue("maria@test.com"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashed");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(5);
            return u;
        });
        when(empleadoRepository.save(any(Empleado.class))).thenAnswer(inv -> inv.getArgument(0));
        when(empleadoRepository.findByUsuarioId(5)).thenReturn(Optional.of(new Empleado()));

        UsuarioAdminResponse response = adminUsuarioService.crearUsuario(request);

        assertNotNull(response);
        verify(usuarioRepository).save(any(Usuario.class));
        verify(empleadoRepository).save(any(Empleado.class));
    }

    @Test
    void crearUsuario_internoSinCircunscripcion_lanzaIllegalArgument() {
        CrearUsuarioRequest request = new CrearUsuarioRequest(
                null, "Maria", "Garcia", "maria@test.com",
                "password123", RolUsuario.interno, null, "Analista");

        when(usuarioRepository.findByEmailAndActivoTrue("maria@test.com"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> adminUsuarioService.crearUsuario(request));
    }

    // ── desactivarUsuario() ──

    @Test
    void desactivarUsuario_usuarioExistente_setActivoFalse() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(ciudadano));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        adminUsuarioService.desactivarUsuario(1);

        assertFalse(ciudadano.getActivo());
        verify(usuarioRepository).save(ciudadano);
    }

    @Test
    void desactivarUsuario_usuarioNoEncontrado_lanzaRecursoNoEncontrado() {
        when(usuarioRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> adminUsuarioService.desactivarUsuario(999));
    }

    // ── resetBloqueo() ──

    @Test
    void resetBloqueo_llamaResetearBloqueoEnProvider() {
        adminUsuarioService.resetBloqueo("20345678901");

        verify(ciudadanoAuthProvider).resetearBloqueo("20345678901");
    }
}
