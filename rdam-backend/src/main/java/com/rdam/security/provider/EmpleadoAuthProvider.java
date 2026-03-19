package com.rdam.security.provider;

import com.rdam.domain.entity.Empleado;
import com.rdam.domain.entity.Usuario;
import com.rdam.repository.EmpleadoRepository;
import com.rdam.security.service.CustomUserDetails;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional; // ⬅️ NUEVO IMPORT

/**
 * AuthenticationProvider para empleados (interno/admin).
 * Valida legajo contra mock LDAP (simulado verificando existencia en DB).
 * No utiliza password local: la autenticación se delega al directorio externo.
 */
@Component
public class EmpleadoAuthProvider implements AuthenticationProvider {

    private final EmpleadoRepository empleadoRepository;

    public EmpleadoAuthProvider(EmpleadoRepository empleadoRepository) {
        this.empleadoRepository = empleadoRepository;
    }

    @Override
    @Transactional(readOnly = true) // ⬅️ LA MAGIA: Mantiene la conexión a la base de datos abierta
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String legajo = authentication.getName();
        String password = authentication.getCredentials().toString();

        // Buscar empleado por legajo
        Empleado empleado = empleadoRepository.findByLegajo(legajo)
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        // Validar contra mock LDAP
        if (!validarContraLdapMock(legajo, password)) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        // ⬅️ Gracias al @Transactional, Hibernate ahora sí puede ir a buscar al Usuario sin crashear
        Usuario usuario = empleado.getUsuario(); 

        CustomUserDetails userDetails = CustomUserDetails.fromEmpleado(
                usuario.getId(),
                empleado.getLegajo(),
                usuario.getPasswordHash(),
                usuario.getRol().name(),
                usuario.getActivo(),
                empleado.getCircunscripcionId()
        );

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        // Aseguramos que acepte el token genérico de Spring y el tuyo personalizado
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication) || 
            EmpleadoAuthToken.class.isAssignableFrom(authentication);
    }

    /**
     * Mock LDAP: simula validación contra directorio externo.
     * En un entorno real se conectaría a un servidor LDAP corporativo.
     * Devuelve true si el legajo existe en la base de datos (ya verificado arriba).
     */
    private boolean validarContraLdapMock(String legajo, String password) {
        // Mock: acepta cualquier password si el legajo existe en la DB.
        // La existencia del legajo ya fue verificada al hacer findByLegajo.
        return empleadoRepository.findByLegajo(legajo).isPresent();
    }
}