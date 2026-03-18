# SETUP - Mock PlusPagos

## Resumen de Archivos Creados

```
rdam-frontend/
└── pluspagos-mock/
    ├── server.js                    # Servidor Node.js puro (AES-256-CBC)
    ├── Dockerfile                   # Imagen Docker
    ├── .dockerignore               # Archivos a ignorar en build
    ├── README.md                   # Documentación del servicio
    ├── docker-compose-snippet.yml  # Configuración para docker-compose
    └── SETUP.md                    # Este archivo
```

## Instalación y Configuración

### 1. Archivos Ya Creados ✓

Los siguientes archivos ya existen y están listos:

- **server.js** - Servidor completo con:
  - Desencriptación AES-256-CBC + SHA-256
  - Página HTML de confirmación con estilos inline
  - Manejo de transacciones en memoria
  - Webhook POST al backend
  - Logs detallados

- **Dockerfile** - Imagen Docker con:
  - Node.js 20-alpine
  - Puerto 8081 expuesto
  - Healthcheck configurado
  - Variables de entorno

### 2. Agregar al docker-compose.yml

Ubicación: Directorio raíz del proyecto RDAM (no en rdam-frontend)

Opción A: Edición manual
```bash
# Abre: RDAM/docker-compose.yml
# Copia el contenido del archivo pluspagos-mock/docker-compose-snippet.yml
# y agreégalo bajo 'services:' en tu docker-compose.yml
```

Opción B: Merge automático (si usas YAML)
```bash
cd RDAM
# Tu docker-compose.yml debe contener:
# - service 'app' (spring boot backend)
# - network 'rdam-network' (asegúrate de que existe)
# Luego agrega el bloque 'pluspagos' del snippet
```

**Estructura esperada del docker-compose.yml:**
```yaml
version: '3.8'

services:
  app:
    build: ...
    ports:
      - "8080:8080"
    healthcheck: ...
    networks:
      - rdam-network
    # ... otras config

  pluspagos:                    # ← AGREGAR ESTO
    build: ./rdam-frontend/pluspagos-mock
    container_name: rdam-pluspagos-mock
    ports:
      - "8081:8081"
    environment:
      NODE_ENV: development
      PORT: 8081
      PLUSPAGOS_SECRET: mock-secret-desarrollo
      BACKEND_URL: http://app:8080
    depends_on:
      app:
        condition: service_healthy
    networks:
      - rdam-network
    restart: unless-stopped

networks:
  rdam-network:
    driver: bridge
```

### 3. Build y Ejecutar

```bash
# Desde RDAM/

# Build de todos los servicios (incluyendo pluspagos)
docker compose up --build -d

# O solo para pluspagos
docker compose up --build -d pluspagos

# Ver logs en tiempo real
docker compose logs -f pluspagos

# Verificar status
docker compose ps

# Detener solo pluspagos
docker compose stop pluspagos

# Reiniciar
docker compose restart pluspagos
```

### 4. Verificar que Funciona

El servidor estará disponible en:
- **URL local**: http://localhost:8081
- **Dentro de Docker**: http://pluspagos:8081

Test rápido (simular POST):
```bash
# Desde el host
curl -X POST http://localhost:8081/pluspagos \
  -d "Comercio=TEST&TransaccionComercioId=SOL-123" \
  # ... incluir datos encriptados en POST

# Debe retornar: HTML (página de confirmación) o error 400
```

### 5. Integración con Frontend

En el frontend (cuando haces POST a /api/solicitudes/{id}/pago-datos):

```javascript
// Backend retorna:
{
  urlPasarela: "http://localhost:8081/pluspagos",
  ...encryptedData
}

// Frontend redirige:
form.action = data.urlPasarela;  // http://localhost:8081/pluspagos
form.submit();
```

### 6. Integración con Backend

El mock hará POST a:
```
POST http://app:8080/api/pagos/webhook
```

Con payload:
```json
{
  "Tipo": "PAGO",
  "TransaccionPlataformaId": "PP-MOCK-1234567890",
  "TransaccionComercioId": "SOL-123",
  "Monto": "5000.00",
  "EstadoId": "3",
  "Estado": "REALIZADA",
  "FechaProcesamiento": "2026-03-18T..."
}
```

El backend debe tener el endpoint `/api/pagos/webhook` que:
1. Reciba el POST
2. Valide TransaccionComercioId
3. Cambie estado de solicitud a PAGADA
4. Responda con 200 OK

### 7. Debugging

Logs del mock (muy útiles para troubleshooting):
```bash
docker compose logs pluspagos

# Salida típica:
# [2026-03-18T...] POST /pluspagos
# [POST /pluspagos] Recibiendo solicitud de pago encriptada...
# [POST /pluspagos] Desencriptando datos...
# [POST /pluspagos] ✓ Desencriptación exitosa
# [POST /pluspagos] Comercio: RDAM
# [POST /pluspagos] ✓ Página de confirmación enviada
```

### 8. Troubleshooting

**Error: "Transacción no encontrada"**
- El navegador hace POST a /pluspagos/confirmar pero la transacción expiró
- Aumentar timeout en server.js si necesitas más tiempo para confirmar

**Error de desencriptación**
- Verificar que `PLUSPAGOS_SECRET` sea igual en mock y backend
- Verificar que algo pasa correctamente por el frontend (revisar logs)

**El webhook no se llama**
- Verificar que `BACKEND_URL` sea correcto
- Revisar logs: `docker compose logs pluspagos`
- El backend debe estar corriendo (app service)
- Asegurarse que el endpoint `/api/pagos/webhook` existe en backend

**Puerto 8081 ya en uso**
- Cambiar puerto en docker-compose.yml:
  ```yaml
  ports:
    - "8082:8081"  # Host:Container
  ```

## Flujo Completo en Desarrollo

1. **Usuario en frontend hace click en "Pagar"**
   ```
   GET http://localhost:5173/... → obtiene datos de pago
   ```

2. **Frontend obtiene datos del backend**
   ```
   GET http://localhost:8080/api/solicitudes/{id}/pago-datos
   ← Retorna: {urlPasarela, Monto(encriptado), ...}
   ```

3. **Frontend redirige a mock**
   ```
   POST http://localhost:8081/pluspagos
   ← Retorna: HTML con formulario de confirmación
   ```

4. **Usuario ve página bonita del mock**
   - Muestra monto: $5000.00 ARS
   - Muestra descripción: "Solicitud XXX"
   - Botón verde: "Confirmar pago"
   - Botón gris: "Cancelar"

5. **Usuario confirma**
   ```
   POST http://localhost:8081/pluspagos/confirmar
   ```

6. **Mock llama webhook del backend**
   ```
   POST http://app:8080/api/pagos/webhook
   {
     "TransaccionComercioId": "SOL-123",
     "Estado": "REALIZADA",
     ...
   }
   ```

7. **Backend procesa pago**
   - Cambia estado de solicitud a PAGADA
   - Responde 200 OK al mock

8. **Mock redirige a usuario**
   ```
   302 → http://localhost:5173/solicitudes/123?pago=success
   ```

9. **Frontend muestra mensaje de éxito**

## Notas Importantes

- ✓ El mock **no procesa pagos de verdad**, solo simula el flujo
- ✓ Las transacciones se guardan **en memoria** (no persisten si se reinicia)
- ✓ El secret **debe coincidir** entre frontend, backend y mock
- ✓ Los URLs deben ser **válidos** (backend debe existir)
- ✓ El mock **usa módulos nativos de Node** (sin npm packages)
- ✓ Es **production-ready** para desarrollo, pero no para producción
  - Para prod: agregar base de datos, logging a archivos, certificados SSL, etc.

## Cleanup

Si necesitas borrar el servicio:

```bash
# Detener y remover containers
docker compose down

# Remover imagen
docker rmi rdam-pluspagos-mock:latest
docker rmi rdam-frontend-pluspagos:latest  # si existe

# Confirmación:
docker images | grep pluspagos  # debe estar vacío
docker ps -a | grep pluspagos   # debe estar vacío
```

## Variables de Entorno

Pueden sobrescribirse en docker-compose.yml:

```yaml
environment:
  PLUSPAGOS_SECRET: tu-secret-diferente
  BACKEND_URL: http://custom-backend:8080
  PORT: 9090  # cambiar puerto
```

O en desarrollo local:
```bash
PLUSPAGOS_SECRET=mi-secret-custom node server.js
```

---

**Estado:** ✓ Completamente implementado y listo para usar
**Versión:** 1.0
**Última actualización:** 2026-03-18
