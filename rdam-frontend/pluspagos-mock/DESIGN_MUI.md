# Diseño Material UI v5 - Mock PlusPagos

Documentación del refactor de la interfaz a Material UI v5.

## Descripción General

La página de confirmación de pago del mock PlusPagos ha sido rediseñada con un layout split-screen estilo Material UI v5, heredando el mismo visual moderno del LoginPage de RDAM.

## Layout Principal (Grid 100vh Split-Screen)

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  BRANDING COLUMN (40-50%)    │    FORM COLUMN (50-60%)    │
│  - Fondo: #0F4A7C gradient   │    - Fondo: Blanco         │
│  - Oculto en mobile          │    - Formulario centrado   │
│  - Textos y footer           │    - Botones de acción     │
│                             │                             │
└─────────────────────────────────────────────────────────────┘
```

## Columna Izquierda (Branding)

Visible solo en desktop (display: { xs: 'none', md: 'flex' })

### Contenedor
- width: 50%
- background: gradiente linear-gradient(135deg, #0F4A7C, #0A3456)
- flex-direction: column
- justifyContent: center
- gap: 48px
- padding: 60px 40px

### Contenido de Branding
1. **Título Principal**
   - Text: "Portal de Pagos Seguro"
   - fontSize: 32px
   - fontWeight: 700
   - color: white
   - letter-spacing: -0.5px

2. **Subtítulo**
   - Text: "Estás a un paso de completar tu trámite en RDAM"
   - fontSize: 14px
   - fontWeight: 400
   - color: rgba(255, 255, 255, 0.8)
   - line-height: 1.6

3. **Footer Branding**
   - position: absolute
   - bottom: 40px, left: 40px
   - Text: "Mock de Integración PlusPagos — Entorno de Desarrollo"
   - fontSize: 12px
   - color: rgba(255, 255, 255, 0.5)

## Columna Derecha (Formulario)

Ocupa todo el ancho en mobile, 50% en desktop

### Contenedor Principal
- display: flex
- flex-direction: column
- justifyContent: center
- alignItems: center
- background: #FFFFFF
- padding: 40px
- flex: 1
- position: relative

### Form Wrapper
- maxWidth: 450px (responsivo)
- animation: slideInRight 0.4s ease-out

### Encabezado del Formulario
1. **Título**
   - Text: "Detalles del Pago"
   - fontSize: 24px
   - fontWeight: 700
   - color: #1B1B1B
   - letter-spacing: -0.5px
   - marginBottom: 8px

2. **Subtítulo**
   - Text: "Sistema de Recaudación de la Provincia de Santa Fe"
   - fontSize: 13px
   - color: #A9AEB1
   - textAlign: center
   - display: block
   - marginBottom: 32px

### Sección de Monto (Destacado)
```
┌─────────────────────────────┐
│    Monto a pagar            │  <- label (fontSize: 12px, #6B7280)
│    $5000.00 ARS             │  <- amount (fontSize: 36px, color: #059669)
└─────────────────────────────┘
```

- background: #F0F4F9
- border-radius: 12px
- padding: 24px
- marginBottom: 32px

### Sección de Información de Transacción

```
┌─────────────────────────────┐
│ ID de Transacción           │
│ SOL-123                     │
│                             │
│ Descripción                 │
│ Solicitud RID-001...        │
└─────────────────────────────┘
```

- background: #FAFBFC
- border-radius: 8px
- border-left: 4px solid #0F4A7C
- padding: 16px
- marginBottom: 32px

### Grupo de Botones

```
┌────────────┬─────────────┐
│  CONFIRMAR │   CANCELAR  │
└────────────┴─────────────┘
```

#### Botón Primario (Confirmar Pago)
- background: #059669 (success)
- color: white
- padding: 16px 24px
- border-radius: 8px
- fontSize: 14px
- fontWeight: 700
- text-transform: uppercase
- letter-spacing: 0.5px
- box-shadow: 0 4px 12px rgba(5, 150, 105, 0.3)
- :hover:
  - background: #047857
  - box-shadow: 0 8px 24px rgba(5, 150, 105, 0.4)
  - transform: translateY(-2px)

#### Botón Secundario (Cancelar)
- background: #F3F4F6
- color: #374151
- border: 1px solid #D1D5DB
- padding: 16px 24px
- border-radius: 8px
- fontSize: 14px
- fontWeight: 700
- :hover:
  - background: #E5E7EB

### Link de Cancelar (Pie de Página)

```
"Cancelar y volver al sistema RDAM"
```

- position: absolute
- bottom: 32px
- left: 50%
- transform: translateX(-50%)
- fontSize: 13px
- color: #71767A
- font-weight: 500
- text-decoration: none
- :hover: color: #0F4A7C

## Página de Error

Mismo layout split-screen con colores de error:

### Branding (Error)
- background: linear-gradient(135deg, #DC2626, #991B1B)

### Contenido de Error
- error-icon: "!"
- error-title: color #DC2626
- error-message: fontSize 14px, color #6B7280
- error-code: background #FEF2F2, border-left 4px solid #DC2626
- Botones: Reintentar (rojo) y Volver (gris)

## Paleta de Colores

### Primarios
- Primary Dark: #0F4A7C (branding sidebar)
- Primary Dark Hover: #0A3456 (gradient)

### Secundarios (Success/Confirmation)
- Success: #059669 (botón confirmar pago)
- Success Dark: #047857 (hover)

### De Error
- Error: #DC2626 (página de error, título)
- Error Dark: #991B1B (fondo gradient error)

### Neutrales
- White: #FFFFFF
- Text Dark: #1B1B1B, #1F2937
- Text Medium: #374151, #6B7280
- Text Light: #A9AEB1
- Background Light: #F3F4F6, #FAFBFC, #F0F4F9
- Border: #D1D5DB

## Tipografía

Font: 'Roboto' con fallback a system fonts

### Escalas
- Título Principal (h1): 32px, fontWeight 700
- Título Secundario (h5): 24px, fontWeight 700
- Subtítulos: 14px, fontWeight 400
- Labels: 12px, fontWeight 600
- Body: 14px, fontWeight 400
- Caption: 13px, fontWeight 500

## Responsive Design

### Desktop (md+)
- Split-screen 50/50
- Branding visible
- Layout óptimo

### Tablet/Mobile (xs-sm)
- Branding oculto (display: none)
- Solo formulario en pantalla completa
- Font sizes reducidos
- Padding: 24px (vs 40px en desktop)

### Breakpoints
- md: 1024px (hasta aquí se oculta branding)
- sm: 600px (tablet)
- xs: < 600px (mobile)

## Animaciones

### Slide In Right
```
@keyframes slideInRight {
  from { opacity: 0; transform: translateX(20px); }
  to { opacity: 1; transform: translateX(0); }
}
```
- Duración: 0.4s
- Timing: ease-out
- Aplicado al form-wrapper

### Transiciones de Botones
- Duración: 0.2s
- On hover: transform translateY(-2px)
- On active: translateY(0)

## Sombras (MUI-like)

### Sombra Primaria (Botón Confirmar)
`box-shadow: 0 4px 12px rgba(5, 150, 105, 0.3)`
On hover: `0 8px 24px rgba(5, 150, 105, 0.4)`

### Sombra Secundaria (Error)
`box-shadow: 0 4px 12px rgba(220, 38, 38, 0.3)`
On hover: `0 8px 24px rgba(220, 38, 38, 0.4)`

## Bordes Redondeados

- mayor elementos: border-radius: 8px
- Contenedores principales: 12px
- Inputs/campos: 8px
- Buttons: 8px

## Espaciado (Gap/Padding/Margin)

### Vertical
- Entre secciones principales: 32px
- Entre elementos: 16-24px
- Padding interno: 24px-40px

### Horizontal
- Gap entre botones: 12px
- Padding interno: 16px

## Características de Accesibilidad

- Focus states en inputs (box-shadow en focus)
- Suficiente contraste de colores
- Touch targets generosos (min 44px)
- Texto de botones claro uppercase

## Compatibilidad

- Chrome/Edge (latest)
- Firefox (latest)
- Safari (latest)
- Mobile browsers

## CSS Variables Utilizadas (Emuladas)

```css
--primary: #0F4A7C
--primary-dark: #0A3456
--success: #059669
--success-dark: #047857
--error: #DC2626
--error-dark: #991B1B
--text-primary: #1B1B1B
--text-secondary: #6B7280
--bg-light: #F3F4F6
--border-light: #D1D5DB
--radius-sm: 8px
--radius-lg: 12px
```

## Mejoras Futuras

1. Agregar zoom/scale animation en logo branding
2. Agregarincidenciaas de campo al focus (outline color)
3. Loading states en botones
4. Toast/snackbar para mensajes
5. Validación visual de campos
6. Icono de candado/seguridad
7. Progress indicator de pasos

## Testing CSS Responsivo

```bash
# Mobile (375px)
# Tablet (768px)
# Desktop (1440px)
```

Ver archivo TESTING.md para tests manuales de responsive.

---

**Última actualización:** 2026-03-18
**Versión del diseño:** MUI v5
**Status:** Implementado y responsive
