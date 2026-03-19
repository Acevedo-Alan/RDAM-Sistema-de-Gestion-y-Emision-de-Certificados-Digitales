# Prompts Utilizados — Asistencia de IA en el Desarrollo

**Alumno:** Alan Acevedo
**Programa:** Campus 2026 — i2T Software Factory
**Fase:** 2 — Backend (Spring Boot)
**Herramienta de IA:** Claude (Anthropic)

---

## Metodología: "Promptear primero, codear después"

El desarrollo del backend RDAM siguió un enfoque iterativo asistido por IA donde cada módulo técnico se abordó en tres etapas:

1. **Definición del problema:** se le planteaba a la IA el requerimiento concreto extraído de la consigna académica, incluyendo restricciones explícitas (no Lombok, no microservicios, DDL inmutable).
2. **Generación asistida:** la IA proponía una implementación base que respetara las convenciones de Spring Boot 3 y las reglas del proyecto.
3. **Ajuste manual:** se revisaba el código generado, se adaptaba al DDL real, se corregían nombres de columnas/enums, y se integraba con el resto del sistema.

Este flujo permitió acelerar la escritura de boilerplate (filtros, providers, configuraciones de seguridad) sin delegar decisiones arquitectónicas críticas, que fueron tomadas en base a la consigna y al modelado relacional previamente definido.

---

## Prompts Representativos

### 1. Autenticación Dual — AuthenticationProvider

**Prompt / Intención:**
> "Necesito implementar autenticación dual en Spring Security 6. Los ciudadanos se autentican con CUIL y password (BCrypt, validado contra PostgreSQL). Los empleados se autentican con Legajo y se validan contra un mock LDAP. Cada tipo de usuario tiene su propio AuthenticationProvider. No usar OAuth ni Keycloak."

**Qué generó:**
Dos clases que implementan `AuthenticationProvider`: una para ciudadanos que consulta la tabla `usuarios` por CUIL y compara el hash BCrypt, y otra para empleados que consulta por legajo y simula validación LDAP.

**Ajuste manual:**
- Se agregó un `.trim()` al comparar el password hash porque el campo en PostgreSQL es `CHAR(255)` y viene con padding de espacios.
- Se añadió `@Transactional(readOnly = true)` en el provider de empleados para evitar `LazyInitializationException` al acceder a la relación `Empleado → Usuario`.

**Fragmento resultante** (`CiudadanoAuthProvider.java`):
```java
@Override
public Authentication authenticate(Authentication authentication)
        throws AuthenticationException {
    String cuil = authentication.getName();
    String password = authentication.getCredentials().toString();

    loginAttemptService.verificarBloqueo(cuil);

    Usuario usuario = usuarioRepository.findByCuilAndActivoTrue(cuil)
            .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

    String storedHash = usuario.getPasswordHash().trim();
    if (!passwordEncoder.matches(password, storedHash)) {
        loginAttemptService.registrarFallo(cuil);
        throw new BadCredentialsException("Credenciales inválidas");
    }

    loginAttemptService.registrarExito(cuil);
    CustomUserDetails userDetails = CustomUserDetails.fromCiudadano(usuario);
    return new UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.getAuthorities());
}
```

---

### 2. Protección contra Fuerza Bruta — LoginAttemptService

**Prompt / Intención:**
> "Implementar bloqueo por intentos fallidos de login. Debe ser en memoria con ConcurrentHashMap, sin modificar el DDL. Máximo 5 intentos, bloqueo temporal de 15 minutos. Si el login es exitoso, limpiar el registro."

**Qué generó:**
Un `@Service` con un `ConcurrentHashMap<String, LoginAttempt>` que registra fallos por CUIL, verifica bloqueo antes de cada intento, y limpia registros en login exitoso.

**Ajuste manual:**
- Se integró directamente dentro del `CiudadanoAuthProvider` en lugar de ser un filtro separado, ya que el bloqueo es por CUIL (no por IP).
- Se lanza `LockedException` (de Spring Security) para que el `GlobalExceptionHandler` lo mapee a HTTP 401 con mensaje descriptivo.

**Fragmento resultante** (`LoginAttemptService.java`):
```java
public void verificarBloqueo(String cuil) {
    LoginAttempt attempt = intentos.get(cuil);
    if (attempt != null && attempt.isBloqueado()) {
        if (attempt.bloqueoExpirado(DURACION_BLOQUEO)) {
            intentos.remove(cuil);
        } else {
            throw new LockedException(
                "Cuenta bloqueada temporalmente por múltiples intentos fallidos");
        }
    }
}

public void registrarFallo(String cuil) {
    intentos.compute(cuil, (key, existing) -> {
        if (existing == null) return new LoginAttempt();
        existing.incrementar(MAX_INTENTOS);
        return existing;
    });
}
```

---

### 3. Rate Limiting por IP — RateLimitFilter

**Prompt / Intención:**
> "Agregar rate limiting en endpoints sensibles usando Bucket4j. Login: 10 req/min por IP. Registro: 5 req/min. Webhook de pagos: 100 req/min. Solicitudes POST: 20 req/min por userId extraído del JWT. Responder 429 si se excede."

**Qué generó:**
Un filtro `OncePerRequestFilter` que mapea reglas de rate limiting por endpoint, resuelve el identificador (IP o userId), y mantiene buckets en un `ConcurrentHashMap` con limpieza periódica.

**Ajuste manual:**
- Se configuró el orden del filtro para que se ejecute *antes* del `JwtAuthenticationFilter`, de modo que las solicitudes abusivas se rechacen sin gastar recursos en validación de token.
- Se añadió limpieza programada cada 5 minutos para evitar memory leak por acumulación de buckets de IPs únicas.

**Fragmento resultante** (`RateLimitFilter.java`):
```java
@Override
protected void doFilterInternal(HttpServletRequest request,
        HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

    RateLimitRule rule = resolverRegla(request);
    if (rule == null) {
        filterChain.doFilter(request, response);
        return;
    }

    String identifier = resolverIdentificador(request, rule);
    Bucket bucket = buckets.computeIfAbsent(identifier,
            k -> crearBucket(rule));

    if (bucket.tryConsume(1)) {
        filterChain.doFilter(request, response);
    } else {
        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write(
            "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded\"}");
    }
}
```

---

### 4. Máquina de Estados (FSM) — Transiciones de Solicitud

**Prompt / Intención:**
> "La solicitud tiene estos estados: PENDIENTE_REVISION → EN_REVISION → APROBADA → PAGADA → EMITIDA. Alternativa: EN_REVISION → RECHAZADA. No permitir saltos ilegales. El estado final (EMITIDA, RECHAZADA) no permite modificaciones. Registrar cada cambio en historial_estados."

**Qué generó:**
Un enum `EstadoSolicitud` con un mapa de transiciones válidas, y métodos en `SolicitudServiceImpl` que validan cada transición antes de persistirla. El historial se registra automáticamente vía trigger en PostgreSQL.

**Ajuste manual:**
- La validación de FSM se mantuvo *también* a nivel de base de datos con el trigger `trg_solicitudes_state_machine`, creando una doble barrera. El servicio valida antes de intentar el UPDATE; si algo se filtra, el trigger lo rechaza.
- Se añadió `CANCELADA` como transición válida desde `PENDIENTE_REVISION` (requerimiento adicional para ciudadanos).
- Se usó `@Lock(PESSIMISTIC_WRITE)` en la query del repositorio para evitar condiciones de carrera entre dos empleados tomando la misma solicitud.

**Fragmento resultante** (`SolicitudServiceImpl.java` — transición aprobar):
```java
@Override
@Transactional
public SolicitudResponse aprobarSolicitud(Integer solicitudId,
        Integer empleadoId, Integer circunscripcionId) {

    Solicitud solicitud = solicitudRepository.findByIdForUpdate(solicitudId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada"));

    validarAccesoEmpleado(solicitud, circunscripcionId);

    if (solicitud.getEstado() != EstadoSolicitud.EN_REVISION) {
        throw new EstadoInvalidoException(
            "Solo se pueden aprobar solicitudes EN_REVISION");
    }

    solicitud.setEstado(EstadoSolicitud.APROBADA);
    solicitud.setFechaAprobacion(LocalDateTime.now());
    solicitudRepository.save(solicitud);

    eventPublisher.publishEvent(new SolicitudAprobadaEvent(this, solicitud));
    return mapToResponse(solicitud);
}
```

---

### 5. Webhook de Pagos — Integración PlusPagos

**Prompt / Intención:**
> "Implementar webhook para recibir notificaciones de pago de PlusPagos (mock). Debe verificar TransaccionComercioId, validar el monto contra la solicitud, verificar que no esté ya PAGADA, y cambiar estado a PAGADA si corresponde. El webhook debe ser idempotente."

**Qué generó:**
Un servicio que parsea el webhook, extrae el ID de solicitud del formato `SOL-{id}`, valida montos, y persiste el pago con estado `APROBADO`. El controller expone el endpoint sin autenticación (como requiere un webhook externo).

**Ajuste manual:**
- Se agregó encriptación AES-256-CBC del payload de ida (datos que se envían al gateway) usando `PlusPagosCryptoService`, ya que el mock de PlusPagos lo requería.
- La idempotencia se garantiza con la constraint `UNIQUE` en `pagos.solicitud_id` a nivel de base de datos, más una verificación previa en el servicio.
- Se añadió MDC (Mapped Diagnostic Context) en el controller para trazabilidad de cada webhook recibido.

**Fragmento resultante** (`PagoServiceImpl.java` — procesamiento webhook):
```java
@Override
@Transactional
public void procesarWebhookPago(WebhookPagoRequest request) {
    String transaccionId = request.getTransaccionComercioId();
    Integer solicitudId = extraerSolicitudId(transaccionId);

    Solicitud solicitud = solicitudRepository.findByIdForUpdate(solicitudId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada"));

    if (solicitud.getEstado() == EstadoSolicitud.PAGADA
            || solicitud.getEstado() == EstadoSolicitud.EMITIDA) {
        return; // Idempotencia: ya fue procesado
    }

    validarEstadoParaPago(solicitud);
    validarMonto(request, solicitud);

    solicitud.setEstado(EstadoSolicitud.PAGADA);
    solicitud.setFechaPago(LocalDateTime.now());
    solicitudRepository.save(solicitud);

    crearRegistroPago(solicitud, request);
    eventPublisher.publishEvent(new PagoAprobadoEvent(this, solicitud));
}
```

---

### 6. Generación de JWT — JwtService

**Prompt / Intención:**
> "Crear un servicio JWT stateless con JJWT. Debe generar access tokens (1 hora) y refresh tokens (7 días). Los claims deben incluir username, rol, userId y circunscripcion_id. El refresh token debe diferenciarse del access token para evitar uso cruzado."

**Qué generó:**
Un `JwtService` con métodos para generar ambos tipos de token, extraer claims, y validar expiración y firma. Cada token incluye un claim `token_type` para distinguirlos.

**Ajuste manual:**
- Se agregó el claim `circunscripcion_id` para empleados (necesario para filtrar solicitudes por circunscripción sin hacer query adicional).
- En `AuthService.renovarToken()` se validó explícitamente que el token recibido sea de tipo `refresh` antes de generar uno nuevo, evitando que un access token compromometido se use para renovación.

**Fragmento resultante** (`JwtService.java`):
```java
public String generateAccessToken(CustomUserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("rol", userDetails.getRol());
    claims.put("userId", userDetails.getUserId());
    claims.put("token_type", "access");
    if (userDetails.getCircunscripcionId() != null) {
        claims.put("circunscripcion_id", userDetails.getCircunscripcionId());
    }
    return Jwts.builder()
            .claims(claims)
            .subject(userDetails.getUsername())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + accessExpiration))
            .signWith(getSigningKey())
            .compact();
}
```

---

### 7. Emisión de Certificado con PDF — CertificadoService

**Prompt / Intención:**
> "Implementar emisión de certificado digital. Solo se puede emitir si el estado es PAGADA. Generar un PDF en memoria con OpenPDF que incluya datos de la solicitud, ciudadano, tipo de certificado y un código de verificación. Calcular hash SHA-256 del PDF. No permitir doble emisión."

**Qué generó:**
Un servicio que valida el estado, genera el PDF usando la API de OpenPDF (`Document`, `PdfWriter`), calcula el hash, y persiste la entidad `Certificado`. El `numero_certificado` lo genera una función de base de datos.

**Ajuste manual:**
- Se separó `emitirCertificado()` (transición de estado + persistencia) de `generarCertificadoPdf()` (generación del binario para descarga) porque son operaciones distintas: una es administrativa y la otra es de acceso del ciudadano.
- Se agregó control de acceso en la descarga: solo el ciudadano dueño de la solicitud puede descargar el PDF.
- Se usó `@JdbcTypeCode(SqlTypes.CHAR)` en el campo `hash_pdf` para un fix de compatibilidad con Hibernate 6.

**Fragmento resultante** (`CertificadoServiceImpl.java` — emisión):
```java
@Override
@Transactional
public CertificadoResponse emitirCertificado(Integer solicitudId,
        Integer empleadoId, Integer circunscripcionId) {

    Solicitud solicitud = solicitudRepository.findByIdForUpdate(solicitudId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada"));

    if (solicitud.getEstado() != EstadoSolicitud.PAGADA) {
        throw new EstadoInvalidoException("Solo se puede emitir si estado es PAGADA");
    }
    if (certificadoRepository.existsBySolicitudId(solicitudId)) {
        throw new EstadoInvalidoException("El certificado ya fue emitido");
    }

    solicitud.setEstado(EstadoSolicitud.EMITIDA);
    solicitud.setFechaEmision(LocalDateTime.now());
    solicitudRepository.save(solicitud);

    Certificado certificado = crearEntidadCertificado(solicitud, empleadoId);
    certificadoRepository.save(certificado);
    eventPublisher.publishEvent(new CertificadoEmitidoEvent(this, solicitud));
    return mapToResponse(certificado);
}
```

---

### 8. Manejo Global de Excepciones — GlobalExceptionHandler

**Prompt / Intención:**
> "Crear un @RestControllerAdvice que maneje todas las excepciones del sistema. Necesito excepciones custom para: estado inválido (422), acceso denegado (403), concurrencia (409), recurso no encontrado (404). También manejar BadCredentialsException, tokens expirados, y errores de validación."

**Qué generó:**
Un handler centralizado con métodos `@ExceptionHandler` para cada tipo de excepción, retornando `ErrorResponse` con status, error, mensaje y timestamp.

**Ajuste manual:**
- Se agregó un handler específico para `DataIntegrityViolationException` que analiza el mensaje de la constraint violada para determinar si es un 409 (conflicto de unicidad) o un 422 (violación de FSM por trigger).
- Se incluyó manejo de `LockedException` (generada por el bloqueo de fuerza bruta) mapeada a 401.

**Fragmento resultante** (`GlobalExceptionHandler.java`):
```java
@ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<ErrorResponse> handleDataIntegrity(
        DataIntegrityViolationException ex) {
    String message = ex.getMostSpecificCause().getMessage();

    if (message != null && message.contains("transicion_estado_invalida")) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY,
            "Transición de estado no permitida");
    }
    return buildResponse(HttpStatus.CONFLICT,
        "Conflicto de integridad de datos");
}
```

---

### 9. Eventos de Dominio — Notificaciones Asíncronas

**Prompt / Intención:**
> "Quiero enviar emails de notificación cuando se aprueba una solicitud, se confirma un pago, o se emite un certificado. Pero no quiero que un fallo de email haga rollback de la transacción. Usar eventos de dominio con listeners asíncronos que se ejecuten después del commit."

**Qué generó:**
Clases de eventos (`SolicitudAprobadaEvent`, `PagoAprobadoEvent`, `CertificadoEmitidoEvent`) y un `EmailService` que escucha con `@TransactionalEventListener(phase = AFTER_COMMIT)` y `@Async`.

**Ajuste manual:**
- Se configuró un `ThreadPoolTaskExecutor` dedicado para emails (`AsyncConfig`) en lugar de usar el pool por defecto, para no saturar los threads de la aplicación.
- Se agregaron templates HTML para cada tipo de email en lugar de texto plano.

**Fragmento resultante** (`EmailService.java`):
```java
@Async("emailTaskExecutor")
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onPagoAprobado(PagoAprobadoEvent event) {
    Solicitud solicitud = event.getSolicitud();
    Usuario ciudadano = solicitud.getCiudadano();

    String html = buildPagoAprobadoTemplate(solicitud, ciudadano);
    enviarEmail(ciudadano.getEmail(),
        "Pago confirmado - Solicitud #" + solicitud.getId(), html);
}
```

---

### 10. Testing — Integración con MockMvc

**Prompt / Intención:**
> "Crear tests de integración para el flujo de autenticación. Usar MockMvc con Spring Security configurado. Necesito testear: login exitoso genera OTP, credenciales inválidas devuelve 401, verificación OTP devuelve JWT válido. Usar @Sql para preparar datos de test."

**Qué generó:**
Una clase `BaseIntegrationTest` abstracta con `@AutoConfigureMockMvc` y perfil `test`, y tests concretos en `AuthIntegrationTest` que usan `@Sql` para setup/teardown y mockean `OtpService` y `EmailService`.

**Ajuste manual:**
- Se creó `JwtTestHelper` para generar tokens de test sin pasar por el flujo completo de autenticación, útil para tests de endpoints protegidos.
- Se separaron los tests unitarios (mocks puros) de los de integración (MockMvc + base de datos) en paquetes distintos.

**Fragmento resultante** (`AuthIntegrationTest.java`):
```java
@Test
void login_conCredencialesValidas_retornaOtpEnviado() throws Exception {
    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"20345678901\",\"password\":\"password123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").exists());

    verify(otpService).generarOtp(anyString());
    verify(emailService).enviarEmail(anyString(), anyString(), anyString());
}
```

---

## Reflexión Final

### Decisiones asistidas por IA

La IA fue especialmente útil en la **implementación de patrones conocidos de Spring Security**: la configuración del `SecurityFilterChain`, la estructura de `AuthenticationProvider`, el filtro JWT, y el manejo de excepciones con `@ControllerAdvice`. Estos son patrones con mucho boilerplate donde la IA acelera significativamente la escritura sin comprometer la calidad.

También fue valiosa en la generación de **código estructural repetitivo**: DTOs como records, mapeos entity-to-response, configuración de Bucket4j, y templates de tests con MockMvc.

### Decisiones tomadas manualmente

Las decisiones de **diseño arquitectónico** fueron humanas:

- **Doble validación de FSM** (aplicación + trigger PostgreSQL) fue una decisión deliberada para garantizar integridad incluso si el código Java tiene un bug.
- **Row-Level Security** mediante `rdam_set_session_user()` se definió en el DDL y se integró en el servicio como decisión de seguridad en profundidad.
- **Separación de emisión vs descarga de PDF** fue un ajuste de diseño que la IA no sugirió inicialmente.
- **El modelo de datos completo** (DDL, triggers, funciones) fue diseñado previamente y la IA debió adaptarse a él, no al revés.
- **La elección de pessimistic locking** sobre optimistic locking fue una decisión basada en el análisis del caso de uso (múltiples empleados compitiendo por tomar solicitudes).

En resumen: la IA escribió código; las decisiones de *qué* código escribir y *por qué* fueron del desarrollador.
