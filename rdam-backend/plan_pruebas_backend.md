# Plan de Pruebas Backend — RDAM
### Sistema de Certificados Digitales
**Alumno:** Alan Acevedo | Campus 2026
**Evaluadores:** Beliz, Meyer, Gatti
**i2T Software Factory**

---

## 1. Objetivo

Validar el correcto funcionamiento del backend RDAM verificando autenticacion JWT
con verificacion OTP por email, gestion del ciclo de vida de solicitudes (FSM),
flujo de pagos con webhook idempotente, emision de certificados PDF y control
de acceso por rol.

---

## 2. Alcance

| Componente                    | Incluido |
|-------------------------------|----------|
| Autenticacion JWT + OTP Email | SI       |
| Solicitudes (FSM)             | SI       |
| Pagos y webhook PlusPagos     | SI       |
| Certificados PDF (OpenPDF)    | SI       |
| Seguridad por roles (RBAC)    | SI       |
| Rate Limiting (Bucket4j)      | SI       |
| DDL / RLS en DB               | NO (cubierto por PostgreSQL) |

---

## 3. Entorno de pruebas

| Variable       | Valor                   |
|----------------|-------------------------|
| base_url       | http://localhost:8080   |
| Base de datos  | PostgreSQL 15 (Docker)  |
| Autenticacion  | JWT stateless + OTP     |
| Herramienta    | Postman v10+            |
| Java           | 17+                     |
| Framework      | Spring Boot 4.0.3       |

---

## 4. Roles del sistema

| Rol        | Descripcion                                             |
|------------|---------------------------------------------------------|
| CIUDADANO  | Crea solicitudes, paga y descarga certificados PDF      |
| INTERNO    | Revisa, aprueba, rechaza solicitudes y emite certificados |
| ADMIN      | Administracion general del sistema                       |

---

## 5. Maquina de estados (FSM)

```
PENDIENTE_REVISION
       |
       v
   EN_REVISION ------------> RECHAZADA
       |
       v
   APROBADA
       |
       v
    PAGADA  <---- Webhook PlusPagos
       |
       v
    EMITIDA
```

Reglas:
- La base de datos valida transiciones via trigger `trg_solicitudes_state_machine`.
- No se permiten saltos ilegales.
- EMITIDA y RECHAZADA son estados finales.
- El historial se registra automaticamente en `historial_estados`.

---

## 6. Flujo de autenticacion — OTP por Email

El login ahora es un proceso de dos pasos:

| Paso | Endpoint         | Descripcion                                          |
|------|------------------|------------------------------------------------------|
| 1    | POST /auth/login | Recibe credenciales (CUIL/legajo + password). Si son validas, genera un codigo OTP de 6 digitos y lo envia al email registrado del usuario. No retorna JWT. |
| 2    | POST /auth/verify| Recibe email + codigo OTP. Si el codigo es correcto y no ha expirado, retorna el JWT (accessToken + refreshToken). |

Reglas del OTP:
- Codigo de 6 digitos numerico.
- Expiracion: 5 minutos.
- Maximo 3 intentos fallidos antes de invalidar el codigo.
- Se envia via JavaMailSender (mock en desarrollo: MailHog en puerto 1025).

---

## 7. Casos de prueba

### 7.1 Autenticacion (OTP por Email)

---

**TC-01 — Login ciudadano (paso 1: envia OTP)**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | POST /auth/login                     |
| Precondicion       | Usuario ciudadano registrado en DB   |
| Request            | `{"username":"20304050607","password":"password123","tipo":"ciudadano"}` |
| Respuesta esperada | 200 OK                               |
| Body esperado      | `{"message":"Codigo de verificacion enviado al email registrado"}` |
| Validacion         | No retorna token. Se envia codigo al email del usuario. |

---

**TC-01b — Verificar OTP ciudadano (paso 2: obtiene JWT)**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | POST /auth/verify                    |
| Precondicion       | TC-01 ejecutado. Codigo OTP disponible. |
| Request            | `{"email":"ciudadano@test.com","codigo":"123456"}` |
| Respuesta esperada | 200 OK                               |
| Body esperado      | `{"accessToken":"<jwt>","refreshToken":"<jwt>","tipo":"ciudadano","username":"20304050607","rol":"ROLE_CIUDADANO","userId":1}` |
| Validacion         | Guardar accessToken en variable `token_ciudadano`. Guardar refreshToken en `refresh_token_ciudadano`. |

---

**TC-02 — Login interno (paso 1: envia OTP)**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | POST /auth/login                     |
| Precondicion       | Empleado con legajo registrado       |
| Request            | `{"username":""username":"operador@test.com"",           "password":"password123","tipo":"empleado"}` |
| Respuesta esperada | 200 OK                               |
| Body esperado      | `{"message":"Codigo de verificacion enviado al email registrado"}` |
| Validacion         | No retorna token. Se envia codigo al email del empleado. |

---

**TC-02b — Verificar OTP interno (paso 2: obtiene JWT)**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | POST /auth/verify                    |
| Precondicion       | TC-02 ejecutado. Codigo OTP disponible. |
| Request            | `{"email":"operador@test.com","codigo":"123456"}` |
| Respuesta esperada | 200 OK                               |
| Body esperado      | `{"accessToken":"<jwt>","refreshToken":"<jwt>","tipo":"empleado","username":"LEG001","rol":"ROLE_INTERNO","userId":2}` |
| Validacion         | Guardar accessToken en variable `token_interno`. |

---

**TC-03 — Login con credenciales invalidas**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | POST /auth/login                     |
| Precondicion       | Ninguna                              |
| Request            | `{"username":"99999999999","password":"wrong","tipo":"ciudadano"}` |
| Respuesta esperada | 401 Unauthorized                     |
| Validacion         | No se envia codigo OTP. Credenciales rechazadas. |

---

**TC-04 — Acceso sin token**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | GET /api/solicitudes/mis-solicitudes |
| Precondicion       | Ninguna. Sin header Authorization.   |
| Respuesta esperada | 401 Unauthorized                     |

---

**TC-05 — Acceso con token malformado**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | GET /api/solicitudes/mis-solicitudes |
| Precondicion       | Header: `Authorization: Bearer token.invalido.jwt` |
| Respuesta esperada | 401 Unauthorized                     |

---

**TC-05b — Verificar OTP con codigo incorrecto**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | POST /auth/verify                    |
| Precondicion       | TC-01 ejecutado.                     |
| Request            | `{"email":"ciudadano@test.com","codigo":"000000"}` |
| Respuesta esperada | 401 Unauthorized                     |
| Validacion         | Codigo incorrecto. No se emite JWT.  |

---

### 7.2 Solicitudes — Ciudadano

---

**TC-06 — Crear solicitud**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | POST /api/solicitudes                |
| Precondicion       | TC-01b ejecutado. Token ciudadano disponible. |
| Header             | `Authorization: Bearer {{token_ciudadano}}` |
| Request            | `{"tipoCertificadoId":1,"circunscripcionId":1}` |
| Respuesta esperada | 201 Created                          |
| Body esperado      | `{"id":1,"estado":"PENDIENTE_REVISION","tipoCertificado":"Nacimiento",...}` |
| Validacion         | id debe ser entero. estado = PENDIENTE_REVISION. Guardar id en `solicitud_id`. |

---

**TC-07 — Ver mis solicitudes paginadas**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | GET /api/solicitudes/mis-solicitudes?page=0&size=10 |
| Precondicion       | TC-06 ejecutado.                     |
| Header             | `Authorization: Bearer {{token_ciudadano}}` |
| Respuesta esperada | 200 OK                               |
| Body esperado      | `{"content":[...],"totalElements":1,"totalPages":1,"size":10,"number":0}` |
| Validacion         | content es array. totalElements >= 1. |

---

**TC-08 — Ver solicitud propia por ID**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | GET /api/solicitudes/{{solicitud_id}} |
| Precondicion       | TC-06 ejecutado.                     |
| Header             | `Authorization: Bearer {{token_ciudadano}}` |
| Respuesta esperada | 200 OK                               |
| Validacion         | id coincide con solicitud_id.        |

---

**TC-09 — Ver solicitud de otro ciudadano**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | GET /api/solicitudes/9999            |
| Precondicion       | ID 9999 pertenece a otro usuario o no existe. |
| Header             | `Authorization: Bearer {{token_ciudadano}}` |
| Respuesta esperada | 403 Forbidden                        |

---

**TC-10 — Ver historial de solicitud propia**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | GET /api/solicitudes/{{solicitud_id}}/historial |
| Precondicion       | TC-06 ejecutado.                     |
| Header             | `Authorization: Bearer {{token_ciudadano}}` |
| Respuesta esperada | 200 OK                               |
| Body esperado      | `[{"estadoAnterior":null,"estadoNuevo":"PENDIENTE_REVISION","fecha":"..."}]` |
| Validacion         | Array con al menos una transicion.   |

---

### 7.3 Solicitudes — Operador Interno

---

**TC-11 — Ver bandeja de solicitudes**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | GET /api/solicitudes/bandeja?page=0&size=10 |
| Precondicion       | TC-02b y TC-06 ejecutados.           |
| Header             | `Authorization: Bearer {{token_interno}}` |
| Respuesta esperada | 200 OK                               |
| Body esperado      | `{"content":[...],"totalElements":1}` |
| Validacion         | Incluye la solicitud creada en TC-06. |

---

**TC-12 — Tomar solicitud**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | POST /api/solicitudes/{{solicitud_id}}/tomar |
| Precondicion       | Solicitud en PENDIENTE_REVISION.     |
| Header             | `Authorization: Bearer {{token_interno}}` |
| Respuesta esperada | 200 OK                               |
| Validacion         | Estado cambia a EN_REVISION.         |

---

**TC-13 — Aprobar solicitud**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | POST /api/solicitudes/{{solicitud_id}}/aprobar |
| Precondicion       | Solicitud en EN_REVISION (TC-12 ejecutado). |
| Header             | `Authorization: Bearer {{token_interno}}` |
| Respuesta esperada | 200 OK                               |
| Validacion         | Estado cambia a APROBADA.            |

---

**TC-13b — Crear solicitud para rechazo**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | POST /api/solicitudes                |
| Precondicion       | TC-01b ejecutado.                    |
| Header             | `Authorization: Bearer {{token_ciudadano}}` |
| Request            | `{"tipoCertificadoId":1,"circunscripcionId":1}` |
| Respuesta esperada | 201 Created                          |
| Validacion         | estado = PENDIENTE_REVISION. Guardar id en `solicitud_id_rechazo`. |
| Nota               | Esta solicitud se crea exclusivamente para probar el flujo de rechazo sin romper la FSM de la solicitud principal. |

---

**TC-13c — Tomar solicitud para rechazo**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | POST /api/solicitudes/{{solicitud_id_rechazo}}/tomar |
| Precondicion       | TC-13b ejecutado. Solicitud en PENDIENTE_REVISION. |
| Header             | `Authorization: Bearer {{token_interno}}` |
| Respuesta esperada | 200 OK                               |
| Validacion         | Estado cambia a EN_REVISION.         |

---

**TC-14 — Rechazar solicitud**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | POST /api/solicitudes/{{solicitud_id_rechazo}}/rechazar |
| Precondicion       | TC-13c ejecutado. Solicitud en EN_REVISION. |
| Header             | `Authorization: Bearer {{token_interno}}` |
| Request            | `{"motivo":"Documentacion incompleta"}` |
| Respuesta esperada | 200 OK                               |
| Validacion         | Estado cambia a RECHAZADA. Usa variable `solicitud_id_rechazo`, NO `solicitud_id`. |

---

**TC-15 — Ciudadano intenta acceder a bandeja**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | GET /api/solicitudes/bandeja         |
| Precondicion       | TC-01b ejecutado.                    |
| Header             | `Authorization: Bearer {{token_ciudadano}}` |
| Respuesta esperada | 403 Forbidden                        |

---

**TC-16 — Tomar solicitud en estado invalido (FSM)**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | POST /api/solicitudes/{{solicitud_id}}/tomar |
| Precondicion       | Solicitud ya en APROBADA (TC-13 ejecutado). |
| Header             | `Authorization: Bearer {{token_interno}}` |
| Respuesta esperada | 409 Conflict o 422 Unprocessable Entity |
| Validacion         | La DB rechaza la transicion. Estado no cambia. |

---

### 7.4 Pagos

---

**TC-17 — Obtener datos de pago (solicitud aprobada)**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | GET /api/solicitudes/{{solicitud_id}}/pago-datos |
| Precondicion       | Solicitud en APROBADA (TC-13 ejecutado). |
| Header             | `Authorization: Bearer {{token_ciudadano}}` |
| Respuesta esperada | 200 OK                               |
| Body esperado      | `{"Comercio":"...","TransaccionComercioId":"SOL-X","Monto":"<encriptado>","urlPasarela":"..."}` |
| Validacion         | Campos Comercio, TransaccionComercioId, Monto y urlPasarela son strings no vacios. |

---

**TC-18 — Reintentar pago (segunda solicitud de datos)**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | GET /api/solicitudes/{{solicitud_id}}/pago-datos |
| Precondicion       | TC-17 ejecutado. Solicitud aun en APROBADA. |
| Header             | `Authorization: Bearer {{token_ciudadano}}` |
| Respuesta esperada | 200 OK                               |
| Validacion         | Se obtienen datos validos para reintentar el pago. |

---

**TC-19 — Pagar solicitud ajena (403)**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | GET /api/solicitudes/9999/pago-datos |
| Precondicion       | ID 9999 pertenece a otro ciudadano.  |
| Header             | `Authorization: Bearer {{token_ciudadano}}` |
| Respuesta esperada | 403 Forbidden o 404 Not Found        |

---

**TC-20 — Pagar solicitud en estado incorrecto**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | GET /api/solicitudes/{{solicitud_id_rechazo}}/pago-datos |
| Precondicion       | Solicitud en RECHAZADA (TC-14 ejecutado). |
| Header             | `Authorization: Bearer {{token_ciudadano}}` |
| Respuesta esperada | 422 Unprocessable Entity             |
| Validacion         | La FSM rechaza la operacion.         |

---

### 7.5 Webhook PlusPagos

---

**TC-21 — Webhook pago aprobado**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | POST /api/pagos/webhook              |
| Precondicion       | TC-17 ejecutado. Solicitud en APROBADA. |
| Header             | `Content-Type: application/json`     |
| Request            | `{"Tipo":"PAGO","TransaccionPlataformaId":"PP-TXN-001","TransaccionComercioId":"SOL-{{solicitud_id}}","Monto":"1500.00","EstadoId":"1","Estado":"APROBADO","FechaProcesamiento":"2026-03-05T10:30:00Z"}` |
| Respuesta esperada | 200 OK                               |
| Validacion post    | GET /api/solicitudes/{{solicitud_id}} debe mostrar estado PAGADA. |

---

**TC-22 — Webhook duplicado (idempotencia)**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | POST /api/pagos/webhook              |
| Precondicion       | TC-21 ya ejecutado.                  |
| Request            | Exactamente igual a TC-21.           |
| Respuesta esperada | 200 OK                               |
| Validacion         | Estado no cambia. No se registra segundo pago en DB. |

---

**TC-23 — Webhook con payload incompleto**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | POST /api/pagos/webhook              |
| Precondicion       | Ninguna.                             |
| Request            | `{"Tipo":"PAGO","TransaccionPlataformaId":"PP-TXN-999"}` (campos obligatorios faltantes) |
| Respuesta esperada | 400 Bad Request                      |
| Validacion         | @Valid rechaza campos @NotBlank faltantes. |

---

**TC-24 — Webhook pago rechazado por pasarela**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | POST /api/pagos/webhook              |
| Precondicion       | Solicitud en PAGADA (TC-21 ejecutado). |
| Request            | `{"Tipo":"PAGO","TransaccionPlataformaId":"PP-TXN-003","TransaccionComercioId":"SOL-{{solicitud_id}}","Monto":"1500.00","EstadoId":"2","Estado":"RECHAZADO","FechaProcesamiento":"2026-03-05T11:00:00Z"}` |
| Respuesta esperada | 200 OK                               |
| Validacion         | Estado permanece en PAGADA. No cambia a RECHAZADA. |

---

### 7.6 Certificados

---

**TC-25 — Emitir certificado (operador interno)**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | POST /api/solicitudes/{{solicitud_id}}/emitir |
| Precondicion       | TC-21 ejecutado. Solicitud en PAGADA. |
| Header             | `Authorization: Bearer {{token_interno}}` |
| Respuesta esperada | 200 OK                               |
| Validacion         | Estado cambia a EMITIDA. Certificado creado en DB con numero_certificado generado. |

---

**TC-26 — Descargar certificado PDF propio**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | GET /api/solicitudes/{{solicitud_id}}/certificado/pdf |
| Precondicion       | TC-25 ejecutado. Solicitud en EMITIDA. |
| Header             | `Authorization: Bearer {{token_ciudadano}}` |
| Respuesta esperada | 200 OK                               |
| Headers respuesta  | `Content-Type: application/pdf` / `Content-Disposition: attachment; filename="certificado-{solicitudId}.pdf"` |
| Validacion         | Body es binario PDF. Content-Length > 0. |

---

**TC-27 — Descargar certificado de otro usuario (403)**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | GET /api/solicitudes/9999/certificado/pdf |
| Precondicion       | ID 9999 pertenece a otro ciudadano.  |
| Header             | `Authorization: Bearer {{token_ciudadano}}` |
| Respuesta esperada | 403 Forbidden                        |

---

**TC-28 — Descargar certificado no emitido (estado invalido)**

| Campo              | Valor                                |
|--------------------|--------------------------------------|
| Endpoint           | GET /api/solicitudes/{{solicitud_id_rechazo}}/certificado/pdf |
| Precondicion       | Solicitud en RECHAZADA (TC-14 ejecutado). |
| Header             | `Authorization: Bearer {{token_ciudadano}}` |
| Respuesta esperada | 422 Unprocessable Entity             |
| Validacion         | Solo se permite descarga en estado PAGADA o EMITIDA. |

---

## 8. Flujo de prueba End-to-End

Ejecutar en orden en Postman (Run Collection) para cubrir el ciclo completo:

```
 #   | Caso  | Accion                                  | Resultado esperado
-----|-------|-----------------------------------------|----------------------------
 1   | TC-01 | Login ciudadano (paso 1)                | OTP enviado al email
 2   | TC-01b| Verificar OTP ciudadano (paso 2)        | JWT ciudadano obtenido
 3   | TC-02 | Login interno (paso 1)                  | OTP enviado al email
 4   | TC-02b| Verificar OTP interno (paso 2)          | JWT interno obtenido
 5   | TC-06 | Crear solicitud                         | Estado: PENDIENTE_REVISION
 6   | TC-12 | Tomar solicitud (operador)              | Estado: EN_REVISION
 7   | TC-13 | Aprobar solicitud (operador)            | Estado: APROBADA
 8   | TC-13b| Crear segunda solicitud (para rechazo)  | Estado: PENDIENTE_REVISION
 9   | TC-13c| Tomar segunda solicitud                 | Estado: EN_REVISION
10   | TC-14 | Rechazar segunda solicitud              | Estado: RECHAZADA
11   | TC-17 | Obtener datos de pago                   | Datos encriptados para PlusPagos
12   | TC-21 | Webhook confirma pago                   | Estado: PAGADA
13   | TC-22 | Webhook duplicado (idempotencia)        | Estado: PAGADA (sin cambio)
14   | TC-25 | Emitir certificado (operador)           | Estado: EMITIDA
15   | TC-26 | Descargar certificado PDF               | PDF binario descargado
```

---

## 9. Criterios de aceptacion

| Criterio                                                    | Obligatorio |
|-------------------------------------------------------------|-------------|
| Login de dos pasos (OTP por email) funciona correctamente   | SI          |
| Codigo OTP incorrecto es rechazado con 401                  | SI          |
| Todos los endpoints protegidos rechazan sin token           | SI          |
| La FSM rechaza transiciones invalidas (trigger PostgreSQL)  | SI          |
| El webhook es idempotente ante llamadas duplicadas          | SI          |
| El ciudadano no accede a recursos de terceros               | SI          |
| El PDF se descarga con Content-Type: application/pdf        | SI          |
| Los listados responden con estructura Page (paginacion)     | SI          |
| Rate Limiting retorna 429 tras exceder 10 req/min en login  | SI          |
| Rechazo usa solicitud independiente (no rompe flujo E2E)    | SI          |

---

## 10. Cobertura por modulo

| Modulo            | Casos totales | Happy path | Casos negativos |
|-------------------|---------------|------------|-----------------|
| Autenticacion OTP | 7             | 4          | 3               |
| Solicitudes       | 11            | 7          | 4               |
| Pagos             | 4             | 2          | 2               |
| Webhook           | 4             | 2          | 2               |
| Certificados      | 4             | 2          | 2               |
| **Total**         | **30**        | **17**     | **13**          |

---

## 11. Variables de entorno Postman

| Variable                  | Tipo        | Descripcion                          |
|---------------------------|-------------|--------------------------------------|
| base_url                  | Inicial     | http://localhost:8080                |
| token_ciudadano           | Dinamica    | JWT del ciudadano (TC-01b)           |
| token_interno             | Dinamica    | JWT del operador interno (TC-02b)    |
| refresh_token_ciudadano   | Dinamica    | Refresh token del ciudadano          |
| refresh_token_interno     | Dinamica    | Refresh token del interno            |
| solicitud_id              | Dinamica    | ID de la solicitud principal (TC-06) |
| solicitud_id_rechazo      | Dinamica    | ID de la solicitud para rechazo (TC-13b) |
| certificado_id            | Dinamica    | ID del certificado emitido (TC-25)   |

---

## 12. Notas tecnicas

- **Conflicto FSM resuelto:** TC-14 ya no intenta rechazar la solicitud aprobada en TC-13. Se crea una segunda solicitud (TC-13b) que atraviesa PENDIENTE_REVISION -> EN_REVISION -> RECHAZADA de forma independiente.
- **Webhook real:** El endpoint es `POST /api/pagos/webhook` (no `/api/webhook/pluspagos`). Los campos del payload usan PascalCase segun formato PlusPagos.
- **Idempotencia:** Si el webhook recibe una transaccion ya procesada (pago APROBADO existente), retorna HTTP 200 silenciosamente sin crear duplicados.
- **OTP en desarrollo:** En entorno local se usa MailHog (puerto 1025) para capturar emails. El codigo OTP se puede ver en la interfaz web de MailHog (http://localhost:8025).
