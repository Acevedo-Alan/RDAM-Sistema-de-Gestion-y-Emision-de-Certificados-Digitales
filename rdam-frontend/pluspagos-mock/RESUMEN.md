# 📋 Resumen de Solución: Mock PlusPagos

## ✓ Status: COMPLETADO

Todos los archivos necesarios han sido creados y están listos para usar.

---

## 📁 Estructura Final

```
RDAM-frontend/
└── pluspagos-mock/
    ├── server.js                 ← Servidor Node.js completo
    ├── Dockerfile               ← Imagen Docker
    ├── .dockerignore            ← Archivo de exclusión Docker
    ├── README.md                ← Documentación del servicio
    ├── SETUP.md                 ← Guía de instalación
    ├── TESTING.md               ← Guía de testing y validación
    ├── docker-compose-snippet.yml ← Configuración a agregar
    └── RESUMEN.md               ← Este archivo
```

---

## 🚀 Inicio Rápido

### 1. Agregar a docker-compose.yml

Tu archivo `docker-compose.yml` debe estar en la **raíz del proyecto RDAM** (no en rdam-frontend).

Copia el contenido de `pluspagos-mock/docker-compose-snippet.yml` y pegalo en tu `docker-compose.yml` bajo la sección `services:`.

**Estructura esperada:**
```yaml
version: '3.8'

services:
  app:
    build: ./rdam-backend
    # ... config
    depends_on:
      - postgres
    
  pluspagos:              ← AGREGAR ESTO
    build: ./rdam-frontend/pluspagos-mock
    ports:
      - "8081:8081"
    environment:
      PLUSPAGOS_SECRET: mock-secret-desarrollo
      BACKEND_URL: http://app:8080
    depends_on:
      app:
        condition: service_healthy

networks:
  rdam-network:
    driver: bridge
```

### 2. Build y ejecutar

```bash
cd RDAM/

# Build de todos los servicios
docker compose up --build -d

# O solo el servicio pluspagos
docker compose up --build -d pluspagos

# Ver logs
docker compose logs -f pluspagos
```

### 3. Verificar que funciona

```bash
curl http://localhost:8081/

# Respuesta: 404 (normal, porque no hay ruta raíz)
# Esto significa que el servicio está arriba ✓
```

---

## 🔑 Características Implementadas

### ✓ Desencriptación AES-256-CBC
- Compatible con `PlusPagosCryptoService.java`
- Usa SHA-256(secret) como clave
- IV aleatorio de 16 bytes integrado en el payload

### ✓ Página HTML de Confirmación
- Responsive (desktop, tablet, móvil)
- Layout split-screen estilo Material UI v5
- Branding sidebar (izquierda, oculto en mobile)
- Formulario centrado (derecha)
- Estilos MUI: colores primarios, tipografía Roboto
- Animaciones suave
- Compatible visual con LoginPage

### ✓ Webhook POST al Backend
- Llama a `CallbackSuccess` con payload JSON
- Convierte monto de centavos a pesos (ej: 500000 → 5000.00)
- Genera ID único con timestamp
- Incluye estado REALIZADA

### ✓ Almacenamiento en Memoria
- Transacciones se guardan entre /pluspagos y /pluspagos/confirmar
- Se limpian después de procesar
- Map eficiente con TransaccionComercioId como key

### ✓ Manejo de Errores
- Validación de campos requeridos
- Desencriptación con try-catch
- HTML de error personalizado
- Fallback si webhook falla (continúa con redirect)

### ✓ Logging Detallado
- Cada request se logea
- Cada desencriptación se logea
- Cada webhook call se logea
- Logs disponibles via `docker compose logs pluspagos`

### ✓ Sin Dependencias Externas
- Solo módulos nativos de Node.js
- http, crypto, url, querystring
- Reducido, rápido, seguro

---

## 📡 Flujo Esperado

```
1. Usuario en Frontend
   ↓
2. GET /api/solicitudes/{id}/pago-datos (Backend)
   ← Retorna datos encriptados + urlPasarela: http://localhost:8081/pluspagos
   ↓
3. POST http://localhost:8081/pluspagos (Mock)
   Datos encriptados en body
   ← Retorna HTML de confirmación
   ↓
4. Usuario ve página de confirmación
   - Monto: $5000.00 ARS
   - Descripción: "Solicitud XXXX"
   - Botones: Confirmar / Cancelar
   ↓
5a. Si confirma → POST /pluspagos/confirmar
    ↓
    Mock llama POST /api/pagos/webhook (Backend)
    Payload: {TransaccionComercioId, Estado: REALIZADA, Monto, ...}
    ↓
    Backend cambia solicitud a PAGADA
    ↓
    302 Redirect a UrlSuccess
    ↓
    Usuario ve "Pago realizado exitosamente"

5b. Si cancela → POST /pluspagos/cancelar
    ↓
    302 Redirect a UrlError
    ↓
    Usuario ve "Pago cancelado"
```

---

## 🔧 Configuración

### Variables de Entorno

```bash
# En Dockerfile (valores por defecto)
PORT=8081
PLUSPAGOS_SECRET=mock-secret-desarrollo
BACKEND_URL=http://app:8080

# Pueden sobrescribirse en docker-compose.yml
```

### Compatibilidad con Backend

- **Secret:** Debe ser igual en `PlusPagosCryptoService.java` y docker-compose
- **Webhook URL:** `/api/pagos/webhook` en backend (ej: http://app:8080/api/pagos/webhook)
- **Formato de monto:** Entrada en centavos (1000 = $10.00), salida en pesos (10.00)
- **Campos obligatorios:** TransaccionComercioId, Estado, Monto, Tipo, FechaProcesamiento

---

## 📚 Documentación

### Archivo | Contenido | Cuándo leer
---|---|---
README.md | Descripción, rutas, variables | Entender qué hace el servicio
SETUP.md | Instalación paso a paso | Primera vez que configuras
TESTING.md | Tests y validación | Verificar que funciona
docker-compose-snippet.yml | Config a copiar | Agregar a docker-compose.yml

---

## ✅ Checklist de Setup

- [ ] Copiar contenido de `docker-compose-snippet.yml` a `docker-compose.yml`
- [ ] Verificar que red `rdam-network` existe en docker-compose
- [ ] Verificar que servicio `app` (backend) tiene healthcheck configurado
- [ ] Revisar archivo `server.js` y entender flujo
- [ ] Hacer `docker compose up --build -d pluspagos`
- [ ] Ver logs: `docker compose logs -f pluspagos`
- [ ] Verificar en logs: "✓ Mock PlusPagos Server escuchando en puerto 8081"
- [ ] Probar acceso a http://localhost:8081 (debe dar 404)

---

## 🐛 Debugging

### Si el servicio no levanta
```bash
docker compose logs pluspagos
# Buscar errores: ERROR, ✗, exit code
```

### Si el desencriptado falla
```bash
# Ver en logs si el payload es válido
docker compose logs pluspagos | grep "aes-256-cbc"
# Verificar PLUSPAGOS_SECRET es igual en backend y docker-compose
```

### Si el webhook no se llama
```bash
# Ver logs de intento de webhook
docker compose logs pluspagos | grep "Llamando al webhook"
# Verificar que backend está levantado en puerto 8080
docker compose ps app
```

### Si el puerto está en uso
```bash
# Cambiar en docker-compose.yml
ports:
  - "8082:8081"  # 8082 en host, 8081 en container
```

---

## 📊 Performance y Limitaciones

### Performance
- ✓ Sin base de datos
- ✓ Almacenamiento en memoria
- ✓ Desencriptación rápida (AES-256-CBC nativo)
- ✓ Respuestas inmediatas

### Limitaciones (normales en desarrollo)
- ⚠ Transacciones no persisten (en memoria)
- ⚠ No maneja concurrencia extrema
- ⚠ No tiene autenticación (es mock)
- ⚠ No registra auditoría

### Para Producción
- Agregar base de datos (PostgreSQL)
- Agregar logging a archivos
- Agregar métricas (Prometheus)
- Agregar seguridad (JWT, HTTPS)
- Agregar rate limiting

---

## 🔐 Seguridad en Desarrollo

El mock usa:
- AES-256-CBC (algoritmo fuerte)
- SHA-256 para derivación de clave
- IV aleatorio para cada encriptación
- Validación de base64

**IMPORTANTE:** Para producción:
- Usar certificados SSL/TLS
- Configurar Rate Limiting
- Agregar JWT o similar authentication
- Auditar logs
- No exponer en Internet

---

## 📞 Soporte y FAQ

### P: ¿Qué pasa si el backend no responde al webhook?
**R:** El mock registra warning en logs pero continúa con el redirect. El usuario ve éxito pero la BD del backend no se actualiza.

### P: ¿Dónde se guardan las transacciones?
**R:** En memoria del proceso Node. Se pierden al reiniciar container.

### P: ¿Puedo cambiar el puerto?
**R:** Sí, modificar en docker-compose.yml: `ports: ["8082:8081"]`

### P: ¿El secret debe ser igual en frontend y backend?
**R:** No en frontend. Frontend recibe datos ya encriptados. El secret es entre backend y mock.

### P: ¿Puedo usar en producción?
**R:** No recomendado. Es un mock para desarrollo. Ver sección "Para Producción" arriba.

### P: ¿Cómo hago request de prueba?
**R:** Ver `TESTING.md` con ejemplos de curl y Node.js

---

## 📝 Próximos Pasos

1. **Agregar a docker-compose.yml** (ver arriba)
2. **Ejecutar:** `docker compose up --build -d pluspagos`
3. **Verificar:** `docker compose logs -f pluspagos`
4. **Testear:** Ver `TESTING.md` para flujos de prueba
5. **Integrar:** Conectar el frontend con `urlPasarela`
6. **Validar:** Verificar que el backend recibe webhooks correctamente

---

## 📌 Archivos Clave

| Archivo | Responsabilidad |
|---------|-----------------|
| server.js | Lógica del servidor (desencriptación, rutas, webhook) |
| Dockerfile | Empaquetamiento en imagen Docker |
| docker-compose-snippet.yml | Configuración de orquestación |
| README.md | Documentación técnica |
| SETUP.md | Guía de instalación |
| TESTING.md | Guía de testing |

---

## 🎯 Objetivo Logrado

✅ Servicio mock de PlusPagos funcional
✅ Compatible con backend (AES-256-CBC, webhook)
✅ Página HTML de confirmación bonita
✅ Logging detallado para debugging
✅ Sin dependencias externas
✅ Pronto para usar en docker compose

---

**Creado:** 2026-03-18
**Versión:** 1.0
**Status:** ✓ LISTO PARA PRODUCCIÓN (DESARROLLO)
