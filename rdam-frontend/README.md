# RDAM — Sistema de Certificados Digitales (Frontend)

Sistema web para la gestión integral de certificados digitales del Registro de Datos de Animales Mayores. Permite a ciudadanos solicitar certificados, a funcionarios internos revisarlos y emitirlos, y a administradores gestionar usuarios, catálogos y reportes.

## Tecnologías principales

| Tecnología | Versión |
|---|---|
| React | 19 |
| Vite | 8 |
| Material UI (MUI) | 7 |
| MUI X Data Grid | 8 |
| React Router | 7 |
| TanStack React Query | 5 |
| Axios | 1.x |
| Tailwind CSS | 3.4 |
| Day.js | 1.x |

## Pre-requisitos

- **Node.js** >= 20.x (LTS recomendado)
- **npm** >= 9.x
- Backend del proyecto corriendo en `http://localhost:8080` (ver repositorio `rdam-backend`)

## Instalación y ejecución

```bash
# 1. Clonar el repositorio
git clone <url-del-repositorio>
cd rdam-frontend

# 2. Copiar las variables de entorno
cp .env.example .env

# 3. Instalar dependencias
npm install

# 4. Levantar el servidor de desarrollo
npm run dev
```

La aplicación estará disponible en `http://localhost:5173`.

> **Nota:** El servidor de desarrollo de Vite incluye un proxy configurado que redirige las rutas `/api/*` y `/auth/*` a `http://localhost:8080`, por lo que no es necesario configurar CORS en desarrollo local.

## Variables de entorno

Archivo `.env.example`:

```env
VITE_API_URL=http://localhost:8080
```

| Variable | Descripción |
|---|---|
| `VITE_API_URL` | URL base del backend API |

## Scripts disponibles

| Script | Comando | Descripción |
|---|---|---|
| `dev` | `npm run dev` | Inicia el servidor de desarrollo con Hot Module Replacement (HMR) |
| `build` | `npm run build` | Genera el build de producción optimizado en la carpeta `dist/` |
| `preview` | `npm run preview` | Sirve localmente el build de producción para verificación previa al deploy |
| `lint` | `npm run lint` | Ejecuta ESLint para análisis estático del código |

## Estructura del proyecto

```
src/
├── api/                    # Capa de integración con el backend
│   ├── axios.js            # Configuración de Axios con interceptores
│   └── endpoints/          # Endpoints organizados por dominio
├── components/             # Componentes reutilizables
│   ├── layout/             # AppLayout, Sidebar, TopBar
│   └── common/             # Componentes genéricos compartidos
├── context/                # React Context (AuthContext)
├── hooks/                  # Custom hooks (useAuth)
├── pages/                  # Páginas organizadas por rol
│   ├── auth/               # Login, OTP, Registro
│   ├── ciudadano/          # Solicitudes, Nueva solicitud, Detalle
│   ├── interno/            # Bandeja, Revisión, Emisión, Historial
│   ├── admin/              # Dashboard, Usuarios, Catálogos, Reportes
│   └── public/             # Verificación pública de certificados
├── router/                 # Configuración de rutas y guards
├── utils/                  # Funciones utilitarias y constantes
├── theme.js                # Tema personalizado de MUI
├── App.jsx                 # Componente raíz
└── main.jsx                # Punto de entrada con providers
```

## Roles del sistema

| Rol | Ruta base | Descripción |
|---|---|---|
| Ciudadano | `/ciudadano/*` | Crea solicitudes, adjunta documentación, realiza pagos y descarga certificados |
| Interno | `/interno/*` | Revisa solicitudes, aprueba/rechaza, emite certificados |
| Admin | `/admin/*` | Gestiona usuarios, catálogos, visualiza dashboard y reportes |

## Deploy con Docker

```bash
docker build --build-arg VITE_API_URL=https://api.ejemplo.com -t rdam-frontend .
docker run -p 80:80 rdam-frontend
```

El contenedor usa Nginx como servidor web con proxy reverso hacia el backend.
