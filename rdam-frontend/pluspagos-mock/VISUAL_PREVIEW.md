# PREVIEW VISUAL - Material UI v5 Layout

Vista previa ASCII de cómo se ve el nuevo diseño refactorizado.

## Desktop View (1440px)

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  ╔═══════════════════════╦═══════════════════════════════╗ │
│  ║                       ║                               ║ │
│  ║   BRANDING COLUMN     ║    FORM COLUMN (Right)        ║ │
│  ║   (40-50%)            ║    (50-60%)                   ║ │
│  ║                       ║                               ║ │
│  ║  Background:          ║  Background: #FFFFFF          ║ │
│  ║  #0F4A7C gradient     ║                               ║ │
│  ║                       ║  ┌─────────────────────────┐  ║ │
│  ║  Textos blancos       ║  │ Detalles del Pago       │  ║ │
│  ║                       ║  │ (Typography h5)         │  ║ │
│  ║  "Portal de Pagos     ║  │ Sistema de Recaudación  │  ║ │
│  ║   Seguro"             ║  │ (Typography caption)    │  ║ │
│  ║                       ║  │                         │  ║ │
│  ║  "Estás a un paso"    ║  │ ┌─────────────────────┐ │  ║ │
│  ║                       ║  │ │ Monto a pagar       │ │  ║ │
│  ║  Footer:              ║  │ │ $5000.00 ARS        │ │  ║ │
│  ║  "Mock de Integración"║  │ │ (green #059669)     │ │  ║ │
│  ║                       ║  │ └─────────────────────┘ │  ║ │
│  ║                       ║  │                         │  ║ │
│  ║                       ║  │ ID: SOL-123             │  ║ │
│  ║                       ║  │ Descripción: ...        │  ║ │
│  ║                       ║  │                         │  ║ │
│  ║                       ║  │ ┌─────────┬────────────┐ │  ║ │
│  ║                       ║  │ │ CONFIRMAR│ CANCELAR  │ │  ║ │
│  ║                       ║  │ │ (Green)  │ (Gray)   │ │  ║ │
│  ║                       ║  │ └─────────┴────────────┘ │  ║ │
│  ║                       ║  │                         │  ║ │
│  ║                       ║  │ Link de cancelación:    │  ║ │
│  ║                       ║  │ "Cancelar y volver"     │  ║ │
│  ║                       ║  └─────────────────────────┘  ║ │
│  ║                       ║                               ║ │
│  ╚═══════════════════════╩═══════════════════════════════╝ │
│                                                             │
└─────────────────────────────────────────────────────────────┘

Colores Activos:
├─ Branding: linear-gradient(135deg, #0F4A7C, #0A3456)
├─ Monto: #059669 (success)
├─ Botón Primario: #059669 (con shadow)
├─ Botón Secundario: #F3F4F6 (con border)
└─ Textos: #1B1B1B, #6B7280, #A9AEB1
```

## Tablet View (768px)

```
┌──────────────────────────────┐
│                              │
│  ┌────────────────────────┐  │
│  │ Detalles del Pago      │  │
│  │                        │  │
│  │ Sistema de Recaudación │  │
│  │                        │  │
│  │ ┌──────────────────────┐ │
│  │ │ Monto a pagar        │ │
│  │ │ $5000.00 ARS         │ │
│  │ │ (Green #059669)      │ │
│  │ └──────────────────────┘ │
│  │                        │  │
│  │ ID: SOL-123            │  │
│  │ Descripción: ...       │  │
│  │                        │  │
│  │ ┌────────┬───────────┐ │  │
│  │ │CONFIRMAR│ CANCELAR │ │  │
│  │ └────────┴───────────┘ │  │
│  │                        │  │
│  │ Link cancelación       │  │
│  └────────────────────────┘  │
│                              │
│ (Branding oculto)            │
└──────────────────────────────┘

Cambios vs Desktop:
├─ Branding: display: none
├─ Formulario: fullwidth
├─ maxWidth: 450px (centrado)
└─ Padding: 40px
```

## Mobile View (375px)

```
┌────────────────────────┐
│                        │
│  ┌──────────────────┐  │
│  │ Detalles del Pago│  │
│  │                  │  │
│  │ Sistema de Recau-│  │
│  │ dacion...        │  │
│  │                  │  │
│  │ ┌────────────────┐ │
│  │ │ Monto          │ │
│  │ │ $5000.00 ARS   │ │
│  │ │ (Green)        │ │
│  │ └────────────────┘ │
│  │                  │  │
│  │ SOL-123          │  │
│  │ Desc...          │  │
│  │                  │  │
│  │ ┌──┬────────────┐ │
│  │ │CF│  CANCELAR │ │
│  │ └──┴────────────┘ │
│  │                  │  │
│  │ Cancelar y volver│  │
│  └──────────────────┘  │
│                        │
└────────────────────────┘

Cambios vs Tablet:
├─ Padding: 24px (reducido)
├─ Font sizes: menores
├─ Full responsive
├─ Buttons: full width
└─ Touch targets: 44px+
```

## Página de Error - Desktop

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  ╔═══════════════════════╦═══════════════════════════════╗ │
│  ║                       ║                               ║ │
│  ║   BRANDING ERROR      ║    ERROR INFO (Right)        ║ │
│  ║   (Red #DC2626)       ║                               ║ │
│  ║   gradient to         ║  Background: #FFFFFF          ║ │
│  ║   #991B1B             ║                               ║ │
│  ║                       ║  ┌─────────────────────────┐  ║ │
│  ║  "Portal de Pagos"    ║  │ ! (error icon)          │  ║ │
│  ║  (mismo texto)        ║  │ Error en Desencriptacion│  ║ │
│  ║                       ║  │ (title - red #DC2626)   │  ║ │
│  ║  Footer               ║  │                         │  ║ │
│  ║                       ║  │ No se pudo procesar...  │  ║ │
│  ║                       ║  │ (message - gray)        │  ║ │
│  ║                       ║  │                         │  ║ │
│  ║                       ║  │ 2026-03-18T10:30:00Z    │  ║ │
│  ║                       ║  │ (timestamp code)        │  ║ │
│  ║                       ║  │                         │  ║ │
│  ║                       ║  │ ┌─────────┬──────────┐  │  ║ │
│  ║                       ║  │ │REINTENTAR│ VOLVER  │  │  ║ │
│  ║                       ║  │ │ (Red)    │ (Gray)  │  │  ║ │
│  ║                       ║  │ └─────────┴──────────┘  │  ║ │
│  ║                       ║  └─────────────────────────┘  ║ │
│  ║                       ║                               ║ │
│  ╚═══════════════════════╩═══════════════════════════════╝ │
│                                                             │
└─────────────────────────────────────────────────────────────┘

Colores Error:
├─ Branding: linear-gradient(135deg, #DC2626, #991B1B)
├─ Título: #DC2626
├─ Botón Reintentar: #DC2626 (con shadow)
├─ Background Error Code: #FEF2F2
└─ Border Left Error Code: 4px solid #DC2626
```

## Componentes Detallados

### Branding Column (Izquierda)
```
Display: flex
Direction: column
Justify: center
Gap: 48px
Width: 50%
Background: gradient #0F4A7C → #0A3456
Padding: 60px

┌─────────────────────────┐
│ ╳ Portal de Pagos       │ (h3, white)
│   Seguro                │
│                         │
│ ╳ Estás a un paso de    │ (body1, rgba white 0.8)
│   completar tu trámite  │
│   en RDAM               │
│                         │
│                    [^]  │ (footer caption, rgba white 0.5)
│   Mock de Integración   │
└─────────────────────────┘
```

### Form Column (Derecha)
```
Display: flex
Direction: column
Justify: center
Align: center
Flex: 1
Background: white
Padding: 40px

┌──────────────────────────┐
│ Form Wrapper             │ (max-width: 450px)
│ ┌──────────────────────┐ │
│ │ Detalles del Pago    │ │ (h5, #1B1B1B)
│ │ Sistema de...        │ │ (caption, #A9AEB1)
│ │                      │ │
│ │ ┌──────────────────┐ │ │
│ │ │ Monto           │ │ │
│ │ │ $5000.00 ARS    │ │ │ (h4, #059669)
│ │ └──────────────────┘ │ │
│ │                      │ │
│ │ ┌──────────────────┐ │ │
│ │ │ ID: SOL-123      │ │ │
│ │ │ Desc: ...        │ │ │
│ │ └──────────────────┘ │ │
│ │                      │ │
│ │ ┌────────┬──────────┐│ │
│ │ │CONFIRMAR│ CANCELAR││ │
│ │ └────────┴──────────┘│ │
│ │                      │ │
│ └──────────────────────┘ │
│ Cancelar y volver        │ (link subtle)
└──────────────────────────┘
```

### Amount Highlight Box
```
Background: #F0F4F9
Border-radius: 12px
Padding: 24px
Margin-bottom: 32px

┌──────────────────────────┐
│ MONTO A PAGAR            │ (label 12px, #6B7280)
│ $5000.00 ARS             │ (value 36px, #059669, bold)
└──────────────────────────┘
```

### Transaction Info Box
```
Background: #FAFBFC
Border-left: 4px solid #0F4A7C
Border-radius: 8px
Padding: 16px
Margin-bottom: 32px

┌──────────────────────────┐
│ ID DE TRANSACCION        │ (label 12px)
│ SOL-123                  │ (value 13px)
│                          │
│ DESCRIPCION              │ (label 12px)
│ Solicitud RID-001...     │ (value 13px)
└──────────────────────────┘
```

### Button Group
```
Display: flex
Gap: 12px
Margin-bottom: 24px

┌────────────┬───────────────┐
│ CONFIRMAR  │   CANCELAR    │
│  (Primary) │  (Secondary)  │
├────────────┼───────────────┤
│ bg: #0596  │ bg: #F3F4F6   │
│ color: wh  │ color: gray   │
│ shadow: 0  │ border: gray  │
│    4px 12  │               │
│ rgba(5...) │               │
└────────────┴───────────────┘
```

## Animaciones

### Entrada (Opcional)
```
@keyframes slideInRight:
  0%:   opacity: 0, translateX(20px)
  100%: opacity: 1, translateX(0)
  Duración: 0.4s
  Timing: ease-out
  Aplicado: form-wrapper
```

### Hover Botón Primario
```
Before:
  background: #059669
  box-shadow: 0 4px 12px rgba(5,150,105,0.3)
  
After:
  background: #047857 (darker)
  box-shadow: 0 8px 24px rgba(5,150,105,0.4)
  transform: translateY(-2px)
```

### Hover Link Cancelación
```
Before:
  color: #71767A
  
After:
  color: #0F4A7C (primary dark)
  text-decoration: none
```

## Especificaciones de Tipografía

### Escala Roboto
```
Título Principal (h1):
├─ Font: Roboto
├─ Size: 32px
├─ Weight: 700
├─ Letter-spacing: -0.5px
└─ Line-height: 1.2

Título Secundario (h5):
├─ Size: 24px
├─ Weight: 700
└─ Letter-spacing: -0.5px

Body (p):
├─ Size: 14px
├─ Weight: 400
└─ Line-height: 1.6

Label:
├─ Size: 12px
├─ Weight: 600
└─ Letter-spacing: 0.5px

Caption:
├─ Size: 13px
├─ Weight: 400-500
└─ Line-height: 1.5
```

## Espaciado (Margin/Padding/Gap)

### Horizontal
```
Padding Branding: 60px
Padding Form: 40px
Padding Box: 24px / 16px
Input Padding: 12px 16px
Button Padding: 16px 24px
Gap Buttons: 12px
Gap Branding Content: 48px
```

### Vertical
```
Gap Main Sections: 32px
Gap Mid Sections: 24px
Gap Small: 16px / 12px
Button Height: ~56px (padding 16px)
Form Wrapper padding-bottom: 24px (para link)
```

## Responsive Transitions

### Desktop → Tablet (1024px)
```
Branding: display none
Grid: flex-direction column
Form column: width 100%
Padding: 40px (mismo)
```

### Tablet → Mobile (600px)
```
Padding: 24px (reducido)
H5: 20px (vs 24px)
H4: 28px (vs 36px)
Gap: 32px (vs 48px)
Font sizes: -2px en general
```

---

**Nota:** Este es un preview ASCII. El diseño real tiene:
- Sombras suaves (0 4px 12px)
- Anti-aliasing de tipografía
- Gradientes suaves
- Transiciones fluidas
- Precision de píxeles

Ver live: docker compose up -d pluspagos
URL: http://localhost:8081/pluspagos

---

**Última actualización:** 2026-03-18
**Status:** Design implementado y responsive
