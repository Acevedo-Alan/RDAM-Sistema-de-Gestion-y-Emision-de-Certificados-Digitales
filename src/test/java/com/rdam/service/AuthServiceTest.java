package com.rdam.service;

import com.rdam.domain.entity.Empleado;
import com.rdam.domain.entity.RolUsuario;
import com.rdam.domain.entity.Usuario;
import com.rdam.dto.AuthResponse;
import com.rdam.dto.LoginResponse;
import com.rdam.repository.EmpleadoRepository;
import com.rdam.repository.UsuarioRepository;
import com.rdam.security.jwt.JwtService;
import com.rdam.security.provider.CiudadanoAuthToken;
import com.rdam.security.provider.EmpleadoAuthToken;
import com.rdam.security.service.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EmpleadoRepository empleadoRepository;

    @Mock
    private OtpService otpService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private Usuario ciudadano;
    private Usuario empleadoUsuario;
    private Empleado empleado;
    private CustomUserDetails ciudadanoDetails;
    private CustomUserDetails empleadoDetails;

    @BeforeEach
    void setUp() {
        ciudadano = new Usuario();
        ciudadano.setId(1);
        ciudadano.setCuil("20345678901");
        ciudadano.setEmail("ciudadano@test.com");
        ciudadano.setPasswordHash("$2a$10$hashedpassword");
        ciudadano.setRol(RolUsuario.ciudadano);
        ciudadano.setActivo(true);
        ciudadano.setNombre("Juan");
        ciudadano.setApellido("Perez");

        empleadoUsuario = new Usuario();
        empleadoUsuario.setId(2);
        empleadoUsuario.setCuil("20987654321");
        empleadoUsuario.setEmail("interno@test.com");
        empleadoUsuario.setPasswordHash("$2a$10$hashedpassword");
        empleadoUsuario.setRol(RolUsuario.interno);
        empleadoUsuario.setActivo(true);
        empleadoUsuario.setNombre("Maria");
        empleadoUsuario.setApellido("Garcia");

        empleado = new Empleado();
        empleado.setId(1);
        empleado.setUsuario(empleadoUsuario);
        empleado.setLegajo("LEG-001");
        empleado.setCircunscripcionId(1);

        ciudadanoDetails = CustomUserDetails.fromCiudadano(
                1, "20345678901", "$2a$10$hashedpassword", "ciudadano", true);

        empleadoDetails = CustomUserDetails.fromEmpleado(
                2, "LEG-001", "$2a$10$hashedpassword", "interno", true, 1);
    }

    // ── authenticate() ──

    @Test
    void authenticate_loginExitosoCiudadano_retornaEmail() {
        String cuil = "20345678901";
        String password = "password123";

        when(usuarioRepository.findByCuilAndActivoTrue(cuil))
                .thenReturn(Optional.of(ciudadano));

        Authentication authResult = mock(Authentication.class);
        when(authResult.getPrincipal()).thenReturn(ciudadanoDetails);
        when(authenticationManager.authenticate(any(CiudadanoAuthToken.class)))
                .thenReturn(authResult);

        when(usuarioRepository.findById(1)).thenReturn(Optional.of(ciudadano));

        String email = authService.authenticate(cuil, password);

        assertEquals("ciudadano@test.com", email);
        verify(authenticationManager).authenticate(any(CiudadanoAuthToken.class));
    }

    @Test
    void authenticate_loginExitosoEmpleado_retornaEmail() {
        String legajo = "LEG-001";
        String password = "password123";

        when(usuarioRepository.findByCuilAndActivoTrue(legajo))
                .thenReturn(Optional.empty());

        Authentication authResult = mock(Authentication.class);
        when(authResult.getPrincipal()).thenReturn(empleadoDetails);
        when(authenticationManager.authenticate(any(EmpleadoAuthToken.class)))
                .thenReturn(authResult);

        when(usuarioRepository.findById(2)).thenReturn(Optional.of(empleadoUsuario));

        String email = authService.authenticate(legajo, password);

        assertEquals("interno@test.com", email);
        verify(authenticationManager).authenticate(any(EmpleadoAuthToken.class));
    }

    @Test
    void authenticate_usuarioNoEncontradoPostAuth_lanzaBadCredentials() {
        String cuil = "20345678901";
        String password = "password123";

        when(usuarioRepository.findByCuilAndActivoTrue(cuil))
                .thenReturn(Optional.of(ciudadano));

        Authentication authResult = mock(Authentication.class);
        when(authResult.getPrincipal()).thenReturn(ciudadanoDetails);
        when(authenticationManager.authenticate(any(CiudadanoAuthToken.class)))
                .thenReturn(authResult);

        when(usuarioRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class,
                () -> authService.authenticate(cuil, password));
    }

    // ── completarLoginConOtp() ──

    @Test
    void completarLoginConOtp_otpValido_retornaAuthResponse() {
        String email = "ciudadano@test.com";
        String codigo = "123456";

        when(otpService.validarOtp(email, codigo)).thenReturn(true);
        when(usuarioRepository.findByEmailAndActivoTrue(email))
                .thenReturn(Optional.of(ciudadano));
        when(jwtService.generateToken(any(CustomUserDetails.class)))
                .thenReturn("jwt-token-generado");

        AuthResponse response = authService.completarLoginConOtp(email, codigo);

        assertNotNull(response);
        assertEquals("jwt-token-generado", response.token());
        verify(otpService).validarOtp(email, codigo);
        verify(jwtService).generateToken(any(CustomUserDetails.class));
    }

    @Test
    void completarLoginConOtp_otpInvalido_lanzaBadCredentials() {
        String email = "ciudadano@test.com";
        String codigo = "000000";

        when(otpService.validarOtp(email, codigo)).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> authService.completarLoginConOtp(email, codigo));
    }

    // ── renovarToken() ──

    @Test
    void renovarToken_tokenValido_retornaLoginResponse() {
        String refreshToken = "valid-refresh-token";

        when(jwtService.isTokenValid(refreshToken)).thenReturn(true);
        when(jwtService.extractTokenType(refreshToken)).thenReturn("refresh");
        when(jwtService.extractUserId(refreshToken)).thenReturn(1);
        when(jwtService.extractUsername(refreshToken)).thenReturn("20345678901");
        when(jwtService.extractCircunscripcionId(refreshToken)).thenReturn(null);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(ciudadano));
        when(jwtService.generateToken(any(CustomUserDetails.class)))
                .thenReturn("new-access-token");

        LoginResponse response = authService.renovarToken(refreshToken);

        assertNotNull(response);
        assertEquals("new-access-token", response.accessToken());
        assertEquals(refreshToken, response.refreshToken());
        assertEquals("ciudadano", response.rol());
        assertEquals(1, response.userId());
    }

    @Test
    void renovarToken_tokenInvalido_lanzaIllegalArgument() {
        String refreshToken = "invalid-token";
        when(jwtService.isTokenValid(refreshToken)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> authService.renovarToken(refreshToken));
    }

    @Test
    void renovarToken_noEsRefreshToken_lanzaIllegalArgument() {
        String accessToken = "access-token";
        when(jwtService.isTokenValid(accessToken)).thenReturn(true);
        when(jwtService.extractTokenType(accessToken)).thenReturn("access");

        assertThrows(IllegalArgumentException.class,
                () -> authService.renovarToken(accessToken));
    }

    @Test
    void renovarToken_usuarioInactivo_lanzaIllegalArgument() {
        String refreshToken = "valid-refresh-token";
        ciudadano.setActivo(false);

        when(jwtService.isTokenValid(refreshToken)).thenReturn(true);
        when(jwtService.extractTokenType(refreshToken)).thenReturn("refresh");
        when(jwtService.extractUserId(refreshToken)).thenReturn(1);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(ciudadano));

        assertThrows(IllegalArgumentException.class,
                () -> authService.renovarToken(refreshToken));
    }

    // ── registrarCiudadano() ──
    // TODO: Pendiente de implementacion en AuthService.
    // Los siguientes tests quedan como especificacion TDD:
    //   - registrarCiudadano_exitoso_guardaUsuario
    //   - registrarCiudadano_cuilYaRegistrado_lanzaIllegalArgument
    //   - registrarCiudadano_emailEnUso_lanzaIllegalArgument
    //   - registrarCiudadano_cuilNoEnPadron_lanzaCuilNoEncontradoEnPadronException
    // Requiere: PadronService, PasswordEncoder (no inyectados actualmente en AuthService).
}
