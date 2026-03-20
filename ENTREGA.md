# RDAM — Entrega Final

**Proyecto:** Sistema de Gestión y Emisión de Certificados Digitales
**Alumno:** Acevedo Alan
**Fecha:** 19/03/2026

---

## Repositorio y Release

- **Repositorio:** https://github.com/Acevedo-Alan/RDAM-Sistema-de-Gestion-y-Emision-de-Certificados-Digitales
- **Release v1.0.0:** https://github.com/Acevedo-Alan/RDAM-Sistema-de-Gestion-y-Emision-de-Certificados-Digitales/releases/tag/v1.0.0

## Cómo levantar el proyecto

> Requisito: tener Docker Desktop instalado.

```bash
git clone https://github.com/Acevedo-Alan/RDAM-Sistema-de-Gestion-y-Emision-de-Certificados-Digitales.git
cd RDAM-Sistema-de-Gestion-y-Emision-de-Certificados-Digitales
cp rdam-backend/.env.example rdam-backend/.env
# Editar rdam-backend/.env con las credenciales (ver sección siguiente)
docker compose up --build -d
```

La aplicación queda disponible en **http://localhost**.

### Variables de entorno requeridas (rdam-backend/.env)

| Variable | Valor sugerido para pruebas |
|---|---|
| `DB_PASSWORD` | `password123` |
| `MAIL_USERNAME` | *(email Gmail para envío de OTP)* |
| `MAIL_PASSWORD` | *(App Password de Gmail)* |
| `JWT_SECRET` | `clave-de-prueba-minimo-32-caracteres-ok` |
| `PLUSPAGOS_MERCHANT_ID` | `RDAM-MOCK-001` |
| `PLUSPAGOS_SECRET` | `mock-secret-desarrollo` |
| `PLUSPAGOS_URL` | `http://pluspagos:8081/pluspagos` |

> Para generar el App Password de Gmail: Google Account > Seguridad > Verificación en 2 pasos > Contraseñas de aplicaciones.

## Documentos entregados

| Documento | Descripción |
|---|---|
| `ENTREGA.md` | Este archivo — punto de entrada con instrucciones |
| `README.md` | Documentación técnica del frontend (tecnologías, estructura, scripts) |
| `PLAN_DE_PRUEBAS_UI.md` | 34 casos de prueba manuales organizados por módulo |

## Stack tecnológico

| Capa | Tecnologías |
|---|---|
| **Frontend** | React 19, Vite 8, MUI 7, React Router 7, TanStack Query, Tailwind CSS |
| **Backend** | Java 17, Spring Boot 3, Spring Security, JWT, JPA/Hibernate |
| **Base de datos** | PostgreSQL 15 |
| **Infraestructura** | Docker, Docker Compose, Nginx |

## Módulos del sistema

| Rol | Funcionalidades principales |
|---|---|
| **Ciudadano** | Registro, solicitud de certificados, pago, descarga de PDF |
| **Interno** | Bandeja de trabajo, revisión, aprobación/rechazo, emisión de certificados |
| **Admin** | Dashboard con métricas, gestión de usuarios, catálogos y reportes |
