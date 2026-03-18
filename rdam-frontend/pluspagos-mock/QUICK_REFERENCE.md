# ⚡ Referencia Rápida - Mock PlusPagos

Comandos y snippets comunes para trabajar con el mock.

---

## 🚀 Start & Stop

```bash
# Levantar el mock
docker compose up --build -d pluspagos

# Detener
docker compose stop pluspagos

# Reiniciar
docker compose restart pluspagos

# Apagar todo
docker compose down
```

---

## 📊 Logs & Debugging

```bash
# Ver logs en tiempo real
docker compose logs -f pluspagos

# Últimas 100 líneas
docker compose logs pluspagos --tail=100

# Buscar errores
docker compose logs pluspagos | grep -i error

# Ver solo desencriptación
docker compose logs pluspagos | grep "Desencript"

# Ver webhooks
docker compose logs pluspagos | grep "webhook"
```

---

## 🔍 Verificación Rápida

```bash
# ¿Está levantado?
curl http://localhost:8081/

# ¿Responde?
curl -v http://localhost:8081/pluspagos

# Ver status en compose
docker compose ps pluspagos

# Ver variables de entorno
docker compose exec pluspagos printenv | grep PLUS
```

---

## 🧪 Tests Manuales

### Generar datos encriptados (Node.js)

```javascript
const crypto = require('crypto');

const secret = 'mock-secret-desarrollo';
const text = '5000.00';

function getKey(secret) {
  return crypto.createHash('sha256').update(secret).digest();
}

function encrypt(text, secret) {
  const key = getKey(secret);
  const iv = crypto.randomBytes(16);
  const cipher = crypto.createCipheriv('aes-256-cbc', key, iv);
  let encrypted = cipher.update(text, 'utf8', 'base64');
  encrypted += cipher.final('base64');
  const combined = Buffer.concat([iv, Buffer.from(encrypted, 'base64')]);
  return combined.toString('base64');
}

console.log('Encriptado:', encrypt(text, secret));
```

### Hacer POST con curl

```bash
# Primero generar datos encriptados con el script anterior

MONTO="ENC_MONTO_AQUI"
DESC="ENC_DESC_AQUI"
CALLBACK="ENC_CALLBACK_AQUI"
SUCCESS="ENC_SUCCESS_AQUI"
ERROR="ENC_ERROR_AQUI"

curl -X POST http://localhost:8081/pluspagos \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data "Comercio=TEST&TransaccionComercioId=SOL-999&Monto=$MONTO&Informacion=$DESC&CallbackSuccess=$CALLBACK&UrlSuccess=$SUCCESS&UrlError=$ERROR" \
  > test.html

# Abrir en navegador
open test.html  # macOS
start test.html # Windows
```

---

## 🔧 Troubleshooting Rápido

| Problema | Comando | Solución |
|----------|---------|----------|
| Puerto en uso | `lsof -i :8081` | Cambiar puerto en docker-compose |
| Servicio no levanta | `docker compose logs pluspagos` | Ver errores en logs |
| Desencriptación falla | `grep "Desencript" <logs>` | Verificar secret |
| Webhook no funciona | `grep "Llamando" <logs>` | Verificar BACKEND_URL |
| Transacción no encontrada | Revisar logs | Normal si tarda mucho |

---

## 📝 Configuración en docker-compose.yml

```yaml
  pluspagos:
    build: ./rdam-frontend/pluspagos-mock
    ports:
      - "8081:8081"              # Cambiar puerto host aquí
    environment:
      PORT: 8081
      PLUSPAGOS_SECRET: mock-secret-desarrollo  # Cambiar secret aquí
      BACKEND_URL: http://app:8080              # Cambiar backend aquí
    depends_on:
      app:
        condition: service_healthy
```

---

## 🔄 Flujo Completo (Manual)

```bash
# Terminal 1: Levanta todo
docker compose up -d
docker compose logs -f pluspagos

# Terminal 2: Genera datos encriptados (ver script anterior)
node encrypt-test.js > payload.txt

# Terminal 3: Hace POST con curl (ver ejemplos anterior)
# Abre HTML en navegador
# Click en "Confirmar pago"
# Ver redirects y logs
```

---

## 🛠️ Maintenance

```bash
# Limpiar todo
docker compose down -v

# Rebuild solo mock
docker compose build pluspagos

# Pull latest node image
docker pull node:20-alpine

# Rebuild con nueva imagen
docker compose down
docker compose up --build -d pluspagos
```

---

## 📱 URLs Importantes

| URL | Propósito |
|-----|-----------|
| http://localhost:8081/pluspagos | POST datos encriptados |
| http://localhost:8081/pluspagos/confirmar | POST confirmación |
| http://localhost:8081/pluspagos/cancelar | POST cancelación |
| http://localhost:8080/api/pagos/webhook | Webhook del backend |

---

## 🔐 Variables de Entorno

```bash
# Locales
PLUSPAGOS_SECRET=mock-secret-desarrollo
BACKEND_URL=http://localhost:8080
PORT=8081

# En Docker
http://app:8080  # Nombre del servicio en compose
```

---

## 📌 Checklist Pre-Deployment

- [ ] docker-compose.yml tiene sección pluspagos
- [ ] Todos los servicios levantan: `docker compose ps`
- [ ] Mock está en puerto 8081: `curl http://localhost:8081/`
- [ ] Backend responde: `curl http://localhost:8080/`
- [ ] Logs son limpios: `docker compose logs pluspagos | grep Error`
- [ ] Frontend redirecciona correctamente
- [ ] Backend recibe webhook y responde 200
- [ ] Usuario ve página de confirmación bonita

---

## 🎓 Conceptos Clave (Referencias)

```javascript
// Encriptación
AES-256-CBC   ← Algoritmo
SHA-256       ← Derivación de clave
16 bytes IV   ← Vector de inicialización aleatorio

// Conversión de monto
500000 centavos → (500000 / 100) → 5000.00 ARS

// Flujo
/pluspagos (recibe)
  ↓
/pluspagos/confirmar (procesa + webhook)
  ↓
/pluspagos/cancelar (redirige sin webhook)
```

---

## 🐛 Debug Mode

Ver TODOS los logs con timestamps:

```bash
docker compose logs pluspagos --timestamps --tail=0 -f

# Salida:
2026-03-18T10:23:45.123Z [POST /pluspagos] Recibiendo...
2026-03-18T10:23:46.234Z [POST /pluspagos] Desencriptando...
```

---

## 📞 Si Algo Falla

1. **Revisar logs:** `docker compose logs pluspagos`
2. **Buscar errores:** `| grep -E "Error|✗|failed"`
3. **Revisar config:** `docker compose exec pluspagos printenv`
4. **Reintentar:** `docker compose restart pluspagos`
5. **Rebuild:** `docker compose up --build -d pluspagos`

---
## 🎨 Nuevo Diseño Material UI v5

El mock ahora tiene un layout split-screen estilo Material UI v5:

```
Desktop View:
┌──────────────────┬──────────────────┐
│   BRANDING       │   FORMULARIO     │
│   (Azul oscuro)  │   (Blanco)       │
│   40% ancho      │   60% ancho      │
└──────────────────┴──────────────────┘

Mobile View:
┌──────────────────┐
│   FORMULARIO     │
│   (Pantalla comp)│
└──────────────────┘
(Branding oculto)
```

Características:
- Split-screen: branding (izquierda) + formulario (derecha)
- Diseño responsive (oculta branding en mobile)
- Colores MUI: primario #0F4A7C, success #059669
- Tipografía Roboto
- Animaciones suave (slideInRight)
- Botones con sombra y hover effects
- Compatible LoginPage visual

Ver DESIGN_MUI.md para detalles completos del diseño.

---
## 🎯 Validación Final

```bash
# Este comando verifica que todo está correcto:

echo "=== Verificación del Mock PlusPagos ==="

echo "1. ¿Servicio levantado?"
docker compose ps pluspagos | grep -q "Up" && echo "✓ SÍ" || echo "✗ NO"

echo "2. ¿Puerto accesible?"
curl -s http://localhost:8081/ > /dev/null && echo "✓ SÍ" || echo "✗ NO"

echo "3. ¿Logs sin errores?"
docker compose logs pluspagos | grep -q "Error" && echo "✗ CON ERRORES" || echo "✓ SÍ"

echo "4. ¿Variables configuradas?"
docker compose exec pluspagos printenv | grep PLUSPAGOS_SECRET > /dev/null && echo "✓ SÍ" || echo "✗ NO"

echo "5. ¿Backend asignado?"
docker compose exec pluspagos printenv | grep BACKEND_URL > /dev/null && echo "✓ SÍ" || echo "✗ NO"

echo "=== Fin de verificación ==="
```

---

## 💾 Backup de Configuración

```bash
# Guardar tu docker-compose.yml actual
cp docker-compose.yml docker-compose.yml.backup

# Si algo falla, restaurar
cp docker-compose.yml.backup docker-compose.yml
```

---

## 🚀 One-Liner para Levantarlo Todo

```bash
# Build + levanta + muestra logs
docker compose up --build -d pluspagos && docker compose logs -f pluspagos
```

---

## 📊 Recursos Utilizados

- **RAM**: ~50-100 MB (Node.js 20-alpine)
- **Disco**: ~150 MB (imagen Docker)
- **CPU**: Mínimo (espera pasivo)
- **Red**: Puerto 8081

---

**Última actualización:** 2026-03-18
