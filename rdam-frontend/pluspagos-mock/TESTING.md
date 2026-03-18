# Testing y Validación - Mock PlusPagos

Guía para probar que el mock funciona correctamente.

## Pre-requisitos para Testing

1. Docker Compose corriendo con el servicio `pluspagos`
2. Backend Spring Boot corriendo en puerto 8080
3. Node.js instalado localmente (para testing adicional)
4. Postman o curl disponible

## Test 1: Verificar que el servicio está arriba

```bash
# Check healthcheck
curl -i http://localhost:8081/

# Respuesta esperada: 404 Not Found (no hay ruta raíz, pero servicio está arriba)
# Respuesta si falla: Connection refused
```

## Test 2: Simular desencriptación correcta

### Desde JavaScript (Node.js)

```javascript
const crypto = require('crypto');

// Datos de prueba
const PLUSPAGOS_SECRET = 'mock-secret-desarrollo';
const montoARS = '5000.00';
const descripcion = 'Solicitud RID-001';

// Función para encriptar (igual que backend)
function encrypt(texto, secret) {
  const key = crypto.createHash('sha256').update(secret).digest();
  const iv = crypto.randomBytes(16);
  const cipher = crypto.createCipheriv('aes-256-cbc', key, iv);
  let encrypted = cipher.update(texto, 'utf8', 'base64');
  encrypted += cipher.final('base64');
  // Retornar: IV + ciphertext en base64
  const combined = Buffer.concat([iv, Buffer.from(encrypted, 'base64')]);
  return combined.toString('base64');
}

// Encriptar datos de prueba
const encryptedMonto = encrypt(montoARS, PLUSPAGOS_SECRET);
const encryptedDescripcion = encrypt(descripcion, PLUSPAGOS_SECRET);
const encryptedCallbackSuccess = encrypt(
  'http://localhost:8080/api/pagos/webhook',
  PLUSPAGOS_SECRET
);
const encryptedUrlSuccess = encrypt(
  'http://localhost:5173/solicitudes/1?success=true',
  PLUSPAGOS_SECRET
);
const encryptedUrlError = encrypt(
  'http://localhost:5173/solicitudes/1?error=cancelled',
  PLUSPAGOS_SECRET
);

console.log('Datos encriptados para test:');
console.log('Monto:', encryptedMonto);
console.log('Descripción:', encryptedDescripcion);
console.log('CallbackSuccess:', encryptedCallbackSuccess);
console.log('UrlSuccess:', encryptedUrlSuccess);
console.log('UrlError:', encryptedUrlError);

// Guardar para usar en curl
const testData = {
  encryptedMonto,
  encryptedDescripcion,
  encryptedCallbackSuccess,
  encryptedUrlSuccess,
  encryptedUrlError,
};

console.log('\nJSON para usar en tests:');
console.log(JSON.stringify(testData, null, 2));
```

### Ejecutar script

```bash
# Crear archivo test-encrypt.js con el código anterior
node test-encrypt.js

# Salida:
# Datos encriptados para test:
# Monto: ABC123...=
# Descripción: DEF456...=
# CallbackSuccess: GHI789...=
# UrlSuccess: JKL012...=
# UrlError: MNO345...=
```

## Test 3: POST a /pluspagos con curl

```bash
# Con los valores encriptados del test anterior, hacer POST
curl -X POST http://localhost:8081/pluspagos \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data "Comercio=RDAM&TransaccionComercioId=SOL-001&Monto=ABC123...&Informacion=DEF456...&CallbackSuccess=GHI789...&UrlSuccess=JKL012...&UrlError=MNO345..."

# Respuesta esperada: HTML (página de confirmación)
# Código HTTP: 200
```

## Test 4: Verificar desencriptación exitosa en logs

```bash
# Mirar logs del container
docker compose logs pluspagos -f

# Deberías ver:
# [POST /pluspagos] Recibiendo solicitud de pago encriptada...
# [POST /pluspagos] Desencriptando datos...
# [POST /pluspagos] ✓ Desencriptación exitosa
```

## Test 5: Flujo completo manual

### 5a. Hacer POST a /pluspagos

```bash
# Guardar respuesta HTML (página de confirmación)
curl -X POST http://localhost:8081/pluspagos \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data "Comercio=RDAM&TransaccionComercioId=SOL-001&Monto=ABC123...&Informacion=DEF456...&CallbackSuccess=GHI789...&UrlSuccess=JKL012...&UrlError=MNO345..." \
  > confirmacion.html

# Abrir en navegador
open confirmacion.html  # macOS
start confirmacion.html # Windows
xdg-open confirmacion.html # Linux
```

### 5b. Simular click en "Confirmar pago" (manualmente en el navegador)
- Deberías ver en logs:
```
[POST /pluspagos/confirmar] Procesando confirmación de pago...
[POST /pluspagos/confirmar] Llamando al webhook:
  URL: http://app:8080/api/pagos/webhook
  Payload: {...}
[POST /pluspagos/confirmar] ✓ Webhook llamado. Status: 200
[POST /pluspagos/confirmar] ✓ Redirigiendo a: http://localhost:5173/...
```

## Test 6: Verificar webhook del backend

### Opción A: Sin backend real (mock del webhook)

Crear un listener local en otro terminal:

```bash
# Terminal 1: Mock listener en puerto 3000
node -e "
const http = require('http');
http.createServer((req, res) => {
  if (req.method === 'POST') {
    let body = '';
    req.on('data', (chunk) => { body += chunk; });
    req.on('end', () => {
      console.log('[WEBHOOK RECIBIDO]');
      console.log(JSON.parse(body));
      res.writeHead(200);
      res.end('OK');
    });
  }
}).listen(3000);
console.log('Listener en http://localhost:3000');
"

# Terminal 2: Cambiar BACKEND_URL en docker-compose.yml
# BACKEND_URL: http://host.docker.internal:3000

# Terminal 3: Hacer test con POST a /pluspagos
```

### Opción B: Con backend real

```bash
# Asegurarse que el endpoint existe en backend
curl -X POST http://localhost:8080/api/pagos/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "Tipo": "PAGO",
    "TransaccionPlataformaId": "PP-MOCK-123",
    "TransaccionComercioId": "SOL-001",
    "Monto": "5000.00",
    "EstadoId": "3",
    "Estado": "REALIZADA",
    "FechaProcesamiento": "2026-03-18T10:30:00Z"
  }'

# Respuesta esperada: 200 OK
```

## Test 7: Cancelación de pago

### 7a. Simular cancelación vía API

```bash
# Primero hacer POST a /pluspagos para registrar transacción
curl -X POST http://localhost:8081/pluspagos \
  --data "Comercio=RDAM&TransaccionComercioId=SOL-003&..." \
  > /tmp/cancel_page.html

# Luego hacer POST a cancelar
curl -X POST http://localhost:8081/pluspagos/cancelar \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data "transaccionId=SOL-003" \
  -v

# Esperar respuesta 302 con Location header
# Verificar en logs:
# [POST /pluspagos/cancelar] Transacción cancelada: SOL-003
# [POST /pluspagos/cancelar] Redirigiendo a: http://localhost:5173/...
```

## Test 8: Errores de validación

### 8a. POST a /pluspagos sin campo requerido

```bash
curl -X POST http://localhost:8081/pluspagos \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data "Comercio=RDAM&TransaccionComercioId=SOL-004"
  # Falta Monto, Informacion, etc.

# Respuesta esperada: 400 con HTML de error
# Verificar en logs:
# [POST /pluspagos] ✗ Error: Campo requerido faltante: Monto
```

### 8b. Datos encriptados inválidos

```bash
curl -X POST http://localhost:8081/pluspagos \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data "Comercio=RDAM&TransaccionComercioId=SOL-005&Monto=DATOS_INVALIDOS&..."

# Respuesta esperada: 400 con HTML de error
# Verificar en logs:
# [POST /pluspagos] ✗ Error: Datos encriptados inválidos
```

## Test 9: Conversión de monto (centavos → pesos)

```bash
# Verificar en logs cuando se procesa confirmación
# Debe mostrar conversión correcta:
# 500000 centavos → 5000.00 ARS

# Buscar en logs:
# docker compose logs pluspagos | grep "Monto"
```

## Test 10: Transacciones en memoria expiran

```bash
# 1. Hacer POST a /pluspagos (registra transacción)
curl -X POST http://localhost:8081/pluspagos \
  --data "...TransaccionComercioId=SOL-999..."

# 2. Esperar (las transacciones se limpian después de confirmar/cancelar)

# 3. Intentar confirmar con transaccionId que no existe
curl -X POST http://localhost:8081/pluspagos/confirmar \
  --data "transaccionId=SOL-FAKE"

# Respuesta esperada: 500 con error "Transacción no encontrada"
```

## Checklist de Validación

- [ ] Docker compose up -d levanta sin errores
- [ ] docker compose ps muestra pluspagos running
- [ ] curl a puerto 8081 responde (404 es OK)
- [ ] POST a /pluspagos retorna HTML (status 200)
- [ ] Logs muestran desencriptación exitosa
- [ ] POST a /confirmar hace webhook al backend
- [ ] Backend recibe payload JSON correcto
- [ ] POST a /cancelar no hace webhook, solo redirección
- [ ] Errores de validación retornan 400/500 con HTML de error
- [ ] Logs son informativos y útiles para debugging

## Comandos útiles

```bash
# Ver estado del servicio
docker compose ps pluspagos

# Seguir logs en tiempo real
docker compose logs -f pluspagos

# Reinicar servicio
docker compose restart pluspagos

# Ver solo últimas 50 líneas
docker compose logs pluspagos --tail=50

# Hacer log grep para buscar errores
docker compose logs pluspagos | grep "Error\|✗"

# Enter a container interactivamente
docker compose exec pluspagos sh

# Ver variables de entorno en contenedor
docker compose exec pluspagos printenv | grep PLUSPAGOS
```

## Notas para debugging

1. **Los datos desencriptados se muestran en logs** - Útil para verificar que la desencriptación es correcta

2. **Las transacciones se guardan 1-2 segundos en memoria** - No preocuparse si "Transacción no encontrada" después de un rato

3. **El webhook hace POST real al backend** - Si el backend está caído, verás warning en logs pero continuará el redirect

4. **El HTML de confirmación es responsive** - Funciona en mobile, tablet, desktop

5. **No hay persistencia entre reinicios** - docker compose restart pluspagos limpia todas las transacciones en memoria

## Troubleshooting

| Problema | Causa | Solución |
|----------|-------|----------|
| 404 on /pluspagos | Servicio no está corriendo | `docker compose up -d pluspagos` |
| "Datos encriptados inválidos" | Base64 corrupto | Verificar encriptación en frontend |
| El webhook no llama | BACKEND_URL incorrecto | Cambiar en docker-compose.yml |
| Webhook falla (warning) | Backend caído | Levantar backend en puerto 8080 |
| "Transacción no encontrada" | Expiró entre POST/confirmar | Normal, reintentar flujo |
| Puerto 8081 en uso | Otro proceso usa puerto | Cambiar en docker-compose: 8082:8081 |

---

**Última actualización:** 2026-03-18
