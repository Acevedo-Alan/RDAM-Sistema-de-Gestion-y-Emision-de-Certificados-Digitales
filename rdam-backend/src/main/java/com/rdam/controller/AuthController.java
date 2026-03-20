package com.rdam.controller;

import com.rdam.dto.AuthResponse;
import com.rdam.dto.LoginOtpResponse;
import com.rdam.dto.LoginRequest;
import com.rdam.dto.LoginResponse;
import com.rdam.dto.RefreshTokenRequest;
import com.rdam.dto.RegisterRequest;
import com.rdam.dto.VerifyOtpRequest;
import com.rdam.service.AuthService;
import com.rdam.service.EmailService;
import com.rdam.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticacion", description = "Endpoints de login, verificacion OTP y renovacion de tokens")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final OtpService otpService;
    private final EmailService emailService;

    public AuthController(AuthService authService,
                          OtpService otpService,
                          EmailService emailService) {
        this.authService = authService;
        this.otpService = otpService;
        this.emailService = emailService;
    }

    @Operation(summary = "Registrar nuevo ciudadano")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ciudadano registrado exitosamente"),
            @ApiResponse(responseCode = "409", description = "Email o CUIL ya registrado"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos")
    })
    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.registrarCiudadano(request);
        return ResponseEntity.status(201).build();
    }

    @Operation(summary = "Iniciar sesion con CUIL o legajo — envia OTP al email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OTP enviado al email registrado"),
            @ApiResponse(responseCode = "400", description = "Datos de login invalidos"),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas"),
            @ApiResponse(responseCode = "403", description = "Cuenta bloqueada por intentos fallidos"),
            @ApiResponse(responseCode = "429", description = "Demasiados intentos — rate limit")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginOtpResponse> login(@Valid @RequestBody LoginRequest request) {
        String email = authService.authenticate(request.username(), request.password());
        String otp = otpService.generarOtp(email);
        emailService.enviarOtp(email, otp);
        return ResponseEntity.ok(new LoginOtpResponse("OTP enviado al email registrado", email));
    }

    @Operation(summary = "Verificar codigo OTP y obtener tokens JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticacion exitosa — tokens generados"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "401", description = "OTP incorrecto o expirado")
    })
    @PostMapping("/verify")
    public ResponseEntity<AuthResponse> verify(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.completarLoginConOtp(request.email(), request.codigo()));
    }

    @Operation(summary = "Renovar access token mediante refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nuevo access token generado"),
            @ApiResponse(responseCode = "401", description = "Refresh token invalido o expirado")
    })
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.renovarToken(request.refreshToken()));
    }
}
