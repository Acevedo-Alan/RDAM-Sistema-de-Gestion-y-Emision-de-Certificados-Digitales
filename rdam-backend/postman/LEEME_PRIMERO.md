# Guia de Pruebas con Postman — RDAM Backend

**Alumno:** Alan Acevedo | Campus 2026 | i2T Software Factory
**Evaluadores:** Beliz, Meyer, Gatti

---

## Prerequisitos

1. El sistema debe estar corriendo (`docker compose up --build`)
2. Postman v10+ instalado
3. Usuario de prueba insertado en la base de datos (ver README.md)

---

## Paso 1: Importar en Postman

1. Abrir Postman
2. File -> Import
3. Seleccionar los dos archivos de esta carpeta:
   - `RDAM_Collection.json` — coleccion con 24 test cases
   - `RDAM_Environment.json` — variables de entorno
4. En la esquina superior derecha de Postman, seleccionar el environment **"RDAM Backend — Local"**

---

## Paso 2: Login ciudadano (TC-01)

1. Abrir la request **TC-01 — Login ciudadano (envia OTP)**
2. Click en **Send**
3. Respuesta esperada: `200 OK` con `{"message": "Codigo de verificacion enviado al email registrado"}`
4. **Revisar tu casilla de Gmail** — llegara un email con el codigo OTP de 6 digitos

---

## Paso 3: Verificar OTP (TC-01b)

1. Abrir la request **TC-01b — Verificar OTP ciudadano (obtiene JWT)**
2. En el body, reemplazar:
   - `TU_EMAIL@gmail.com` por tu email real
   - `REEMPLAZAR_CON_CODIGO_OTP` por el codigo de 6 digitos recibido
3. Click en **Send**
4. Respuesta esperada: `200 OK` con `{"token": "eyJ..."}`
5. **El script de Tests captura automaticamente el JWT** en la variable `token_ciudadano`

---

## Paso 4: Login empleado interno (TC-02 + TC-02b)

1. Ejecutar **TC-02** — envia OTP al email del empleado
2. Copiar el OTP del email
3. Ejecutar **TC-02b** — reemplazar email y codigo en el body
4. El JWT del interno se captura automaticamente en `token_interno`

---

## Paso 5: Ejecutar el resto de los tests

Una vez obtenidos ambos JWT, los demas tests funcionan automaticamente porque:

- Los headers `Authorization: Bearer {{token_ciudadano}}` y `Bearer {{token_interno}}` usan las variables capturadas
- El `solicitud_id` se captura automaticamente en TC-07
- El `solicitud_id_rechazo` se captura automaticamente en TC-12

**Ejecutar en orden secuencial:** TC-07 -> TC-08 -> TC-09 -> ... -> TC-24

---

## Flujo E2E completo

```
TC-01  Login ciudadano         -> OTP enviado
TC-01b Verify OTP              -> JWT ciudadano capturado
TC-02  Login interno           -> OTP enviado
TC-02b Verify OTP              -> JWT interno capturado
TC-03  Login invalido          -> 401
TC-04  Sin token               -> 401
TC-05  Token malformado        -> 401
TC-06  OTP incorrecto          -> 401
TC-07  Crear solicitud         -> 201 PENDIENTE_REVISION (captura solicitud_id)
TC-08  Pago en estado invalido -> 422
TC-09  Ciudadano en endpoint interno -> 403
TC-10  Tomar solicitud         -> 200 EN_REVISION
TC-11  Aprobar solicitud       -> 200 APROBADA
TC-12  Crear solicitud rechazo -> 201 (captura solicitud_id_rechazo)
TC-13  Tomar para rechazo      -> 200
TC-14  Rechazar solicitud      -> 200 RECHAZADA
TC-15  Transicion invalida FSM -> 422
TC-16  Datos de pago           -> 200
TC-17  Pagar solicitud ajena   -> 403/404
TC-18  Pagar RECHAZADA         -> 422
TC-19  Webhook REALIZADA       -> 200 PAGADA
TC-20  Webhook duplicado       -> 200 (idempotente)
TC-21  Webhook incompleto      -> 400
TC-22  Webhook RECHAZADO       -> 200 (ignorado)
TC-23  Emitir certificado      -> 200 EMITIDA
TC-24  Emitir duplicado        -> 422
```

---

## Notas importantes

- **Los TC-01b y TC-02b requieren intervencion manual** (copiar el OTP del email)
- El resto de la coleccion es 100% automatico gracias a los scripts de Tests
- Si el JWT expira (1 hora), repetir TC-01/TC-01b para obtener uno nuevo
- El webhook (TC-19) usa `Estado: "REALIZADA"`, NO "APROBADO"
