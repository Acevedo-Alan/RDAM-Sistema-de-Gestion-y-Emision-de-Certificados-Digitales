# 📚 Índice - Mock PlusPagos

Guía de navegación por toda la documentación.

---

## 🎯 Primeros Pasos (Empieza aquí)

### 1. RESUMEN.md ← **LEER PRIMERO**
- Qué se creó y por qué
- Estructura final
- Checklist rápido de setup
- ~5 minutos

→ [Leer RESUMEN.md](./RESUMEN.md)

### 2. SETUP.md ← **Segundo paso**
- Instrucciones detalladas paso a paso
- Cómo agregar a docker-compose.yml
- Verificación que funciona
- ~15 minutos

→ [Leer SETUP.md](./SETUP.md)

---

## 📖 Documentación por Tipo

### Para Entender el Diseño MUI v5

**DESIGN_MUI.md** - Especificación completa de diseño
- Layout split-screen (branding + formulario)
- Paleta de colores MUI
- Tipografía Roboto
- Componentes y su estructura
- Media queries responsive
- Animaciones

→ [Leer DESIGN_MUI.md](./DESIGN_MUI.md)

### Para ver Cambios Realizados

**CHANGELOG.md** - Historial de versiones
- Qué es nuevo en v2.0 (refactor MUI)
- Qué mejoró vs v1.0
- Notas de migración
- Cómo verificar los cambios

→ [Leer CHANGELOG.md](./CHANGELOG.md)

### Para Entender el Código

**README.md** - Documentación técnica
- Qué hace cada ruta
- Variables de entorno
- Algoritmo de desencriptación
- Formato del webhook

→ [Leer README.md](./README.md)

### Para Implementar en el Frontend

**INTEGRACION_FRONTEND.md** - Ejemplos React
- Cómo llamar desde frontend
- Componentes de ejemplo
- Manejo de redirects
- Custom hooks
- Logs esperados

→ [Leer INTEGRACION_FRONTEND.md](./INTEGRACION_FRONTEND.md)

### Para Hacer Tests

**TESTING.md** - Testing y validación
- Cómo verificar que funciona
- Tests manuales con curl
- Debugging
- Troubleshooting completo
- Checklist de validación

→ [Leer TESTING.md](./TESTING.md)

### Para Troubleshooting Rápido

**QUICK_REFERENCE.md** - Referencia rápida
- Comandos comunes
- Logs y debugging
- Verificación rápida
- One-liners útiles

→ [Leer QUICK_REFERENCE.md](./QUICK_REFERENCE.md)

---

## 📁 Archivos de Código

### server.js
Servidor Node.js completo (sin dependencias):
- ✓ Desencriptación AES-256-CBC
- ✓ Página HTML de confirmación
- ✓ Webhook al backend
- ✓ Almacenamiento en memoria
- ✓ Manejo de errores
- ✓ Logs detallados

**Cuándo leer:** Necesites entender la lógica o hacer cambios

### Dockerfile
Imagen Docker para el servicio:
- Node.js 20-alpine
- Puerto 8081
- Healthcheck
- Variables de entorno

**Cuándo leer:** Necesites configurar el build o imagen

### docker-compose-snippet.yml
Configuración para agregar a tu docker-compose.yml:
- Servicio pluspagos
- Puertos y ambiente
- Dependencias y health check
- Network configuration

**Cuándo copiar:** Setup inicial

---

## 🚀 Guías Rápidas por Caso de Uso

### Caso 1: "Quiero levantarlo ahora"
1. Leer: [RESUMEN.md](./RESUMEN.md) (5 min)
2. Leer: [SETUP.md](./SETUP.md) Sección "Inicio Rápido" (5 min)
3. Copiar: docker-compose-snippet.yml a tu docker-compose.yml
4. Ejecutar: `docker compose up --build -d pluspagos`
5. Ver: `docker compose logs -f pluspagos`

**Tiempo total: ~15 minutos**

### Caso 2: "Necesito integrar con mi frontend"
1. Leer: [INTEGRACION_FRONTEND.md](./INTEGRACION_FRONTEND.md)
2. Copiar componente de ejemplo
3. Actualizar ruta y parametrizaciones
4. Probar con frontend levantado

**Tiempo total: ~30 minutos**

### Caso 3: "Algo no funciona"
1. Ver: [QUICK_REFERENCE.md](./QUICK_REFERENCE.md) - Troubleshooting
2. Ver: `docker compose logs pluspagos` (buscar errores)
3. Leer: [TESTING.md](./TESTING.md) - sección de errores
4. Revisar: [README.md](./README.md) - formato de datos esperado

**Tiempo total: ~10-20 minutos**

### Caso 4: "Quiero probar todo manualmente"
1. Leer: [TESTING.md](./TESTING.md) completo
2. Generar datos con script JavaScript
3. Hacer requests con curl
4. Verificar logs

**Tiempo total: ~45 minutos**

### Caso 5: "Necesito entender cómo funciona la desencriptación"
1. Leer: [README.md](./README.md) - sección "Desencriptación"
2. Leer: [TESTING.md](./TESTING.md) - Test 2 y 3
3. Ver: server.js líneas 20-40 (código de decrypt)

**Tiempo total: ~15 minutos**

---

## 🔍 Búsqueda Rápida

### Si necesitas INFO sobre...

| Tema | Dónde buscar |
|------|--------------|
| Rutas HTTP | README.md → Rutas |
| Variables de ambiente | SETUP.md → Variables de Entorno |
| Desencriptación | README.md → Desencriptación |
| Webhook del backend | README.md → Webhook del Backend |
| Integración React | INTEGRACION_FRONTEND.md |
| Hacer un test | TESTING.md |
| Comandos Docker | QUICK_REFERENCE.md → Logs & Debugging |
| Troubleshooting | QUICK_REFERENCE.md → Troubleshooting Rápido |
| Errores | TESTING.md → Errores de validación |
| Performance | RESUMEN.md → Performance y Limitaciones |
| Diseño Material UI v5 | DESIGN_MUI.md |
| Cambios realizados | CHANGELOG.md |

---

## 📊 Mapa Mental

```
Mock PlusPagos
├─ ¿Qué es? → RESUMEN.md
├─ ¿Cómo instalar? → SETUP.md
├─ ¿Cómo funciona? → README.md
├─ ¿Cómo integrar? → INTEGRACION_FRONTEND.md
├─ ¿Cómo testear? → TESTING.md
├─ ¿Comandos rápidos? → QUICK_REFERENCE.md
├─ ¿Diseño MUI v5? → DESIGN_MUI.md
├─ ¿Cambios realizados? → CHANGELOG.md
│
├─ Código
│  ├─ server.js
│  ├─ Dockerfile
│  └─ docker-compose-snippet.yml
│
└─ Problemas?
   ├─ Ver logs
   ├─ Leer TESTING.md
   └─ Revisar QUICK_REFERENCE.md
```

---

## ✅ Checklist de Lectura

### Lectura Mínima (Obligatoria)
- [ ] RESUMEN.md (Qué y por qué)
- [ ] SETUP.md - Sección "Inicio Rápido" (Cómo)
- [ ] QUICK_REFERENCE.md (Comandos comunes)
- [ ] CHANGELOG.md (Qué cambió en v2.0)

**Tiempo: ~25 minutos**

### Lectura Completa Recomendada
- [ ] RESUMEN.md (completo)
- [ ] SETUP.md (completo)
- [ ] README.md (Rutas y Variables)
- [ ] INTEGRACION_FRONTEND.md (Tu caso)
- [ ] QUICK_REFERENCE.md (Top to bottom)
- [ ] DESIGN_MUI.md (Entender el diseño)
- [ ] CHANGELOG.md (Qué cambió y por qué)

**Tiempo: ~75 minutos**

### Lectura Avanzada (Opcional)
- [ ] TESTING.md (completo - para profundizar)
- [ ] server.js (comentarios y código)
- [ ] README.md (toda la sección de Debugging)
- [ ] DESIGN_MUI.md (Entender toda la especificación)

**Tiempo: ~60 minutos**

---

## 🎓 Currículum de Aprendizaje

### Nivel 1: Usar el Mock (Principiante)
Leer: RESUMEN.md + SETUP.md
Objetivo: Levantar el servicio
Tiempo: 15 min

### Nivel 2: Integrar en Frontend (Intermedio)
Leer: INTEGRACION_FRONTEND.md + QUICK_REFERENCE.md
Objetivo: Conectar frontend
Tiempo: 45 min

### Nivel 3: Validar y Testear (Avanzado)
Leer: TESTING.md (completo)
Objetivo: Tests manuales
Tiempo: 60 min

### Nivel 4: Entender el Código (Experto)
Leer: README.md + server.js + comentarios en código
Objetivo: Poder hacer cambios al servidor
Tiempo: 90 min

---

## 🔗 Referencias Cruzadas Comunes

**Si estás en SETUP.md y necesitas ver el código:**
→ Abre server.js

**Si estás en TESTING.md y necesitas entender la desencriptación:**
→ Lee README.md → Desencriptación

**Si estás en INTEGRACION_FRONTEND.md y necesitas los URLs:**
→ Lee README.md → Rutas

**Si algo no funciona:**
→ TESTING.md → sección de tu error → QUICK_REFERENCE.md

**Si necesitas un comando rápido:**
→ QUICK_REFERENCE.md (tabla de troubleshooting)

---

## 📝 Notas de Estudio

### Conceptos Clave
1. **Desencriptación**: AES-256-CBC con SHA-256(secret)
2. **Flujo**: Recibe → Desencripta → Muestra → Confirma → Webhook → Redirige
3. **Almacenamiento**: En memoria (transacciones temporales)
4. **Webhook**: POST JSON al backend con estado REALIZADA
5. **Conversión**: Centavos → Pesos (monto / 100)

### Archivos Más Importantes
1. **server.js** - El corazón (300 líneas)
2. **docker-compose-snippet.yml** - Configuración (20 líneas)
3. **SETUP.md** - Guía paso a paso

### Tiempo de Lectura Total
- Mínimo: 20 minutos
- Recomendado: 60 minutos
- Completo: 150 minutos

---

## 🎯 Quick Start (TL;DR)

```bash
# 1. Copiar docker-compose-snippet.yml a tu docker-compose.yml
# 2. docker compose up --build -d pluspagos
# 3. docker compose logs -f pluspagos
# 4. Ver "✓ Mock PlusPagos Server escuchando en puerto 8081"
# 5. ¡Listo! Ya funciona.

# Si necesitas inicar: leer SETUP.md (5 min)
# Si necesitas integrar: leer INTEGRACION_FRONTEND.md (15 min)
# Si algo falla: leer TESTING.md o QUICK_REFERENCE.md
```

---

## 📞 FAQ de Documentación

**P: ¿Por dónde empiezo?**
R: RESUMEN.md (5 min) → SETUP.md "Inicio Rápido" (5 min) → CHANGELOG.md para ver cambios → Levanta servicio

**P: ¿Qué es lo nuevo en v2.0?**
R: Ver CHANGELOG.md o DESIGN_MUI.md para detalles del diseño Material UI v5

**P: ¿Qué archivo leo para integrar con React?**
R: INTEGRACION_FRONTEND.md (tiene ejemplos completos)

**P: ¿Cómo veo los nuevos estilos Material UI?**
R: Levanta con `docker compose up --build -d pluspagos` y abre http://localhost:8081/pluspagos verá split-screen moderno

**P: ¿Cómo veo los logs?**
R: `docker compose logs -f pluspagos` (ver QUICK_REFERENCE.md)

**P: ¿Cambió la API o funcionalidad?**
R: No, solo el UI. Todas las rutas y APIs son iguales. Ver CHANGELOG.md → "Sin cambios en dependencias"

**P: ¿Qué es lo más importante?**
R: Entender que el mock desencripta datos y simula una pasarela. Lee RESUMEN.md + CHANGELOG.md.

**P: ¿Necesito leer TODO?**
R: No. Lee RESUMEN.md + SETUP.md + CHANGELOG.md (25 min) y ya funciona. Luego lee según necesites.

**P: ¿Dónde está el ejemplo de curl?**
R: TESTING.md → Test 3

**P: ¿Cómo deshabilito el mock?**
R: Comenta sección pluspagos en docker-compose.yml (ver QUICK_REFERENCE.md)

---

## 🚀 Siguientes Pasos Después de Leer

1. **Levanta el servicio** (SETUP.md)
2. **Verifica que funciona** (QUICK_REFERENCE.md - Verificación Rápida)
3. **Integra en frontend** (INTEGRACION_FRONTEND.md)
4. **Haz tests** (TESTING.md)
5. **Carga datos reales y prueba flujo completo**

---

**Última actualización:** 2026-03-18
**Versión documentación:** 1.0
**Status:** ✓ Completa y lista para usar
