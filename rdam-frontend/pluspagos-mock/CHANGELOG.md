# CHANGELOG - Mock PlusPagos

Historial de cambios y mejoras.

## [2.0] - 2026-03-18 - REFACTOR UI MATERIAL v5

### Nuevo (NEW)
- Documento DESIGN_MUI.md con especificación completa del diseño
- Layout split-screen estilo Material UI v5
- Branding sidebar izquierda (oculto en mobile)
- Columna derecha con formulario centrado
- Animación slideInRight en entrada de formulario
- Paleta de colores MUI: primary #0F4A7C, success #059669
- Tipografía Roboto
- Sombras y hover effects MUI-like
- Mayor constraint en formulario (maxWidth: 450px)

### Mejora (IMPROVED)
- generator de HTML completamente refactorizado
- Diseño más profesional y moderno
- Página de error con colores de error (#DC2626)
- Página de error con mismo layout split-screen
- Mejor responsividad visual (media queries mejoradas)
- Espaciado consistente (32px gaps principales, 16px segundarios)
- Botones con sombra y transformaciones suaves
- Mejor legibilidad de tipografía

### Cambiado (CHANGED)
- Esquema de colores: de gradiente púrpura a azul oscuro primario
- Estructura HTML: de contenedor centrado a flex split-screen
- Estilos de botones: de simple a MUI-like con sombras
- Footer de seguridad: de emoji a link sutil de cancelación
- Página de error: de simple gradient a split-screen error

### Código
- server.js: 396 líneas → 512 líneas (nuevo HTML)
- Function generateConfirmationPageHTML: completamente reescrita
- Function generateErrorPageHTML: completamente reescrita
- Sin cambios en lógica de servidor (APIs iguales)
- Sin cambios en dependencias (sigue siendo cero npm packages)

### Documentación
- Nuevo: DESIGN_MUI.md (380 líneas con especificación completa)
- Actualizado: README.md (menciona MUI v5)
- Actualizado: QUICK_REFERENCE.md (sección de diseño)
- Actualizado: RESUMEN.md (características de UI)
- Actualizado: ESTATUS.md (marca [NEW] y [IMPROVED])
- Actualizado: INDEX.md (incluye DESIGN_MUI.md)

### Testing
- Responsive en desktop (1440px): split-screen 50/50
- Responsive en tablet (768px): transición al formulario fullwidth
- Responsive en mobile (375px): formulario fullwidth, branding oculto
- Layout fluido: sin horizontal scroll
- Todos los botones tienen adecuados touch targets (min 44px)

### Compatibilidad
- Chrome/Chromium (desktop y mobile)
- Firefox (desktop)
- Safari (desktop y iOS)
- Edge
- Navegadores móviles estándar

### Performance
- Sin cambios: mismo procesamiento
- CSS inline optimizado
- HTML más grande (función de presentación)
- Tamaño HTML: +116 líneas (estilos MUI)

### Próximas Mejoras (Futuro)
- Agregar campos de tarjeta reales (número, CVC, vencimiento)
- Validación visual de campos
- Toast/snackbar para confirmaciones
- Loading state en botón confirmar
- Zoom animation en logo
- Progress stepper (paso 1 de 2, etc.)

---

## [1.0] - 2026-03-18 - RELEASE INICIAL

### Inicial (INITIAL)
- Servidor Node.js puro (sin dependencias npm)
- Desencriptación AES-256-CBC + SHA-256
- Compatible con PlusPagosCryptoService.java
- Página HTML de confirmación responsive
- Layout centrado con card blanca
- Almacenamiento en memoria
- Webhook POST al backend
- Manejo de errores
- Logs detallados
- Dockerizado (Node.js 20-alpine)
- Documentación completa (9 archivos)

---

## Resumen de Cambios v1 → v2

```
0 dependencias → 0 dependencias (sin cambios)
Colores: púrpura gradient → azul primario + success verde
Layout: centrado card → split-screen (branding + form)
Tipografía: system font → Roboto (MUI)
Estilos: simples → MUI-like (sombras, hover, transforms)
Documentación: 9 archivos → 10 archivos (+DESIGN_MUI.md)
Lineas de código: 396 → 512 en server.js
```

---

## Notas de Migración

Si estabas usando v1.0:
1. No necesitas cambios en backend
2. No necesitas cambios en frontend
3. URLs y APIs son idénticas
4. Solo cambia la interfaz visual
5. Nuevo archivo DESIGN_MUI.md para referencia

---

## Verificar el Cambio

Después de update:
```bash
docker compose down
docker compose up --build -d pluspagos
docker compose logs -f pluspagos

# Deberías ver:
# [POST /pluspagos] ✓ Desencriptación exitosa
# [POST /pluspagos] ✓ Página de confirmación enviada
```

Luego abre en navegador: http://localhost:8081/pluspagos
- Deberías ver: Split-screen con branding azul a la izquierda
- Derecha: formulario blanco centrado con monto verde grande

---

**Versión Actual:** 2.0 (Material UI v5)
**Status:** Stable, Production-ready para desarrollo
**Última actualización:** 2026-03-18
