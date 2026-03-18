# Mock PlusPagos - Servicio de Pasarela de Pagos

Servidor mock de PlusPagos que simula una pasarela de pagos real. Desencripta datos de pago usando AES-256-CBC, muestra una página de confirmación estilo Material UI v5 y procesa callbacks al backend.

## Características

- ✓ Desencriptación AES-256-CBC con SHA-256 (compatible con PlusPagosCryptoService.java)
- ✓ Interfaz Material UI v5 con layout split-screen (branding + formulario)
- ✓ Página de confirmación responsive (desktop, tablet, móvil)
- ✓ Webhook POST al backend con detalles de pago
- ✓ Almacenamiento temporal de transacciones en memoria
- ✓ Manejo de cancelación de pagos
- ✓ Página de error con diseño consistente
- ✓ Logs detallados para debugging
- ✓ Sin dependencias externas (solo módulos nativos de Node.js)

## Rutas

### POST /pluspagos
Recibe datos encriptados de pago y muestra página de confirmación.

**Parámetros esperados:**
- `Comercio` - ID del comercio
- `TransaccionComercioId` - ID de transacción único
- `Monto` - Monto encriptado (en centavos)
- `Informacion` - Descripción encriptada
- `CallbackSuccess` - URL encriptada del webhook de éxito
- `CallbackCancel` - URL encriptada del webhook de cancelación (opcional)
- `UrlSuccess` - URL encriptada para redirigir tras éxito
- `UrlError` - URL encriptada para redirigir tras cancelación

### POST /pluspagos/confirmar
Procesa la confirmación, llama al webhook del backend y redirige a UrlSuccess.

**Parámetros:**
- `transaccionId` - ID de la transacción almacenada

### POST /pluspagos/cancelar
Procesa la cancelación y redirige a UrlError.

**Parámetros:**
- `transaccionId` - ID de la transacción almacenada

## Variables de Entorno

```bash
PORT=8081                           # Puerto del servidor
PLUSPAGOS_SECRET=mock-secret-desarrollo  # Secret para desencriptación
BACKEND_URL=http://app:8080        # URL del backend para webhooks
```

## Webhook del Backend

Al confirmar un pago, realiza POST a `CallbackSuccess` con:

```json
{
  "Tipo": "PAGO",
  "TransaccionPlataformaId": "PP-MOCK-{timestamp}",
  "TransaccionComercioId": "{TransaccionComercioId}",
  "Monto": "{monto_plano}",
  "EstadoId": "3",
  "Estado": "REALIZADA",
  "FechaProcesamiento": "{ISO timestamp}"
}
```

## Desencriptación

Compatible con `PlusPagosCryptoService.java`:

1. SHA-256(PLUSPAGOS_SECRET) → clave AES-256 (32 bytes)
2. Entrada base64 contiene: [IV 16 bytes][Ciphertext]
3. Desencriptar con AES-256-CBC

## Uso Local

```bash
# Con variables de entorno
PLUSPAGOS_SECRET=mock-secret-desarrollo \
BACKEND_URL=http://localhost:8080 \
node server.js

# O con valores por defecto
node server.js
```

## Docker

```bash
# Desde el directorio del proyecto RDAM
docker compose up --build -d pluspagos

# Ver logs
docker compose logs -f pluspagos

# Detener
docker compose stop pluspagos
```

## Flujo Completo

1. Frontend obtiene datos de pago del backend: GET /api/solicitudes/{id}/pago-datos
2. Frontend redirige/postea a http://localhost:8081/pluspagos con datos encriptados
3. Mock desencripta y muestra página de confirmación
4. Usuario confirma o cancela
5. Si confirma: Mock llama webhook backend + redirige a UrlSuccess
6. Si cancela: Mock redirige a UrlError
7. Backend procesa el webhook y cambia estado de solicitud a PAGADA

## Debugging

El servidor imprime logs detallados:

```
[POST /pluspagos] Recibiendo solicitud de pago encriptada...
[POST /pluspagos] Desencriptando datos...
[POST /pluspagos] ✓ Desencriptación exitosa
[POST /pluspagos/confirmar] Llamando al webhook:
  URL: http://app:8080/api/pagos/webhook
  Payload: {...}
[POST /pluspagos/confirmar] ✓ Webhook llamado. Status: 200
[POST /pluspagos/confirmar] ✓ Redirigiendo a: ...
```
