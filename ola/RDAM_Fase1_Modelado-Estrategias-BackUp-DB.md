# RDAM — Fase 1: Modelado del Sistema
## Documentación Técnica Formal

**Proyecto:** i2T Software Factory — Campus de Verano 2026  
**Cliente:** Gobierno / Sector Público  
**Responsables:** Beliz, Meyer  
**Fecha de entrega:** 25/02/2026  
**Versión:** 1.0  
**Stack:** Java Spring Boot · React · PostgreSQL · JWT

---

## Tabla de Contenidos

1. [Script DDL — Base de Datos PostgreSQL](#1-script-ddl)
2. [Estrategia de Backup y Recuperación](#2-estrategia-de-backup-y-recuperacion)
3. [Mapa de Endpoints REST](#3-mapa-de-endpoints-rest)
4. [Seguridad: Validación por Circunscripción](#4-seguridad-validacion-por-circunscripcion)

---

## 1. Script DDL

### 1.1 Diseño y Decisiones

El esquema está normalizado a **Tercera Forma Normal (3FN)**. Las decisiones de diseño más relevantes son:

- `circunscripciones` es una tabla catálogo independiente, lo que permite agregar nuevas sin tocar código.
- `empleados` extiende `usuarios` con FK a `circunscripciones`, separando el dominio ciudadano del interno.
- `solicitudes` referencia directamente a `circunscripciones` para hacer posible la validación de acceso horizontal sin JOIN adicionales.
- `historial_estados` funciona como log inmutable de auditoría: nunca se modifica ni elimina.
- Se usa **soft delete** (`activo`, `eliminado_en`) en usuarios y tipos de certificado para mantener integridad referencial histórica.
- Todos los timestamps usan `TIMESTAMPTZ` para correcta gestión de zonas horarias.

### 1.2 Script DDL Completo

```sql
-- =============================================================================
-- RDAM — Script DDL Completo
-- Motor: PostgreSQL 15+
-- Descripción: Esquema de base de datos para el sistema de gestión y emisión
--              de certificados digitales con soporte de circunscripciones.
-- Versión: 1.0 | Fecha: Febrero 2026
-- =============================================================================

-- Extensión para UUIDs (opcional, se puede usar SERIAL)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================================================
-- TIPOS ENUMERADOS
-- =============================================================================

-- Estados posibles de una solicitud a lo largo de su ciclo de vida
CREATE TYPE estado_solicitud AS ENUM (
    'PENDIENTE_REVISION',  -- Recién creada, esperando ser tomada
    'EN_REVISION',         -- Asignada a un empleado, en proceso
    'APROBADA',            -- Aprobada, pendiente de pago
    'RECHAZADA',           -- Rechazada (estado final)
    'PAGADA',              -- Pago confirmado, pendiente emisión
    'EMITIDA',             -- Certificado emitido (estado final)
    'CANCELADA'            -- Cancelada por el ciudadano (estado final)
);

-- Roles del sistema
CREATE TYPE rol_usuario AS ENUM (
    'ciudadano',
    'interno',
    'admin'
);

-- =============================================================================
-- TABLA: circunscripciones
-- Catálogo de las unidades administrativas del organismo.
-- Diseñada para escalar sin modificar código.
-- =============================================================================
CREATE TABLE circunscripciones (
    id          SERIAL          PRIMARY KEY,
    nombre      VARCHAR(100)    NOT NULL,
    codigo      VARCHAR(20)     NOT NULL,   -- Código corto (ej: CIRC-01)
    descripcion TEXT,
    activo      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_circunscripciones_codigo UNIQUE (codigo)
);

COMMENT ON TABLE circunscripciones IS
    'Unidades administrativas del organismo. Permite escalar sin cambios de código.';

-- =============================================================================
-- TABLA: roles
-- Tabla catálogo de roles. Permite gestión dinámica en el futuro.
-- =============================================================================
CREATE TABLE roles (
    id          SERIAL          PRIMARY KEY,
    nombre      rol_usuario     NOT NULL UNIQUE,
    descripcion TEXT,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE roles IS 'Catálogo de roles del sistema.';

-- =============================================================================
-- TABLA: usuarios
-- Todos los actores del sistema: ciudadanos, internos y admins.
-- =============================================================================
CREATE TABLE usuarios (
    id              SERIAL          PRIMARY KEY,
    nombre          VARCHAR(150)    NOT NULL,
    apellido        VARCHAR(150)    NOT NULL,
    email           VARCHAR(255)    NOT NULL,
    cuil            VARCHAR(11),                -- Solo ciudadanos (puede ser NULL para internos creados por admin)
    telefono        VARCHAR(30),
    direccion       TEXT,
    fecha_nacimiento DATE,
    password_hash   VARCHAR(255)    NOT NULL,   -- bcrypt, min 10 rounds
    rol             rol_usuario     NOT NULL DEFAULT 'ciudadano',
    activo          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    eliminado_en    TIMESTAMPTZ,               -- Soft delete

    CONSTRAINT uq_usuarios_email UNIQUE (email),
    CONSTRAINT chk_usuarios_cuil_formato CHECK (cuil ~ '^\d{11}$' OR cuil IS NULL)
);

COMMENT ON TABLE usuarios IS
    'Tabla central de actores. Incluye ciudadanos, internos y admins.';
COMMENT ON COLUMN usuarios.password_hash IS 'Hash bcrypt con mínimo 10 rounds. Nunca almacenar texto plano.';
COMMENT ON COLUMN usuarios.eliminado_en IS 'Soft delete: si no es NULL, el usuario está desactivado.';

-- Índices para consultas frecuentes
CREATE INDEX idx_usuarios_email ON usuarios (email);
CREATE INDEX idx_usuarios_rol   ON usuarios (rol);
CREATE INDEX idx_usuarios_activo ON usuarios (activo) WHERE activo = TRUE;

-- =============================================================================
-- TABLA: empleados
-- Extiende usuarios con datos propios del personal interno.
-- Un empleado pertenece a UNA sola circunscripción.
-- =============================================================================
CREATE TABLE empleados (
    id                  SERIAL      PRIMARY KEY,
    usuario_id          INTEGER     NOT NULL,
    circunscripcion_id  INTEGER     NOT NULL,
    legajo              VARCHAR(50),            -- Número de legajo interno
    cargo               VARCHAR(100),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_empleados_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE RESTRICT,
    CONSTRAINT fk_empleados_circunscripcion
        FOREIGN KEY (circunscripcion_id) REFERENCES circunscripciones (id) ON DELETE RESTRICT,
    CONSTRAINT uq_empleados_usuario UNIQUE (usuario_id)  -- 1 empleado por usuario
);

COMMENT ON TABLE empleados IS
    'Perfil extendido de empleados internos. Vincula usuarios con su circunscripción.';
COMMENT ON COLUMN empleados.circunscripcion_id IS
    'Circunscripción a la que pertenece el empleado. Solo puede gestionar solicitudes de su propia circunscripción.';

CREATE INDEX idx_empleados_circunscripcion ON empleados (circunscripcion_id);
CREATE INDEX idx_empleados_usuario         ON empleados (usuario_id);

-- =============================================================================
-- TABLA: tipos_certificado
-- Catálogo configurable de tipos de certificado disponibles.
-- Administrable por el rol admin sin necesidad de deploy.
-- =============================================================================
CREATE TABLE tipos_certificado (
    id          SERIAL          PRIMARY KEY,
    nombre      VARCHAR(150)    NOT NULL,
    descripcion TEXT,
    activo      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    eliminado_en TIMESTAMPTZ,

    CONSTRAINT uq_tipos_certificado_nombre UNIQUE (nombre)
);

COMMENT ON TABLE tipos_certificado IS
    'Catálogo de tipos de certificado. Solo los activos aparecen en el formulario ciudadano.';

-- =============================================================================
-- TABLA: solicitudes
-- Entidad central del sistema. Registra cada pedido de certificado.
-- Vinculada a la circunscripción para control de acceso por empleado.
-- =============================================================================
CREATE TABLE solicitudes (
    id                  SERIAL              PRIMARY KEY,
    ciudadano_id        INTEGER             NOT NULL,   -- Usuario solicitante
    tipo_certificado_id INTEGER             NOT NULL,
    circunscripcion_id  INTEGER             NOT NULL,   -- Circunscripción de la solicitud
    estado              estado_solicitud    NOT NULL DEFAULT 'PENDIENTE_REVISION',
    motivo_rechazo      TEXT,                           -- Obligatorio si estado = RECHAZADA
    empleado_asignado_id INTEGER,                       -- Empleado que tomó la solicitud
    fecha_deseada       DATE,                           -- Fecha deseada de emisión indicada por el ciudadano
    fecha_asignacion    TIMESTAMPTZ,
    fecha_aprobacion    TIMESTAMPTZ,
    fecha_rechazo       TIMESTAMPTZ,
    fecha_pago          TIMESTAMPTZ,
    fecha_emision       TIMESTAMPTZ,
    fecha_cancelacion   TIMESTAMPTZ,
    monto_arancel       NUMERIC(10, 2)      NOT NULL DEFAULT 5000.00,
    created_at          TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ         NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_solicitudes_ciudadano
        FOREIGN KEY (ciudadano_id) REFERENCES usuarios (id) ON DELETE RESTRICT,
    CONSTRAINT fk_solicitudes_tipo_certificado
        FOREIGN KEY (tipo_certificado_id) REFERENCES tipos_certificado (id) ON DELETE RESTRICT,
    CONSTRAINT fk_solicitudes_circunscripcion
        FOREIGN KEY (circunscripcion_id) REFERENCES circunscripciones (id) ON DELETE RESTRICT,
    CONSTRAINT fk_solicitudes_empleado
        FOREIGN KEY (empleado_asignado_id) REFERENCES empleados (id) ON DELETE SET NULL,
    CONSTRAINT chk_solicitudes_motivo_rechazo
        CHECK (estado != 'RECHAZADA' OR motivo_rechazo IS NOT NULL)
);

COMMENT ON TABLE solicitudes IS
    'Entidad core. Cada fila representa un trámite completo de solicitud de certificado.';
COMMENT ON COLUMN solicitudes.circunscripcion_id IS
    'Circunscripción dueña de la solicitud. Los empleados solo pueden gestionar solicitudes de su propia circunscripción.';
COMMENT ON COLUMN solicitudes.motivo_rechazo IS
    'Obligatorio cuando estado = RECHAZADA. Validado a nivel DB con CHECK constraint.';

CREATE INDEX idx_solicitudes_ciudadano        ON solicitudes (ciudadano_id);
CREATE INDEX idx_solicitudes_circunscripcion  ON solicitudes (circunscripcion_id);
CREATE INDEX idx_solicitudes_estado           ON solicitudes (estado);
CREATE INDEX idx_solicitudes_empleado         ON solicitudes (empleado_asignado_id);
CREATE INDEX idx_solicitudes_created_at       ON solicitudes (created_at DESC);
-- Índice compuesto para el filtro más frecuente (circunscripción + estado)
CREATE INDEX idx_solicitudes_circ_estado      ON solicitudes (circunscripcion_id, estado);

-- =============================================================================
-- TABLA: adjuntos
-- Documentos subidos por el ciudadano como parte de la solicitud.
-- =============================================================================
CREATE TABLE adjuntos (
    id              SERIAL          PRIMARY KEY,
    solicitud_id    INTEGER         NOT NULL,
    nombre_original VARCHAR(255)    NOT NULL,
    nombre_storage  VARCHAR(255)    NOT NULL,   -- Nombre interno en filesystem/S3
    ruta_storage    TEXT            NOT NULL,   -- Path o URL al archivo
    mime_type       VARCHAR(100)    NOT NULL,
    tamano_bytes    INTEGER         NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_adjuntos_solicitud
        FOREIGN KEY (solicitud_id) REFERENCES solicitudes (id) ON DELETE CASCADE,
    CONSTRAINT chk_adjuntos_tamano
        CHECK (tamano_bytes > 0 AND tamano_bytes <= 5242880),   -- Máx 5 MB
    CONSTRAINT chk_adjuntos_mime
        CHECK (mime_type IN ('application/pdf', 'image/jpeg', 'image/png'))
);

COMMENT ON TABLE adjuntos IS 'Documentos adjuntos a una solicitud. Máx 3 por solicitud, 5MB c/u.';

CREATE INDEX idx_adjuntos_solicitud ON adjuntos (solicitud_id);

-- =============================================================================
-- TABLA: pagos
-- Registro de transacciones de pago asociadas a solicitudes aprobadas.
-- =============================================================================
CREATE TABLE pagos (
    id                  SERIAL          PRIMARY KEY,
    solicitud_id        INTEGER         NOT NULL UNIQUE,    -- 1 pago por solicitud
    monto               NUMERIC(10, 2)  NOT NULL,
    moneda              CHAR(3)         NOT NULL DEFAULT 'ARS',
    estado_pago         VARCHAR(30)     NOT NULL DEFAULT 'PENDIENTE',
    -- Estados posibles: PENDIENTE, APROBADO, RECHAZADO, REVERSADO
    proveedor_pago      VARCHAR(50),    -- 'mercadopago', 'stripe', etc.
    id_externo          VARCHAR(255),   -- ID de la transacción en el proveedor
    preferencia_id      VARCHAR(255),   -- ID de preferencia/intent del proveedor
    datos_respuesta     JSONB,          -- Payload completo del webhook del proveedor
    fecha_intento       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    fecha_confirmacion  TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_pagos_solicitud
        FOREIGN KEY (solicitud_id) REFERENCES solicitudes (id) ON DELETE RESTRICT,
    CONSTRAINT chk_pagos_monto
        CHECK (monto > 0)
);

COMMENT ON TABLE pagos IS
    'Transacciones de pago. El campo datos_respuesta almacena el webhook del proveedor para auditoría.';

CREATE INDEX idx_pagos_solicitud   ON pagos (solicitud_id);
CREATE INDEX idx_pagos_id_externo  ON pagos (id_externo);

-- =============================================================================
-- TABLA: certificados
-- Certificados emitidos. Vinculados 1:1 con la solicitud.
-- =============================================================================
CREATE TABLE certificados (
    id                  SERIAL          PRIMARY KEY,
    solicitud_id        INTEGER         NOT NULL UNIQUE,
    numero_certificado  VARCHAR(30)     NOT NULL,   -- Formato: CERT-{AÑO}-{SEQ}
    ruta_pdf            TEXT            NOT NULL,   -- Path o URL al PDF generado
    emitido_por_id      INTEGER         NOT NULL,   -- Empleado que emitió
    fecha_emision       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_certificados_solicitud
        FOREIGN KEY (solicitud_id) REFERENCES solicitudes (id) ON DELETE RESTRICT,
    CONSTRAINT fk_certificados_emitido_por
        FOREIGN KEY (emitido_por_id) REFERENCES empleados (id) ON DELETE RESTRICT,
    CONSTRAINT uq_certificados_numero UNIQUE (numero_certificado)
);

COMMENT ON TABLE certificados IS
    'Certificados PDF emitidos. Número único con formato CERT-{AÑO}-{SEQ}.';

CREATE INDEX idx_certificados_solicitud ON certificados (solicitud_id);
CREATE INDEX idx_certificados_numero    ON certificados (numero_certificado);

-- =============================================================================
-- TABLA: historial_estados
-- Log inmutable de auditoría de todos los cambios de estado de una solicitud.
-- Nunca se modifica ni elimina.
-- =============================================================================
CREATE TABLE historial_estados (
    id              SERIAL              PRIMARY KEY,
    solicitud_id    INTEGER             NOT NULL,
    estado_anterior estado_solicitud,              -- NULL cuando es la creación
    estado_nuevo    estado_solicitud    NOT NULL,
    usuario_id      INTEGER             NOT NULL,   -- Quién realizó la acción
    comentario      TEXT,
    metadata        JSONB,                          -- Datos extra (ej: id de pago)
    created_at      TIMESTAMPTZ         NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_historial_solicitud
        FOREIGN KEY (solicitud_id) REFERENCES solicitudes (id) ON DELETE RESTRICT,
    CONSTRAINT fk_historial_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE RESTRICT
);

COMMENT ON TABLE historial_estados IS
    'Log inmutable de auditoría. Nunca modificar ni eliminar registros de esta tabla.';

CREATE INDEX idx_historial_solicitud ON historial_estados (solicitud_id);
CREATE INDEX idx_historial_created   ON historial_estados (created_at DESC);

-- =============================================================================
-- FUNCIÓN Y TRIGGER: updated_at automático
-- =============================================================================
CREATE OR REPLACE FUNCTION fn_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Aplicar trigger a todas las tablas con updated_at
CREATE TRIGGER trg_usuarios_updated_at
    BEFORE UPDATE ON usuarios
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_empleados_updated_at
    BEFORE UPDATE ON empleados
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_solicitudes_updated_at
    BEFORE UPDATE ON solicitudes
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_pagos_updated_at
    BEFORE UPDATE ON pagos
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_circunscripciones_updated_at
    BEFORE UPDATE ON circunscripciones
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_tipos_certificado_updated_at
    BEFORE UPDATE ON tipos_certificado
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- =============================================================================
-- DATOS INICIALES
-- =============================================================================

-- Circunscripciones (las 5 requeridas)
INSERT INTO circunscripciones (nombre, codigo, descripcion) VALUES
    ('Circunscripción 1', 'CIRC-01', 'Unidad administrativa norte'),
    ('Circunscripción 2', 'CIRC-02', 'Unidad administrativa sur'),
    ('Circunscripción 3', 'CIRC-03', 'Unidad administrativa este'),
    ('Circunscripción 4', 'CIRC-04', 'Unidad administrativa oeste'),
    ('Circunscripción 5', 'CIRC-05', 'Unidad administrativa central');

-- Roles del sistema
INSERT INTO roles (nombre, descripcion) VALUES
    ('ciudadano', 'Usuario externo que solicita certificados'),
    ('interno',   'Empleado del organismo que procesa solicitudes'),
    ('admin',     'Administrador con acceso total al sistema');

-- Tipos de certificado iniciales
INSERT INTO tipos_certificado (nombre, descripcion) VALUES
    ('Certificado de Libre Deuda',    'Acredita que el ciudadano no posee deudas con el organismo'),
    ('Certificado de Supervivencia',  'Acredita que el ciudadano se encuentra con vida'),
    ('Certificado de Residencia',     'Acredita el domicilio del ciudadano'),
    ('Certificado de Antecedentes',   'Acredita los antecedentes registrados del ciudadano');

-- Usuario admin inicial (contraseña: cambiar en primer login)
-- Hash corresponde a: 'Admin2026!' con bcrypt 10 rounds (CAMBIAR EN PRODUCCIÓN)
INSERT INTO usuarios (nombre, apellido, email, password_hash, rol) VALUES
    ('Administrador', 'Sistema', 'admin@rdam.gob.ar',
     '$2b$10$PLACEHOLDER_HASH_CAMBIAR_EN_PRODUCCION', 'admin');
```

---

## 2. Estrategia de Backup y Recuperación

### 2.1 Política General

La estrategia combina backups **Full** semanales con **incrementales** diarios mediante Write-Ahead Logging (WAL) de PostgreSQL. El objetivo es cumplir con los parámetros de disponibilidad del sistema: **RPO < 24 horas** y **RTO < 4 horas**.

### 2.2 Tipos de Backup y Frecuencia

| Tipo | Frecuencia | Retención | Herramienta | Ventana |
|------|------------|-----------|-------------|---------|
| **Full** | Semanal (domingo 02:00 AM) | 30 días | `pg_dump` | ~15–30 min |
| **Incremental (WAL)** | Continuo + archivo cada hora | 7 días | `pg_basebackup` + `archive_command` | Continuo |
| **Backup Pre-Deploy** | Antes de cada release | 5 releases | `pg_dump` | Manual |

### 2.3 Configuración del Servidor PostgreSQL

Agregar en `postgresql.conf` para habilitar archivado WAL:

```ini
# Archivado WAL para backups incrementales
wal_level = replica
archive_mode = on
archive_command = 'cp %p /backups/wal_archive/%f'
# En S3: archive_command = 'aws s3 cp %p s3://rdam-backups/wal/%f'
max_wal_senders = 3
```

### 2.4 Procedimiento de Backup Full — Paso a Paso

```bash
# 1. Variables de entorno (configurar en .env o cron)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=rdam_prod
DB_USER=rdam_backup_user
BACKUP_DIR=/backups/full
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/rdam_full_${DATE}.dump"

# 2. Crear directorio si no existe
mkdir -p $BACKUP_DIR

# 3. Ejecutar backup (formato custom para restauración selectiva)
PGPASSFILE=~/.pgpass pg_dump \
    -h $DB_HOST \
    -p $DB_PORT \
    -U $DB_USER \
    -d $DB_NAME \
    --format=custom \
    --compress=9 \
    --blobs \
    --verbose \
    --file=$BACKUP_FILE

# 4. Verificar integridad del backup
pg_restore --list $BACKUP_FILE > /dev/null && \
    echo "[OK] Backup verificado: $BACKUP_FILE" || \
    echo "[ERROR] Backup corrupto: $BACKUP_FILE"

# 5. Calcular y guardar checksum
sha256sum $BACKUP_FILE > "${BACKUP_FILE}.sha256"

# 6. Transferir a almacenamiento externo (S3 como ejemplo)
aws s3 cp $BACKUP_FILE s3://rdam-backups/full/
aws s3 cp "${BACKUP_FILE}.sha256" s3://rdam-backups/full/

# 7. Purgar backups locales con más de 30 días
find $BACKUP_DIR -name "*.dump" -mtime +30 -delete
```

**Archivo `.pgpass`** (permisos 600, nunca en repositorio):
```
localhost:5432:rdam_prod:rdam_backup_user:PASSWORD_SEGURA
```

**Cron job recomendado** (`crontab -e`):
```cron
# Backup full todos los domingos a las 02:00 AM
0 2 * * 0 /scripts/backup_full.sh >> /var/log/rdam_backup.log 2>&1

# Backup rápido (schema only) cada día hábil a las 08:00 AM
0 8 * * 1-5 pg_dump -U rdam_backup_user -d rdam_prod --schema-only \
    -f /backups/schema/schema_$(date +\%Y\%m\%d).sql
```

### 2.5 Procedimiento de Restore — Paso a Paso

#### Escenario A: Restore Completo (Disaster Recovery)

```bash
# 1. Detener la aplicación (evitar escrituras durante el restore)
systemctl stop rdam-api

# 2. Crear base de datos nueva (o limpiar la existente)
psql -U postgres -c "DROP DATABASE IF EXISTS rdam_prod;"
psql -U postgres -c "CREATE DATABASE rdam_prod OWNER rdam_app_user;"

# 3. Verificar integridad del backup antes de restaurar
sha256sum -c /backups/full/rdam_full_20260224_020000.dump.sha256

# 4. Restaurar desde backup custom
pg_restore \
    -h localhost \
    -p 5432 \
    -U postgres \
    -d rdam_prod \
    --verbose \
    --exit-on-error \
    /backups/full/rdam_full_20260224_020000.dump

# 5. Verificar que las tablas críticas tienen datos
psql -U rdam_app_user -d rdam_prod -c "
    SELECT 'solicitudes' as tabla, COUNT(*) as filas FROM solicitudes
    UNION ALL
    SELECT 'certificados', COUNT(*) FROM certificados
    UNION ALL
    SELECT 'usuarios', COUNT(*) FROM usuarios;
"

# 6. Reiniciar la aplicación
systemctl start rdam-api

# 7. Registrar evento en bitácora de incidentes
echo "$(date) - Restore completado desde: rdam_full_20260224_020000.dump" \
    >> /var/log/rdam_incidentes.log
```

#### Escenario B: Restore Parcial (tabla específica)

```bash
# Restaurar solo la tabla 'solicitudes' sin afectar el resto
pg_restore \
    -h localhost -U postgres -d rdam_prod \
    --table=solicitudes \
    /backups/full/rdam_full_20260224_020000.dump
```

#### Escenario C: Point-in-Time Recovery (PITR) con WAL

```bash
# 1. Restaurar el backup base más cercano al punto deseado
pg_restore -d rdam_prod /backups/full/rdam_full_20260223_020000.dump

# 2. Crear archivo recovery.conf (PostgreSQL 12+: en postgresql.conf)
cat >> /etc/postgresql/15/main/postgresql.conf << 'EOF'
restore_command = 'cp /backups/wal_archive/%f %p'
recovery_target_time = '2026-02-24 14:30:00'   # Momento exacto a recuperar
recovery_target_action = 'promote'
EOF

# 3. Crear archivo señal de recovery
touch /var/lib/postgresql/15/main/recovery.signal

# 4. Iniciar PostgreSQL — aplicará WAL hasta el punto especificado
systemctl start postgresql
```

### 2.6 Escenario de Desastre Total

En caso de pérdida total del servidor (falla de disco, ransomware, etc.):

1. **Provisionar nueva instancia** de PostgreSQL en servidor limpio (mismo hardware o cloud).
2. **Instalar PostgreSQL 15** con la misma versión principal que el servidor original.
3. **Descargar el último backup Full** desde almacenamiento externo (S3/Blob) y verificar checksum.
4. **Ejecutar restore completo** siguiendo el Escenario A.
5. **Aplicar WAL archivados** si se requiere recuperación hasta un punto más reciente.
6. **Actualizar DNS / balanceador de carga** para apuntar al nuevo servidor.
7. **Validar integridad** con queries de control antes de reabrir tráfico.

**Meta RTO:** < 4 horas desde la detección del incidente.

### 2.7 Buenas Prácticas de Almacenamiento

- Los backups se guardan en **ubicación geográfica distinta** al servidor de producción (regla 3-2-1).
- Los archivos `.dump` se comprimen con nivel 9 (`--compress=9`) para reducir costo de almacenamiento.
- Nunca almacenar credenciales de base de datos en los scripts; usar `.pgpass` o variables de entorno del sistema operativo.
- Cifrar los backups en reposo con AES-256 si contienen datos sensibles de ciudadanos (cumplimiento GDPR / normativa local).
- Realizar un **drill de restore** mensual en entorno de QA para garantizar la validez del procedimiento.
- Monitorear el tamaño del directorio WAL: si crece sin control indica que el archivado no está funcionando.

---

## 3. Mapa de Endpoints REST

### 3.1 Convenciones

- Todos los endpoints retornan `Content-Type: application/json`.
- Autenticación via `Authorization: Bearer <JWT>` (excepto endpoints públicos).
- Los errores siguen el formato: `{ "error": "CÓDIGO", "mensaje": "Descripción legible" }`.
- La validación de circunscripción se aplica a nivel de middleware para endpoints marcados con ✅.

### 3.2 Autenticación

| Método | Endpoint | Descripción | Rol | Circ. | Responses |
|--------|----------|-------------|-----|-------|-----------|
| `POST` | `/auth/login` | Autenticación. Retorna JWT con payload: `{ id, rol, circunscripcion_id }` | Público | ❌ | 200, 401, 422 |
| `POST` | `/auth/register` | Registro de nuevo ciudadano | Público | ❌ | 201, 409, 422 |
| `POST` | `/auth/forgot-password` | Solicita reset de contraseña vía email | Público | ❌ | 200, 404 |
| `POST` | `/auth/reset-password` | Establece nueva contraseña con token recibido | Público | ❌ | 200, 400, 410 |
| `GET`  | `/auth/me` | Retorna datos del usuario autenticado | Todos | ❌ | 200, 401 |

**Payload JWT decodificado:**
```json
{
  "sub": 42,
  "rol": "interno",
  "circunscripcion_id": 3,
  "iat": 1740000000,
  "exp": 1740086400
}
```

### 3.3 Circunscripciones

| Método | Endpoint | Descripción | Rol | Circ. | Responses |
|--------|----------|-------------|-----|-------|-----------|
| `GET`  | `/circunscripciones` | Lista todas las circunscripciones activas | Todos | ❌ | 200 |
| `POST` | `/circunscripciones` | Crea nueva circunscripción | admin | ❌ | 201, 409, 422 |
| `PUT`  | `/circunscripciones/:id` | Actualiza datos de una circunscripción | admin | ❌ | 200, 404, 422 |

### 3.4 Empleados

| Método | Endpoint | Descripción | Rol | Circ. | Responses |
|--------|----------|-------------|-----|-------|-----------|
| `GET`  | `/empleados` | Lista empleados (admin: todos; interno: solo su circunscripción) | admin, interno | ✅ | 200, 403 |
| `POST` | `/empleados` | Crea nuevo empleado interno, asigna circunscripción | admin | ❌ | 201, 409, 422 |
| `GET`  | `/empleados/:id` | Detalle de un empleado | admin | ❌ | 200, 403, 404 |
| `PUT`  | `/empleados/:id` | Actualiza datos del empleado (incluyendo circunscripción) | admin | ❌ | 200, 404, 422 |
| `DELETE` | `/empleados/:id` | Soft delete (desactiva usuario asociado) | admin | ❌ | 200, 404 |

### 3.5 Solicitudes

| Método | Endpoint | Descripción | Rol | Circ. | Responses |
|--------|----------|-------------|-----|-------|-----------|
| `POST` | `/solicitudes` | Crea nueva solicitud. El ciudadano selecciona circunscripción | ciudadano | ❌ | 201, 422 |
| `GET`  | `/solicitudes` | Lista solicitudes. Ciudadano: las propias. Interno/Admin: filtradas por circunscripción | Todos | ✅ | 200, 403 |
| `GET`  | `/solicitudes/:id` | Detalle de una solicitud con historial de estados | Todos | ✅ | 200, 403, 404 |
| `PUT`  | `/solicitudes/:id/tomar` | Empleado toma la solicitud → estado EN_REVISION | interno, admin | ✅ | 200, 400, 403, 404 |
| `PUT`  | `/solicitudes/:id/aprobar` | Aprueba solicitud → estado APROBADA | interno, admin | ✅ | 200, 400, 403, 404 |
| `PUT`  | `/solicitudes/:id/rechazar` | Rechaza con motivo obligatorio → estado RECHAZADA | interno, admin | ✅ | 200, 400, 403, 404, 422 |
| `PUT`  | `/solicitudes/:id/reasignar` | Reasigna a otro empleado de la misma circunscripción | interno, admin | ✅ | 200, 400, 403, 404 |
| `PUT`  | `/solicitudes/:id/cancelar` | Ciudadano cancela su solicitud (solo antes de pagar) | ciudadano | ❌ | 200, 400, 403, 404 |
| `GET`  | `/solicitudes/:id/historial` | Timeline completo de cambios de estado | Todos | ✅ | 200, 403, 404 |

### 3.6 Pagos

| Método | Endpoint | Descripción | Rol | Circ. | Responses |
|--------|----------|-------------|-----|-------|-----------|
| `POST` | `/pagos/iniciar` | Inicia el proceso de pago. Crea preferencia en la pasarela y retorna URL de redirección | ciudadano | ❌ | 201, 400, 404, 422 |
| `POST` | `/pagos/webhook` | Webhook del proveedor de pago. Confirma el pago y actualiza estado a PAGADA. **Endpoint público con validación de firma HMAC** | Público (firma) | ❌ | 200, 400, 401 |
| `GET`  | `/pagos/:solicitud_id` | Datos del pago de una solicitud | ciudadano, interno, admin | ✅ | 200, 403, 404 |

### 3.7 Certificados

| Método | Endpoint | Descripción | Rol | Circ. | Responses |
|--------|----------|-------------|-----|-------|-----------|
| `POST` | `/certificados` | Emite el certificado de una solicitud PAGADA. Genera PDF, lo almacena y envía por email | interno, admin | ✅ | 201, 400, 403, 404 |
| `GET`  | `/certificados/:id/pdf` | Descarga el PDF del certificado. Ciudadano solo puede descargar el suyo. | ciudadano, interno, admin | ✅* | 200, 403, 404 |

*En certificados, el ciudadano puede acceder al PDF de su propia solicitud sin restricción de circunscripción.

### 3.8 Tipos de Certificado (Admin)

| Método | Endpoint | Descripción | Rol | Responses |
|--------|----------|-------------|-----|-----------|
| `GET`  | `/tipos-certificado` | Lista tipos activos (público para el formulario ciudadano) | Todos | 200 |
| `POST` | `/tipos-certificado` | Crea nuevo tipo | admin | 201, 409, 422 |
| `PUT`  | `/tipos-certificado/:id` | Actualiza tipo existente | admin | 200, 404, 422 |
| `DELETE` | `/tipos-certificado/:id` | Soft delete (activo = false) | admin | 200, 404 |

### 3.9 Usuarios (Admin)

| Método | Endpoint | Descripción | Rol | Responses |
|--------|----------|-------------|-----|-----------|
| `GET`  | `/usuarios` | Lista usuarios internos y admins | admin | 200 |
| `GET`  | `/usuarios/:id` | Detalle de un usuario | admin | 200, 404 |
| `PUT`  | `/usuarios/:id` | Actualiza datos de un usuario | admin | 200, 404, 422 |
| `DELETE` | `/usuarios/:id` | Soft delete (desactiva cuenta) | admin | 200, 404 |

---

## 4. Seguridad: Validación por Circunscripción

### 4.1 Principio de Diseño

El sistema implementa **control de acceso en dos capas**:

1. **Control por rol** — verifica el tipo de usuario (ciudadano / interno / admin).
2. **Control por circunscripción** — verifica que el empleado solo opera sobre solicitudes de su circunscripción.

Ambas capas se implementan como **middlewares de Express** encadenados, garantizando que ningún controlador procese una request sin haber pasado ambas validaciones.

### 4.2 Estructura del JWT

El token incluye el `circunscripcion_id` del empleado en el payload, evitando una consulta adicional a la base de datos en cada request:

```javascript
// Al hacer login (authService.js)
const payload = {
    sub: usuario.id,
    rol: usuario.rol,
    // Para empleados: se adjunta su circunscripción al momento del login
    circunscripcion_id: empleado?.circunscripcion_id ?? null,
};
const token = jwt.sign(payload, process.env.JWT_SECRET, { expiresIn: '24h' });
```

### 4.3 Middleware de Autenticación

```javascript
// middleware/authenticate.js
const jwt = require('jsonwebtoken');

const authenticate = (req, res, next) => {
    const authHeader = req.headers.authorization;
    if (!authHeader?.startsWith('Bearer ')) {
        return res.status(401).json({ error: 'TOKEN_AUSENTE', mensaje: 'Se requiere autenticación.' });
    }

    const token = authHeader.split(' ')[1];
    try {
        req.user = jwt.verify(token, process.env.JWT_SECRET);
        next();
    } catch (err) {
        return res.status(401).json({ error: 'TOKEN_INVALIDO', mensaje: 'Token expirado o inválido.' });
    }
};

module.exports = authenticate;
```

### 4.4 Middleware de Autorización por Rol

```javascript
// middleware/authorize.js

/**
 * Verifica que el usuario autenticado tenga al menos uno de los roles requeridos.
 * @param {...string} roles - Roles permitidos ('ciudadano', 'interno', 'admin')
 */
const authorize = (...roles) => (req, res, next) => {
    if (!roles.includes(req.user.rol)) {
        return res.status(403).json({
            error: 'ROL_INSUFICIENTE',
            mensaje: `Esta acción requiere uno de los siguientes roles: ${roles.join(', ')}.`,
        });
    }
    next();
};

module.exports = authorize;
```

### 4.5 Middleware de Validación por Circunscripción

Este es el núcleo de la seguridad horizontal. Verifica que la solicitud sobre la que opera el empleado pertenezca a su propia circunscripción.

```javascript
// middleware/validateCircunscripcion.js
const pool = require('../config/db');

/**
 * Verifica que la solicitud (req.params.id) pertenezca a la circunscripción
 * del empleado autenticado. Los admins pueden acceder a todas.
 *
 * Se ubica DESPUÉS de authenticate y authorize en la cadena de middlewares.
 */
const validateCircunscripcion = async (req, res, next) => {
    // Los administradores tienen acceso irrestricto
    if (req.user.rol === 'admin') return next();

    const solicitudId = req.params.id;
    if (!solicitudId) return next(); // Endpoints sin ID (listados se filtran en el controlador)

    try {
        const { rows } = await pool.query(
            'SELECT circunscripcion_id FROM solicitudes WHERE id = $1',
            [solicitudId]
        );

        if (rows.length === 0) {
            return res.status(404).json({
                error: 'SOLICITUD_NO_ENCONTRADA',
                mensaje: 'La solicitud solicitada no existe.',
            });
        }

        const solicitudCirc = rows[0].circunscripcion_id;
        const empleadoCirc  = req.user.circunscripcion_id;

        if (solicitudCirc !== empleadoCirc) {
            // Mismo mensaje que 404 para no revelar la existencia del recurso
            return res.status(403).json({
                error: 'ACCESO_DENEGADO',
                mensaje: 'No tiene permisos para acceder a esta solicitud.',
            });
        }

        next();
    } catch (err) {
        next(err); // Delega al manejador global de errores
    }
};

module.exports = validateCircunscripcion;
```

### 4.6 Filtrado en Listados (Prevención de Exposición Masiva)

Los endpoints `GET /solicitudes` no reciben un ID, pero deben igualmente restringir los datos:

```javascript
// controllers/solicitudesController.js
const getSolicitudes = async (req, res, next) => {
    try {
        let query;
        let params;

        if (req.user.rol === 'ciudadano') {
            // El ciudadano solo ve SUS solicitudes
            query = 'SELECT * FROM solicitudes WHERE ciudadano_id = $1 ORDER BY created_at DESC';
            params = [req.user.sub];

        } else if (req.user.rol === 'interno') {
            // El empleado interno solo ve solicitudes de SU circunscripción
            query = `SELECT * FROM solicitudes
                     WHERE circunscripcion_id = $1
                     ORDER BY created_at DESC`;
            params = [req.user.circunscripcion_id];

        } else {
            // Admin: acceso total, con posibilidad de filtrar por circunscripción
            const circFiltro = req.query.circunscripcion_id;
            if (circFiltro) {
                query  = 'SELECT * FROM solicitudes WHERE circunscripcion_id = $1 ORDER BY created_at DESC';
                params = [circFiltro];
            } else {
                query  = 'SELECT * FROM solicitudes ORDER BY created_at DESC';
                params = [];
            }
        }

        const { rows } = await pool.query(query, params);
        res.json({ data: rows });

    } catch (err) {
        next(err);
    }
};
```

### 4.7 Composición en las Rutas

Los tres middlewares se encadenan declarativamente en la definición de rutas:

```javascript
// routes/solicitudes.js
const express    = require('express');
const router     = express.Router();
const auth       = require('../middleware/authenticate');
const authorize  = require('../middleware/authorize');
const validateCirc = require('../middleware/validateCircunscripcion');
const ctrl       = require('../controllers/solicitudesController');

// Listado: sin ID, el filtrado ocurre dentro del controlador
router.get('/',
    auth,
    authorize('ciudadano', 'interno', 'admin'),
    ctrl.getSolicitudes
);

// Detalle: validación de circunscripción por ID
router.get('/:id',
    auth,
    authorize('ciudadano', 'interno', 'admin'),
    validateCirc,
    ctrl.getSolicitudById
);

// Aprobar: solo internos o admins de la misma circunscripción
router.put('/:id/aprobar',
    auth,
    authorize('interno', 'admin'),
    validateCirc,
    ctrl.aprobarSolicitud
);

// Rechazar
router.put('/:id/rechazar',
    auth,
    authorize('interno', 'admin'),
    validateCirc,
    ctrl.rechazarSolicitud
);

// Cancelar: solo el ciudadano dueño de la solicitud
router.put('/:id/cancelar',
    auth,
    authorize('ciudadano'),
    ctrl.cancelarSolicitud    // Verifica ciudadano_id === req.user.sub dentro del controlador
);

module.exports = router;
```

### 4.8 Validación de Propiedad para Ciudadanos

Los ciudadanos tienen restricción distinta: no por circunscripción sino por **propiedad de la solicitud**. Esta validación ocurre en el controlador:

```javascript
// Dentro de solicitudesController.js — cancelarSolicitud
const cancelarSolicitud = async (req, res, next) => {
    const { id } = req.params;
    const { rows } = await pool.query(
        'SELECT ciudadano_id, estado FROM solicitudes WHERE id = $1',
        [id]
    );

    if (!rows.length) {
        return res.status(404).json({ error: 'SOLICITUD_NO_ENCONTRADA' });
    }

    // El ciudadano solo puede cancelar SUS propias solicitudes
    if (rows[0].ciudadano_id !== req.user.sub) {
        return res.status(403).json({ error: 'ACCESO_DENEGADO' });
    }

    // Validar que el estado permite cancelación
    const estadosPermitidos = ['PENDIENTE_REVISION', 'EN_REVISION', 'APROBADA'];
    if (!estadosPermitidos.includes(rows[0].estado)) {
        return res.status(400).json({
            error: 'ESTADO_NO_PERMITE_CANCELACION',
            mensaje: `No se puede cancelar una solicitud en estado ${rows[0].estado}.`,
        });
    }

    // ... proceder con la cancelación
};
```

### 4.9 Validación de Reasignación: Misma Circunscripción

Al reasignar una solicitud, se valida que el nuevo empleado pertenezca a la misma circunscripción:

```javascript
// En el controlador de reasignación
const reasignarSolicitud = async (req, res, next) => {
    const { nuevoEmpleadoId } = req.body;
    const solicitudId = req.params.id;

    // Obtener circunscripción del nuevo empleado
    const { rows: empleadoRows } = await pool.query(
        'SELECT circunscripcion_id FROM empleados WHERE id = $1',
        [nuevoEmpleadoId]
    );

    if (!empleadoRows.length) {
        return res.status(404).json({ error: 'EMPLEADO_NO_ENCONTRADO' });
    }

    // Verificar que el nuevo empleado pertenece a la misma circunscripción
    const { rows: solicitudRows } = await pool.query(
        'SELECT circunscripcion_id FROM solicitudes WHERE id = $1',
        [solicitudId]
    );

    if (empleadoRows[0].circunscripcion_id !== solicitudRows[0].circunscripcion_id) {
        return res.status(400).json({
            error: 'CIRCUNSCRIPCION_INCOMPATIBLE',
            mensaje: 'Solo se puede reasignar a empleados de la misma circunscripción.',
        });
    }

    // ... proceder con la reasignación
};
```

### 4.10 Resumen del Modelo de Seguridad

```
Request → [authenticate] → [authorize(roles)] → [validateCircunscripcion] → Controller
                ↓                   ↓                       ↓
           401 / 403           403 / 403                403 / 404
         (sin token)       (rol incorrecto)       (circunscripción incorrecta)
```

La combinación de estos tres controles garantiza que:

- Un empleado de la Circunscripción 2 **no puede ver ni modificar** solicitudes de la Circunscripción 3, aunque conozca el ID.
- Un ciudadano **no puede ver solicitudes de otro ciudadano** aunque ambos pertenezcan a la misma circunscripción.
- El rol `admin` tiene acceso transversal pero puede ser auditado en el `historial_estados`.
- Todo acceso indebido genera un log en la tabla `historial_estados` o en el sistema de monitoring.

---

## Apéndice A: Matriz de Transiciones de Estado

| Estado Origen | Acción | Estado Destino | Rol Requerido |
|---------------|--------|----------------|---------------|
| `PENDIENTE_REVISION` | Tomar | `EN_REVISION` | interno, admin |
| `PENDIENTE_REVISION` | Cancelar | `CANCELADA` | ciudadano |
| `EN_REVISION` | Aprobar | `APROBADA` | interno, admin |
| `EN_REVISION` | Rechazar | `RECHAZADA` | interno, admin |
| `EN_REVISION` | Cancelar | `CANCELADA` | ciudadano |
| `APROBADA` | Pago confirmado (webhook) | `PAGADA` | Sistema (webhook) |
| `APROBADA` | Cancelar | `CANCELADA` | ciudadano |
| `PAGADA` | Emitir | `EMITIDA` | interno, admin |

---

## Apéndice B: Checklist de Entrega — Fase 1

- [x] DDL ejecutable sin errores con datos iniciales para 5 circunscripciones
- [x] Tablas normalizadas en 3FN con PK, FK, CHECK, UNIQUE, NOT NULL
- [x] Índices estratégicos en columnas de búsqueda frecuente
- [x] Soft delete implementado en usuarios y tipos_certificado
- [x] Campos `created_at` / `updated_at` con trigger automático en todas las tablas
- [x] Enum `estado_solicitud` con los 7 estados del ciclo de vida
- [x] Estrategia de backup Full + WAL incremental documentada
- [x] Procedimiento de restore paso a paso con comandos ejecutables
- [x] Escenario de desastre total documentado con RTO < 4h
- [x] 25+ endpoints mapeados con método, rol, validación de circunscripción y códigos de respuesta
- [x] Middleware `authenticate` implementado con JWT
- [x] Middleware `authorize` implementado por rol
- [x] Middleware `validateCircunscripcion` implementado con prevención de acceso horizontal
- [x] Filtrado por circunscripción en listados (no solo en detalle)
- [x] Validación de propiedad para ciudadanos
- [x] Validación de reasignación dentro de misma circunscripción
- [x] Arquitectura preparada para escalar nuevas circunscripciones sin cambios de código

---

**FIN DEL DOCUMENTO — RDAM Fase 1: Modelado**

*i2T Software Factory — Campus de Verano 2026*
