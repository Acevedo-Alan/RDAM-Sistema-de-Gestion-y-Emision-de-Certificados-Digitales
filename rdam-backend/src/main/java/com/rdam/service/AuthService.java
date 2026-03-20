package com.rdam.service;

import com.rdam.domain.entity.Empleado;
import com.rdam.domain.entity.RolUsuario;
import com.rdam.domain.entity.Usuario;
import com.rdam.dto.AuthResponse;
import com.rdam.dto.LoginResponse;
import com.rdam.dto.RegisterRequest;
import com.rdam.repository.EmpleadoRepository;
import com.rdam.repository.UsuarioRepository;
import com.rdam.security.jwt.JwtService;
import com.rdam.security.provider.CiudadanoAuthToken;
import com.rdam.security.provider.EmpleadoAuthToken;
import com.rdam.security.service.CustomUserDetails;
import com.rdam.service.exception.ConflictoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final OtpService otpService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager,
                       UsuarioRepository usuarioRepository,
                       EmpleadoRepository empleadoRepository,
                       OtpService otpService,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.empleadoRepository = empleadoRepository;
        this.otpService = otpService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public String authenticate(String username, String password) {
        Authentication result;

        if (usuarioRepository.findByCuilAndActivoTrue(username).isPresent()) {
            result = authenticationManager.authenticate(new CiudadanoAuthToken(username, password));
        } else {
            result = authenticationManager.authenticate(new EmpleadoAuthToken(username, password));
        }

        CustomUserDetails userDetails = (CustomUserDetails) result.getPrincipal();
        Usuario usuario = usuarioRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));

        logger.info("action=AUTH_SUCCESS userId={} username={}", userDetails.getUserId(), username);
        return usuario.getEmail();
    }

    public CustomUserDetails loadUserByEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));

        if (usuario.getRol() == RolUsuario.ciudadano) {
            return CustomUserDetails.fromCiudadano(
                    usuario.getId(),
                    usuario.getCuil(),
                    usuario.getPasswordHash(),
                    usuario.getRol().name(),
                    usuario.getActivo());
        }

        Empleado empleado = empleadoRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BadCredentialsException("Empleado no encontrado"));

        return CustomUserDetails.fromEmpleado(
                usuario.getId(),
                empleado.getLegajo(),
                usuario.getPasswordHash(),
                usuario.getRol().name(),
                usuario.getActivo(),
                empleado.getCircunscripcionId());
    }

    public AuthResponse completarLoginConOtp(String email, String codigo) {
        boolean valid = otpService.validarOtp(email, codigo);
        if (!valid) {
            throw new BadCredentialsException("Codigo OTP invalido o expirado");
        }
        CustomUserDetails userDetails = loadUserByEmail(email);
        String accessToken = jwtService.generateToken(userDetails);
        logger.info("action=LOGIN_SUCCESS email={} userId={}", email, userDetails.getUserId());
        return new AuthResponse(accessToken);
    }

    @Transactional
    public void registrarCiudadano(RegisterRequest request) {
        logger.info("action=REGISTRO_CIUDADANO email={} cuil={}", request.email(), request.cuil());

        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ConflictoException("El email ya esta registrado");
        }
        if (usuarioRepository.existsByCuil(request.cuil())) {
            throw new ConflictoException("El CUIL ya esta registrado");
        }

        String[] partes = request.nombre().trim().split("\\s+", 2);
        String nombre = partes[0];
        String apellido = partes.length > 1 ? partes[1] : "-";

        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setEmail(request.email());
        usuario.setCuil(request.cuil());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(RolUsuario.ciudadano);
        usuario.setActivo(true);
        usuario.setCreatedAt(OffsetDateTime.now());
        usuario.setUpdatedAt(OffsetDateTime.now());

        usuarioRepository.save(usuario);

        logger.info("action=REGISTRO_CIUDADANO_OK email={}", request.email());
    }

    public LoginResponse renovarToken(String refreshToken) {
        if (!jwtService.isTokenValid(refreshToken)) {
            throw new IllegalArgumentException("Refresh token invalido o expirado");
        }
        if (!"refresh".equals(jwtService.extractTokenType(refreshToken))) {
            throw new IllegalArgumentException("El token proporcionado no es un refresh token");
        }
        Integer userId = jwtService.extractUserId(refreshToken);
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (!usuario.getActivo()) {
            throw new IllegalArgumentException("Usuario desactivado. No se puede renovar el token.");
        }
        String rolActual = usuario.getRol().name();
        String username = jwtService.extractUsername(refreshToken);
        Integer circunscripcionId = jwtService.extractCircunscripcionId(refreshToken);
        CustomUserDetails userDetails = new CustomUserDetails(
                userId, username, null, rolActual, true, circunscripcionId);
        String newAccessToken = jwtService.generateToken(userDetails);
        logger.info("action=REFRESH_TOKEN_SUCCESS userId={}", userId);
        return new LoginResponse(newAccessToken, refreshToken, rolActual, username, rolActual, userId);
    }
}