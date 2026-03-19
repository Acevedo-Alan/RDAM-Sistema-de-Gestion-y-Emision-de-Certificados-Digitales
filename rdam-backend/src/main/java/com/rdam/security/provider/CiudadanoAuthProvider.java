package com.rdam.security.provider;

import com.rdam.domain.entity.Usuario;
import com.rdam.repository.UsuarioRepository;
import com.rdam.security.service.CustomUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AuthenticationProvider para ciudadanos.
 * Valida CUIL + password BCrypt contra la base de datos.
 * Implementa bloqueo por intentos fallidos en memoria.
 */
@Component
public class CiudadanoAuthProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(CiudadanoAuthProvider.class);

    private static final int MAX_INTENTOS = 5;
    private static final int MINUTOS_BLOQUEO = 15;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /** Mapa en memoria para rastrear intentos fallidos por CUIL */
    private final ConcurrentHashMap<String, LoginAttempt> intentosLogin = new ConcurrentHashMap<>();

    public CiudadanoAuthProvider(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String cuil = authentication.getName();
        String password = authentication.getCredentials().toString();


        // Verificar bloqueo temporal
        verificarBloqueo(cuil);

        // Buscar ciudadano activo por CUIL
        Usuario usuario = usuarioRepository.findByCuilAndActivoTrue(cuil)
                .orElseThrow(() -> {
                    log.warn("action=AUTH_FAILED_USER_NOT_FOUND cuil={}", cuil);
                    registrarIntentoFallido(cuil);
                    return new BadCredentialsException("Credenciales inválidas");
                });


        // FIX: .trim() quita espacios extra que PostgreSQL añade a campos CHAR
        String dbHash = usuario.getPasswordHash() != null ? usuario.getPasswordHash().trim() : "";


        // Validar password con BCrypt
        if (!passwordEncoder.matches(password, dbHash)) {
            log.warn("action=AUTH_FAILED_BAD_CREDENTIALS cuil={}", cuil);
            registrarIntentoFallido(cuil);
            throw new BadCredentialsException("Credenciales inválidas");
        }


        // Login exitoso: limpiar intentos fallidos
        intentosLogin.remove(cuil);

        CustomUserDetails userDetails = CustomUserDetails.fromCiudadano(
                usuario.getId(),
                usuario.getCuil(),
                dbHash,
                usuario.getRol().name(),
                usuario.getActivo());

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication) ||
                CiudadanoAuthToken.class.isAssignableFrom(authentication);
    }

    /**
     * Resetea el bloqueo de login para un CUIL dado.
     * Utilizado desde el panel de administración.
     */
    public void resetearBloqueo(String cuil) {
        intentosLogin.remove(cuil);
        log.info("action=BLOQUEO_RESETEADO cuil={}", cuil);
    }

    /**
     * Verifica si el CUIL está temporalmente bloqueado.
     */
    private void verificarBloqueo(String cuil) {
        LoginAttempt attempt = intentosLogin.get(cuil);
        if (attempt == null) {
            return;
        }
        if (attempt.bloqueadoHasta() != null && LocalDateTime.now().isBefore(attempt.bloqueadoHasta())) {
            throw new LockedException("Cuenta bloqueada temporalmente. Intente nuevamente en 15 minutos.");
        }
        // Si el bloqueo ya expiró, limpiar el registro
        if (attempt.bloqueadoHasta() != null && LocalDateTime.now().isAfter(attempt.bloqueadoHasta())) {
            intentosLogin.remove(cuil);
        }
    }

    /**
     * Registra un intento fallido. Si se alcanza el máximo, bloquea temporalmente.
     */
    private void registrarIntentoFallido(String cuil) {
        intentosLogin.compute(cuil, (key, existing) -> {
            if (existing == null) {
                return new LoginAttempt(1, null);
            }
            int nuevosIntentos = existing.intentos() + 1;
            LocalDateTime bloqueo = nuevosIntentos >= MAX_INTENTOS
                    ? LocalDateTime.now().plusMinutes(MINUTOS_BLOQUEO)
                    : existing.bloqueadoHasta();
            return new LoginAttempt(nuevosIntentos, bloqueo);
        });
    }

    /**
     * Record simple para rastrear intentos de login fallidos.
     */
    private record LoginAttempt(int intentos, LocalDateTime bloqueadoHasta) {
    }
}