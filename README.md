# RDAM Backend — Sistema de Certificados Digitales

**Alumno:** Alan Acevedo | Campus 2026 | i2T Software Factory
**Evaluadores:** Beliz, Meyer, Gatti

Sistema backend monolitico para la gestion integral de certificados digitales.
Implementa autenticacion dual (CUIL + Legajo) con verificacion OTP por email,
maquina de estados finitos (FSM) para solicitudes, integracion con gateway
de pagos PlusPagos, emision de certificados PDF y control de acceso basado
en roles (RBAC) con Row-Level Security (RLS) en PostgreSQL.

---

## Inicio rapido para el evaluador

### 1. Obtener App Password de Gmail

El sistema envia codigos OTP por email. Para que funcione se necesita un App Password de Gmail:

1. Ir a [myaccount.google.com](https://myaccount.google.com) -> Seguridad
2. Activar la verificacion en dos pasos (si no esta activada)
3. Ir a **Contrasenas de aplicaciones** (buscar "App Passwords")
4. Crear una nueva con nombre "RDAM" -> copiar la clave de 16 caracteres

### 2. Configurar variables de entorno

```bash
cp .env.example .env
```

Editar `.env` y completar estas 4 variables obligatorias:

```
DB_PASSWORD=tu_password_para_postgres
MAIL_USERNAME=tu_email@gmail.com
MAIL_PASSWORD=xxxx xxxx xxxx xxxx    # App Password de Gmail (paso anterior)
JWT_SECRET=clave-secreta-minimo-32-caracteres-para-hmac-sha256
```

### 3. Levantar el sistema

```bash
docker compose up --build
```

Esperar a que el healthcheck confirme que la app esta lista (~40 segundos).

### 4. Insertar usuario de prueba

Conectarse a la base de datos y ejecutar:

```sql
-- Conectar: docker exec -it rdam-backend-db-1 psql -U rdam_app_user -d rdam_prod

-- Ciudadano de prueba
INSERT INTO usuarios (cuil, nombre, apellido, email, password_hash, activo, created_at, updated_at)
VALUES (
  '20123456789',
  'Test',
  'Ciudadano',
  'TU_EMAIL@gmail.com',          -- REEMPLAZAR con tu email real
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',  -- password: rdam1234
  true,
  NOW(),
  NOW()
);

-- Empleado interno de prueba
INSERT INTO empleados (legajo, nombre, apellido, email, rol, circunscripcion_id, activo, created_at, updated_at)
VALUES (
  'LEG001',
  'Test',
  'Operador',
  'TU_EMAIL@gmail.com',          -- REEMPLAZAR con tu email real
  'ROLE_INTERNO',
  1,
  true,
  NOW(),
  NOW()
);
```

### 5. Probar con Postman

1. Importar los archivos de la carpeta `postman/`:
   - `RDAM_Collection.json` — 24 test cases con scripts automaticos
   - `RDAM_Environment.json` — variables de entorno
2. Seguir la guia paso a paso en `postman/LEEME_PRIMERO.md`

### Verificar que esta corriendo

```bash
curl http://localhost:8080/actuator/health
# Swagger UI: http://localhost:8080/swagger-ui.html
```

---

## Stack Tecnologico

| Componente          | Tecnologia                          |
|---------------------|-------------------------------------|
| Lenguaje            | Java 17                             |
| Framework           | Spring Boot 4.0.3                   |
| Seguridad           | Spring Security 6 + JWT stateless   |
| Persistencia        | Spring Data JPA + Hibernate         |
| Base de datos       | PostgreSQL 15                       |
| Generacion PDF      | OpenPDF 2.0.3 (LGPL/MPL)           |
| Rate Limiting       | Bucket4j 8.10.1                     |
| Email               | Spring Boot Starter Mail (JavaMailSender) |
| Documentacion API   | SpringDoc OpenAPI (Swagger UI)      |
| Monitoreo           | Spring Boot Actuator                |
| Autenticacion JWT   | JJWT 0.12.6                         |
| Contenedores        | Docker + Docker Compose             |

---

## Arquitectura

```
                    +-------------------+
                    |    Controllers    |  <-- Solo HTTP, cero logica
                    +-------------------+
                            |
                    +-------------------+
                    |     Services      |  <-- Logica de negocio, FSM, RLS
                    +-------------------+
                            |
                    +-------------------+
                    |   Repositories    |  <-- Spring Data JPA
                    +-------------------+
                            |
                    +-------------------+
                    |   PostgreSQL 15   |  <-- DDL, triggers, RLS, funciones
                    +-------------------+
```

**Modulos principales:**

- `controller/` — REST endpoints con RBAC via `@PreAuthorize`
- `service/` — Logica de negocio, validacion FSM, bloqueo pesimista
- `repository/` — Interfaces JPA con queries nativas para locking
- `security/` — Autenticacion dual, JWT filter, rate limiting, OTP
- `config/` — Beans, exception handler, async, security config
- `domain/entity/` — Entidades JPA mapeadas al DDL oficial
- `dto/` — Records Java 17 con validacion `@Valid`

---

## Seguridad

### Autenticacion Dual

| Tipo       | Identificador | Validacion        | Rol resultante        |
|------------|---------------|-------------------|-----------------------|
| Ciudadano  | CUIL          | BCrypt en PostgreSQL | ROLE_CIUDADANO      |
| Empleado   | Legajo        | Mock LDAP         | ROLE_INTERNO / ROLE_ADMIN |

### Verificacion OTP por Email

El login es un proceso de dos pasos:

1. **POST /auth/login** — Valida credenciales. Si son correctas, genera un codigo OTP de 6 digitos y lo envia al email registrado del usuario. No retorna JWT.
2. **POST /auth/verify** — Recibe email + codigo OTP. Si es correcto y no ha expirado (5 min), retorna el JWT.

### JWT Stateless

- Access Token: 1 hora de vigencia
- Refresh Token: 7 dias, con claim `token_type=refresh`
- El endpoint `/auth/refresh` valida que el usuario siga activo en DB antes de emitir un nuevo access token

### Proteccion Anti-Abuso

- **Bloqueo por intentos fallidos:** 5 intentos -> bloqueo temporal 15 minutos (ConcurrentHashMap en memoria)
- **Rate Limiting:** 10 requests/minuto/IP en `/auth/login` (Bucket4j). Retorna HTTP 429.

---

## Maquina de Estados (FSM)

```
PENDIENTE_REVISION --> EN_REVISION --> APROBADA --> PAGADA --> EMITIDA
                            |
                            +--> RECHAZADA
```

Las transiciones son validadas por el trigger `trg_solicitudes_state_machine` en PostgreSQL.
Cada cambio de estado se registra automaticamente en la tabla `historial_estados`.

---

## Integracion de Pagos — PlusPagos

| Endpoint                              | Descripcion                        |
|---------------------------------------|------------------------------------|
| GET /api/solicitudes/{id}/pago-datos  | Datos encriptados para el gateway  |
| POST /api/pagos/webhook              | Webhook de notificacion (publico)  |

- Encriptacion AES-256-CBC compatible con el mock Node.js
- Webhook idempotente: re-envios duplicados retornan 200 sin duplicar registros
- Validacion de monto, estado y TransaccionComercioId

---

## Certificados PDF

Generados con OpenPDF (fork open-source de iText). Incluyen:

- Titulo oficial del sistema
- Datos de la solicitud (tipo, circunscripcion, ciudadano, CUIL)
- Fecha de emision
- Codigo de verificacion UUID
- Leyenda de certificado electronico

---

## Endpoints Principales

### Autenticacion

| Metodo | Endpoint        | Acceso  | Descripcion                     |
|--------|-----------------|---------|----------------------------------|
| POST   | /auth/login     | Publico | Paso 1: credenciales -> OTP     |
| POST   | /auth/verify    | Publico | Paso 2: OTP -> JWT              |
| POST   | /auth/refresh   | Publico | Renovar access token            |

### Solicitudes — Ciudadano

| Metodo | Endpoint                              | Rol        | Descripcion                |
|--------|---------------------------------------|------------|----------------------------|
| POST   | /api/solicitudes                      | CIUDADANO  | Crear solicitud            |
| GET    | /api/solicitudes/mis-solicitudes      | CIUDADANO  | Listar propias (paginado)  |
| GET    | /api/solicitudes/{id}                 | CIUDADANO  | Ver solicitud propia       |
| GET    | /api/solicitudes/{id}/historial       | CIUDADANO  | Ver historial de estados   |
| GET    | /api/solicitudes/{id}/pago-datos      | CIUDADANO  | Datos para PlusPagos       |
| GET    | /api/solicitudes/{id}/certificado/pdf | CIUDADANO  | Descargar PDF              |

### Solicitudes — Operador

| Metodo | Endpoint                              | Rol             | Descripcion          |
|--------|---------------------------------------|-----------------|----------------------|
| GET    | /api/solicitudes/bandeja              | INTERNO/ADMIN   | Bandeja (paginado)   |
| POST   | /api/solicitudes/{id}/tomar           | INTERNO/ADMIN   | Tomar para revision  |
| POST   | /api/solicitudes/{id}/aprobar         | INTERNO/ADMIN   | Aprobar              |
| POST   | /api/solicitudes/{id}/rechazar        | INTERNO/ADMIN   | Rechazar con motivo  |
| POST   | /api/solicitudes/{id}/emitir          | INTERNO/ADMIN   | Emitir certificado   |

### Webhook

| Metodo | Endpoint              | Acceso  | Descripcion                |
|--------|-----------------------|---------|----------------------------|
| POST   | /api/pagos/webhook    | Publico | Webhook PlusPagos          |

---

## Estructura del Proyecto

```
src/main/java/com/rdam/
  config/              # AsyncConfig, GlobalExceptionHandler, SecurityConfig
  controller/          # AuthController, SolicitudController, WebhookPagoController
  domain/entity/       # Usuario, Empleado, Solicitud, Pago, Certificado, ...
  dto/                 # LoginRequest, SolicitudResponse, WebhookPagoRequest, ...
  repository/          # JpaRepository interfaces
  security/
    filter/            # JwtAuthenticationFilter, RateLimitFilter
    jwt/               # JwtService
    provider/          # CiudadanoAuthProvider, EmpleadoAuthProvider
    service/           # CustomUserDetails, CustomUserDetailsService
  service/
    event/             # PagoAprobadoEvent, CertificadoEmitidoEvent
    exception/         # AccesoDenegadoException, EstadoInvalidoException, ...
    impl/              # SolicitudServiceImpl, PagoServiceImpl, CertificadoServiceImpl
```

---

## Uso de Inteligencia Artificial y Prompt Engineering

Este proyecto fue desarrollado utilizando Inteligencia Artificial como **Pair Programmer**
bajo un marco de reglas inmutables. La IA no opero de forma autonoma, sino que fue guiada
mediante **Prompt Engineering estructurado** con restricciones arquitectonicas estrictas.

### Reglas inmutables aplicadas a la IA

1. **CERO logica de negocio en controllers:** Los controllers solo manejan HTTP. Toda
   validacion, transicion de estados y regla de dominio vive en la capa de servicios.

2. **RLS como autoridad:** Row-Level Security de PostgreSQL es la ultima linea de defensa.
   Antes de cada operacion de escritura, el servicio establece el contexto de sesion RLS
   via `rdam_set_session_user()`. La IA no pudo eludir esta regla.

3. **FSM estricta:** La maquina de estados es propiedad de la base de datos
   (trigger `trg_solicitudes_state_machine`). La IA no invento estados ni permitio
   transiciones ilegales. Cada transicion fue validada tanto en Java como en PostgreSQL.

4. **DDL inmutable:** El DDL oficial no fue modificado bajo ninguna circunstancia.
   No se inventaron tablas, columnas ni estados.

5. **No Lombok salvo indicacion explicita:** Las entidades usan getters/setters manuales.
   Los DTOs usan records de Java 17.

### Aplicaciones de la IA en el proyecto

| Area                               | Uso de IA                                                |
|------------------------------------|----------------------------------------------------------|
| Arquitectura de capas              | Diseno de la separacion controller/service/repository    |
| Autenticacion dual                 | Implementacion de multiples AuthenticationProvider       |
| OTP por email                      | Generacion de la arquitectura del servicio OTP con       |
|                                    | JavaMailSender, almacenamiento en memoria y expiracion   |
| Integracion PlusPagos              | Encriptacion AES-256-CBC interoperable con Node.js       |
| Generacion de PDF                  | Construccion del documento con OpenPDF (tablas, fuentes) |
| Plan de pruebas                    | Diseno de 30 casos de prueba con cobertura E2E           |
| Manejo de concurrencia             | Bloqueo pesimista + idempotencia en webhooks             |
| Docker                             | Dockerfile multietapa + docker-compose con healthchecks  |

### Metodologia de Prompt Engineering

- **Prompts de contexto:** Cada interaccion incluyo un archivo de reglas como contexto
  inmutable, asegurando que la IA respetara las reglas arquitectonicas.
- **Micro-observaciones de trinchera:** Se aplicaron correcciones criticas basadas en
  experiencia practica (ej: `entityManager.refresh()` para leer valores generados por la DB
  antes de publicar eventos).
- **Cheat Sheets por fase:** Cada fase de desarrollo se documento con un documento
  detallado de especificaciones que la IA utilizo como blueprint.

Los prompts utilizados durante el desarrollo se documentan en `prompts_usados.md`.

---

## Testing

El plan de pruebas completo se encuentra en `plan_pruebas_backend.md`.
La coleccion de Postman importable esta en `postman/RDAM_Collection.json`.

Para instrucciones detalladas de como ejecutar las pruebas, ver `postman/LEEME_PRIMERO.md`.

---

## Licencia

Proyecto academico — i2T Software Factory, Campus 2026.
