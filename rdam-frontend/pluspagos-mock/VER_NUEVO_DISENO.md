# VER EL NUEVO DISEÑO MATERIAL UI v5

Guía rápida para ver el refactor.

## 30 Segundos

```bash
cd RDAM/
docker compose up --build -d pluspagos
open http://localhost:8081/pluspagos
```

Verás:
- Split-screen moderno (azul + blanco)
- Branding sidebar izquierdo
- Formulario centrado derecha
- Monto en verde grande
- Botones con sombra MUI

## 2 Minutos - Verificación Completa

```bash
# 1. Antes de empezar
cd RDAM/

# 2. Check actual del código
node -c rdam-frontend/pluspagos-mock/server.js
# Output: (sin errores = válido)

# 3. Levantar servicio
docker compose up --build -d pluspagos

# 4. Esperar que esté listo
sleep 3
docker compose logs pluspagos | grep "escuchando"

# 5. Abrir en navegador (elegir según OS)
# macOS:
open http://localhost:8081/pluspagos

# Windows:
start http://localhost:8081/pluspagos

# Linux:
xdg-open http://localhost:8081/pluspagos
```

## Qué Verás (Desktop 1440px)

```
Lado Izquierdo:
- Fondo azul oscuro (#0F4A7C)
- Texto blanco: "Portal de Pagos Seguro"
- Subtítulo: "Estás a un paso de completar..."
- Footer pequeño: "Mock de Integración PlusPagos"

Lado Derecho:
- Fondo blanco
- Título: "Detalles del Pago"
- Monto grande en verde: "$5000.00 ARS"
- Box con ID transacción y descripción
- Dos botones: "CONFIRMAR PAGO" (verde) y "CANCELAR" (gris)
- Link abajo: "Cancelar y volver al sistema RDAM"
```

## Qué Verás (Tablet 768px)

```
- Lado izquierdo: DESAPARECE (hidden)
- Formulario: ocupa fullwidth
- Mismos colores y estilos
- Botones fullwidth también
```

## Qué Verás (Mobile 375px)

```
- Formulario: 100% ancho
- Padding reducido: 24px
- Typography: más pequeña
- Botones: fullwidth responsive
- Touch targets: 44px+ (óptimo para móvil)
```

## Cambios vs Versión Anterior (v1.0)

```
ANTES (v1.0):
- Card centrada en gradiente púrpura
- Simplistico pero básico
- Sin split-screen

AHORA (v2.0):
- Split-screen profesional
- Branding sidebar #0F4A7C
- Formulario centrado blanco
- Estilos Material UI v5
- Botones con shadow
- Tipografía Roboto
- Responsive completo
```

## Verificar Responsividad

### Opción 1: DevTools del Navegador
```
1. Abre http://localhost:8081/pluspagos
2. F12 o Ctrl+Shift+I para DevTools
3. Ctrl+Shift+M para Toggle Device Toolbar
4. Prueba tamaños:
   - 1440px (Desktop): split-screen visible
   - 768px (Tablet): branding oculto
   - 375px (Mobile): formulario fullwidth
```

### Opción 2: Resize Manual
```
1. Abre navegador fullscreen
2. Redimensiona ventana manualmente
3. En ~1024px:
   - Branding desaparece
   - Formulario ocupa todo
4. En ~600px:
   - Font sizes reducen
   - Padding ajusta
```

## Archivos Clave del Refactor

### Código
- **server.js** - HTML generado con nuevo layout (512 líneas)

### Documentación
- **DESIGN_MUI.md** - Especificación completa del diseño
- **VISUAL_PREVIEW.md** - Preview ASCII del layout
- **CHANGELOG.md** - Qué cambió de v1 a v2
- **REFACTOR_SUMMARY.md** - Resumen ejecutivo del refactor

## Colores Principales

```
Primary Dark: #0F4A7C (branding sidebar)
Success: #059669 (botón confirmar, monto)
Error: #DC2626 (error page)
White: #FFFFFF (background form)
Neutral: #F3F4F6, #D1D5DB, #6B7280, #A9AEB1
```

## Tipografía

```
Font: Roboto (Google Font style)
Fallback: -apple-system, BlinkMacSystemFont, 'Segoe UI'

Tamaños:
- H1 Branding: 32px, weight 700
- H5 Form Title: 24px, weight 700
- H4 Amount: 36px (desktop) / 28px (mobile), weight 700
- Body: 14px, weight 400
- Label: 12px, weight 600
- Caption: 13px, weight 400-500
```

## Interactividad

### Click "CONFIRMAR PAGO"
```
1. Actualiza color a hover (más oscuro)
2. Hace POST a /pluspagos/confirmar
3. Backend webhook: POST /api/pagos/webhook
4. Redirige a UrlSuccess
```

### Click "CANCELAR"
```
1. Hace POST a /pluspagos/cancelar
2. Redirige a UrlError (sin webhook)
```

### Hover Link "Cancelar y volver"
```
1. Color cambia a primary (#0F4A7C)
2. Hace POST a /pluspagos/cancelar
```

## Prueba Completa (5 minutos)

```bash
# Terminal 1: Levanta mock
cd RDAM/
docker compose up --build -d pluspagos
docker compose logs -f pluspagos

# Terminal 2: Abre en navegador
# http://localhost:8081/pluspagos

# Deberías ver:
# - HTML renderizado
# - Split-screen layout
# - Colores MUI aplicados
# - Responsive en redimensionamiento

# Terminal 1 mostrará logs:
# [POST /pluspagos] ✓ Página de confirmación enviada
```

## Limpieza

```bash
# Detener solo pluspagos
docker compose stop pluspagos

# O detener todo
docker compose down

# Ver logs después
docker compose logs pluspagos --tail=50
```

## Troubleshooting

### No veo split-screen
- Verificar que navegador es desktop (1024px+)
- Redimensionar ventana más ancho
- F12 → DevTools → Console → ver errores

### Veo error de conexión
```bash
# Verificar que el servicio está corriendo
docker compose ps pluspagos

# Ver logs
docker compose logs pluspagos

# Levantar de nuevo
docker compose up --build -d pluspagos
```

### Colores no se ven bien
- Limpiar caché: Ctrl+F5 (hard refresh)
- DevTools → Disable cache si está habilitada
- Verificar que no hay CSS override

## Comparación Visual Rápida

### URL en Navegador
```
http://localhost:8081/pluspagos
├─ Espera por HTML Response
├─ CSS inline se aplica
├─ Layout split-screen se renderiza
└─ Totalmente autocontenido (no hay requests adicionales)
```

## Documentación para Profundizar

- **DESIGN_MUI.md** - For layout details
- **VISUAL_PREVIEW.md** - Para ver ASCII preview
- **CHANGELOG.md** - Para entender cambios
- **REFACTOR_SUMMARY.md** - Para resumen completo
- **README.md** - Para APIs y rutas

## Status del Refactor

✓ HTML completamente refactorizado
✓ Material UI v5 aplicado
✓ Layout split-screen implementado
✓ Responsive en todos los breakpoints
✓ Sintaxis valida (node -c verificado)
✓ Sin cambios en funcionalidad backend
✓ Documentación completa

## Próximo Paso

Después de ver el diseño, leer:
1. DESIGN_MUI.md para entender la especificación
2. VISUAL_PREVIEW.md para ver los detalles
3. CHANGELOG.md para entender qué cambió

---

**Status:** ✓ REFACTOR COMPLETADO
**Versión:** 2.0 Material UI v5
**Fecha:** 2026-03-18
**Tiempo para ver:** 30 segundos
**Tiempo para probar completo:** 5 minutos
