# Plan de Pruebas Manuales — RDAM Frontend

**Proyecto:** RDAM — Sistema de Certificados Digitales
**Versión:** 1.0
**Fecha:** 19/03/2026
**Ambiente:** Desarrollo local (`http://localhost:5173`)
**Pre-requisito general:** Backend corriendo en `http://localhost:8080` con base de datos inicializada.

---

## 1. Módulo de Autenticación

| ID | Módulo | Descripción | Precondiciones | Pasos a ejecutar | Resultado Esperado | Estado |
|---|---|---|---|---|---|---|
| AUTH-01 | Autenticación | Inicio de sesión exitoso con credenciales válidas | Usuario registrado en el sistema | 1. Navegar a `/login` 2. Ingresar email y contraseña válidos 3. Click en "Iniciar sesión" 4. Ingresar código OTP recibido por email 5. Click en "Verificar" | Se redirige al home correspondiente al rol del usuario (Admin → `/admin`, Interno → `/interno/bandeja`, Ciudadano → `/ciudadano/solicitudes`) | |
| AUTH-02 | Autenticación | Validación de campos vacíos en login | Ninguna | 1. Navegar a `/login` 2. Dejar ambos campos vacíos 3. Click en "Iniciar sesión" | Se muestran mensajes de validación indicando que los campos son obligatorios. No se envía la petición al backend | |
| AUTH-03 | Autenticación | Validación de credenciales inválidas | Ninguna | 1. Navegar a `/login` 2. Ingresar un email válido y una contraseña incorrecta 3. Click en "Iniciar sesión" | Se muestra un mensaje de error indicando credenciales inválidas. El usuario permanece en la pantalla de login | |
| AUTH-04 | Autenticación | Redirección según rol — Admin | Usuario con rol `admin` registrado | 1. Iniciar sesión con usuario admin 2. Completar verificación OTP | Se redirige a `/admin` (Dashboard Administrativo) | |
| AUTH-05 | Autenticación | Redirección según rol — Interno | Usuario con rol `interno` registrado | 1. Iniciar sesión con usuario interno 2. Completar verificación OTP | Se redirige a `/interno/bandeja` (Bandeja de trabajo) | |
| AUTH-06 | Autenticación | Redirección según rol — Ciudadano | Usuario con rol `ciudadano` registrado | 1. Iniciar sesión con usuario ciudadano 2. Completar verificación OTP | Se redirige a `/ciudadano/solicitudes` (Mis solicitudes) | |
| AUTH-07 | Autenticación | Acceso a ruta protegida sin sesión | No haber iniciado sesión | 1. Navegar directamente a `/admin` sin estar autenticado | Se redirige automáticamente a `/login` | |
| AUTH-08 | Autenticación | Registro de nuevo ciudadano | Ninguna | 1. Navegar a `/register` 2. Completar todos los campos del formulario 3. Click en "Registrarse" | Se crea la cuenta y se muestra confirmación o se redirige al login | |

---

## 2. Dashboard Administrativo

| ID | Módulo | Descripción | Precondiciones | Pasos a ejecutar | Resultado Esperado | Estado |
|---|---|---|---|---|---|---|
| DASH-01 | Dashboard | Visualización de métricas generales | Sesión iniciada como admin. Existen solicitudes en el sistema | 1. Iniciar sesión como admin 2. Verificar que se carga la página `/admin` | Se muestran las tarjetas de métricas con datos numéricos (total de solicitudes, pendientes, emitidas, etc.) | |
| DASH-02 | Dashboard | Navegación por Sidebar — Usuarios | Sesión iniciada como admin | 1. Hacer click en "Usuarios" en el Sidebar | Se navega a `/admin/usuarios` y se muestra la bandeja de usuarios | |
| DASH-03 | Dashboard | Navegación por Sidebar — Catálogos | Sesión iniciada como admin | 1. Hacer click en "Catálogos" en el Sidebar | Se navega a `/admin/catalogos` y se muestra la gestión de catálogos | |
| DASH-04 | Dashboard | Navegación por Sidebar — Reportes | Sesión iniciada como admin | 1. Hacer click en "Reportes" en el Sidebar | Se navega a `/admin/reportes` y se muestra la página de reportes | |
| DASH-05 | Dashboard | Responsive — Sidebar se oculta en móvil | Sesión iniciada como admin | 1. Reducir el ancho del navegador a < 768px (o usar DevTools en modo mobile) | El Sidebar fijo desaparece y se muestra un ícono de menú hamburguesa en el TopBar | |
| DASH-06 | Dashboard | Responsive — Drawer móvil funcional | Sesión iniciada como admin. Viewport en modo mobile | 1. Hacer click en el ícono de menú hamburguesa 2. Seleccionar una opción del menú | Se abre un Drawer lateral con las opciones de navegación. Al seleccionar una opción, se navega a la ruta correcta y el Drawer se cierra | |
| DASH-07 | Dashboard | Responsive — Métricas se adaptan | Sesión iniciada como admin | 1. Reducir el ancho del navegador progresivamente | Las tarjetas de métricas se reorganizan en una sola columna sin desbordamiento horizontal ni contenido cortado | |

---

## 3. Bandeja de Usuarios (Admin)

| ID | Módulo | Descripción | Precondiciones | Pasos a ejecutar | Resultado Esperado | Estado |
|---|---|---|---|---|---|---|
| USR-01 | Usuarios | Carga del DataGrid de usuarios | Sesión iniciada como admin. Existen usuarios registrados | 1. Navegar a `/admin/usuarios` | Se muestra el DataGrid con la lista de usuarios, incluyendo columnas de nombre, email, rol y estado | |
| USR-02 | Usuarios | Búsqueda de usuario por nombre o email | Sesión iniciada como admin. DataGrid cargado | 1. Escribir un término de búsqueda en el campo de filtro/búsqueda 2. Esperar resultado | El DataGrid filtra y muestra solo los usuarios que coinciden con el criterio de búsqueda | |
| USR-03 | Usuarios | Filtro por rol | Sesión iniciada como admin. DataGrid cargado | 1. Seleccionar un filtro de rol (admin, interno, ciudadano) | El DataGrid muestra solo los usuarios con el rol seleccionado | |
| USR-04 | Usuarios | Chip de estado "Activo" | Sesión iniciada como admin. Existen usuarios activos | 1. Navegar a `/admin/usuarios` 2. Localizar un usuario con estado activo | Se muestra un Chip de color verde con el texto "Activo" | |
| USR-05 | Usuarios | Chip de estado "Inactivo" | Sesión iniciada como admin. Existen usuarios inactivos | 1. Navegar a `/admin/usuarios` 2. Localizar un usuario con estado inactivo | Se muestra un Chip de color gris con el texto "Inactivo" | |
| USR-06 | Usuarios | Chip de estado "Bloqueado" | Sesión iniciada como admin. Existen usuarios bloqueados | 1. Navegar a `/admin/usuarios` 2. Localizar un usuario con estado bloqueado | Se muestra un Chip de color rojo con el texto "Bloqueado" | |
| USR-07 | Usuarios | Paginación del DataGrid | Sesión iniciada como admin. Más de 10 usuarios registrados | 1. Navegar a `/admin/usuarios` 2. Verificar controles de paginación en la parte inferior del DataGrid 3. Hacer click en "Siguiente página" | Se muestran los siguientes registros y los controles de paginación reflejan la página actual | |
| USR-08 | Usuarios | Edición de usuario | Sesión iniciada como admin | 1. Hacer click en un usuario de la lista 2. Modificar un campo (ej: rol o estado) 3. Guardar cambios | Los cambios se persisten y el DataGrid refleja los valores actualizados | |

---

## 4. Flujo de Emisión de Certificados

### 4a. Rol Interno — Emisión

| ID | Módulo | Descripción | Precondiciones | Pasos a ejecutar | Resultado Esperado | Estado |
|---|---|---|---|---|---|---|
| EMIT-01 | Emisión (Interno) | Visualizar solicitud con estado "Pagada" en bandeja | Sesión iniciada como interno. Existe al menos una solicitud con estado PAGADO | 1. Navegar a `/interno/bandeja` 2. Localizar la solicitud con estado "Pagado" | Se muestra la solicitud en la bandeja con un Chip verde indicando estado "Pagado" | |
| EMIT-02 | Emisión (Interno) | Tomar solicitud para revisión | Sesión iniciada como interno. Solicitud en estado PAGADO visible | 1. Hacer click en la solicitud con estado "Pagado" 2. Click en "Tomar solicitud" | La solicitud queda asignada al funcionario actual y se navega a la vista de revisión (`/interno/solicitudes/:id`) | |
| EMIT-03 | Emisión (Interno) | Aprobar solicitud | Sesión iniciada como interno. Solicitud tomada y en revisión | 1. Revisar los datos de la solicitud y los adjuntos 2. Click en "Aprobar" | La solicitud cambia su estado a "Aprobado". Se actualiza el historial de estados | |
| EMIT-04 | Emisión (Interno) | Emitir certificado | Sesión iniciada como interno. Solicitud aprobada | 1. Navegar a `/interno/solicitudes/:id/emitir` 2. Verificar los datos del certificado 3. Click en "Emitir certificado" | Se genera el certificado digital. La solicitud cambia a estado "Publicado". Se muestra un mensaje de confirmación | |
| EMIT-05 | Emisión (Interno) | Rechazar solicitud con motivo | Sesión iniciada como interno. Solicitud tomada | 1. En la vista de revisión, click en "Rechazar" 2. Ingresar el motivo del rechazo 3. Confirmar | La solicitud cambia a estado "Rechazado". El motivo queda registrado en el historial | |
| EMIT-06 | Emisión (Interno) | Reasignar solicitud a otro funcionario | Sesión iniciada como interno. Solicitud tomada | 1. En la vista de revisión, click en "Reasignar" 2. Seleccionar el funcionario destino 3. Confirmar | La solicitud se reasigna al funcionario seleccionado y desaparece de la bandeja del funcionario actual | |

### 4b. Rol Ciudadano — Descarga de certificado

| ID | Módulo | Descripción | Precondiciones | Pasos a ejecutar | Resultado Esperado | Estado |
|---|---|---|---|---|---|---|
| CERT-01 | Certificado (Ciudadano) | Visualizar solicitud con certificado listo | Sesión iniciada como ciudadano. Existe una solicitud propia con estado PUBLICADO | 1. Navegar a `/ciudadano/solicitudes` 2. Localizar la solicitud con estado "Publicado" | Se muestra la solicitud con un Chip azul indicando estado "Publicado" y la opción de ver el detalle | |
| CERT-02 | Certificado (Ciudadano) | Descargar certificado en PDF | Sesión iniciada como ciudadano. Solicitud en estado PUBLICADO | 1. Hacer click en la solicitud publicada para ver el detalle 2. Click en "Descargar PDF" | Se descarga el archivo PDF del certificado digital. El archivo se abre correctamente en un visor de PDF | |
| CERT-03 | Certificado (Ciudadano) | Verificación pública del certificado | Certificado emitido con token de verificación | 1. Navegar a `/verificar/:token` (URL pública) | Se muestra la información del certificado verificado: titular, tipo de certificado, fecha de emisión y estado de validez | |
| CERT-04 | Certificado (Ciudadano) | Crear nueva solicitud | Sesión iniciada como ciudadano | 1. Navegar a `/ciudadano/nueva` 2. Completar el formulario con tipo de certificado, circunscripción y datos requeridos 3. Adjuntar documentación 4. Click en "Enviar solicitud" | Se crea la solicitud exitosamente con estado "Pendiente". Aparece en la lista de "Mis solicitudes" | |
| CERT-05 | Certificado (Ciudadano) | Cancelar solicitud pendiente | Sesión iniciada como ciudadano. Solicitud en estado PENDIENTE | 1. Navegar al detalle de la solicitud pendiente 2. Click en "Cancelar solicitud" 3. Confirmar la acción | La solicitud cambia a estado "Cancelado" y ya no permite acciones adicionales | |

---

## Resumen de cobertura

| Módulo | Cantidad de casos | IDs |
|---|---|---|
| Autenticación | 8 | AUTH-01 a AUTH-08 |
| Dashboard Administrativo | 7 | DASH-01 a DASH-07 |
| Bandeja de Usuarios | 8 | USR-01 a USR-08 |
| Emisión de Certificados (Interno) | 6 | EMIT-01 a EMIT-06 |
| Certificados (Ciudadano) | 5 | CERT-01 a CERT-05 |
| **Total** | **34** | |

---

## Instrucciones para el tester

1. Completar la columna **Estado** con: `Pass`, `Fail` o `Blocked`.
2. Si el resultado es `Fail`, agregar una nota con el comportamiento observado.
3. Si el resultado es `Blocked`, indicar el motivo del bloqueo (ej: "Backend no disponible", "Datos de prueba insuficientes").
4. Ejecutar las pruebas en los navegadores: **Chrome** (última versión) y **Firefox** (última versión).
5. Para los casos de responsive (DASH-05 a DASH-07), usar las DevTools del navegador con viewport de **375x667** (iPhone SE) y **768x1024** (iPad).
