# RDAM — Sistema de Certificados Digitales

Sistema integral para la gestión y emisión de certificados digitales del Registro de Datos de Animales Mayores. Desarrollado como proyecto del Campus de Verano 2026 — i2T Software Factory.

## Arquitectura

| Servicio | Tecnología | Puerto |
|---|---|---|
| **Frontend** | React 19 + Vite 8 + MUI 7 | `80` |
| **Backend** | Spring Boot 3 + Java 17 | `8080` |
| **Base de datos** | PostgreSQL 15 | `5432` |
| **Pasarela de pagos** | Node.js (mock PlusPagos) | `8081` |

## Pre-requisitos

- **Docker** >= 24.x
- **Docker Compose** >= 2.x (incluido en Docker Desktop)

> No es necesario tener Java, Node.js ni PostgreSQL instalados. Todo corre dentro de contenedores.

## Levantar el proyecto

### 1. Configurar variables de entorno

```bash
cp rdam-backend/.env.example rdam-backend/.env
```

Editar `rdam-backend/.env` y completar los valores obligatorios:

```env
DB_PASSWORD=una_password_segura
MAIL_USERNAME=tu_email@gmail.com
MAIL_PASSWORD=tu_app_password_de_gmail
JWT_SECRET=una-clave-de-al-menos-32-caracteres
PLUSPAGOS_MERCHANT_ID=RDAM-MOCK-001
PLUSPAGOS_SECRET=mock-secret-desarrollo
PLUSPAGOS_URL=http://pluspagos:8081/pluspagos
```

> **Nota sobre MAIL_PASSWORD:** Se necesita un App Password de Gmail, no la contraseña de la cuenta. Se genera en: Google Account > Seguridad > Contraseñas de aplicaciones.

### 2. Construir y levantar

```bash
docker compose up --build -d
```

La primera ejecución tarda unos minutos mientras descarga imágenes y compila ambos proyectos. Las siguientes ejecuciones son mucho más rápidas gracias al cache de Docker.

### 3. Verificar que todo esté corriendo

```bash
docker compose ps
```

Los 4 servicios deben estar en estado `healthy` o `running`:

```
NAME        STATUS
db          healthy
app         healthy
frontend    running
pluspagos   healthy
```

### 4. Acceder a la aplicación

| Recurso | URL |
|---|---|
| Aplicación web | http://localhost |
| API REST (Postman) | http://localhost:8080 |
| Mock PlusPagos | http://localhost:8081 |

## Comandos útiles

```bash
# Ver logs del backend en tiempo real
docker compose logs -f app

# Ver logs de todos los servicios
docker compose logs -f

# Reiniciar solo el backend
docker compose restart app

# Apagar todo (conservando datos de la base)
docker compose down

# Apagar todo y borrar la base de datos (reset completo)
docker compose down -v
```

## Estructura del proyecto

```
RDAM/
├── docker-compose.yml              # Orquestador de los 4 servicios
├── rdam-backend/
│   ├── Dockerfile                  # Build multi-stage: Maven → JRE
│   ├── .env.example                # Template de variables de entorno
│   ├── db-init/
│   │   └── RDAM-Script-DDL.sql     # Esquema de base de datos (se ejecuta automáticamente)
│   ├── pom.xml
│   └── src/                        # Código fuente Java (Spring Boot)
├── rdam-frontend/
│   ├── Dockerfile                  # Build multi-stage: Node → Nginx
│   ├── nginx.conf                  # Proxy reverso hacia el backend
│   ├── package.json
│   ├── src/                        # Código fuente React
│   └── pluspagos-mock/
│       ├── Dockerfile
│       └── server.js               # Mock de pasarela de pagos
```

## Roles del sistema

| Rol | Acceso | Funcionalidades |
|---|---|---|
| **Ciudadano** | `/ciudadano/*` | Crear solicitudes, adjuntar documentación, pagar y descargar certificados |
| **Interno** | `/interno/*` | Revisar solicitudes, aprobar/rechazar, emitir certificados |
| **Admin** | `/admin/*` | Dashboard de métricas, gestión de usuarios, catálogos y reportes |

## Troubleshooting

**El backend no arranca (exit code 1):**
Verificar que el `.env` tenga todos los valores completos. Consultar logs con `docker compose logs app`.

**La base de datos no inicializa:**
Si se modificó el DDL después de la primera ejecución, es necesario borrar el volumen: `docker compose down -v && docker compose up --build -d`.

**El frontend muestra errores de red:**
Verificar que el backend esté `healthy` con `docker compose ps`. El frontend depende del proxy Nginx hacia el backend.
