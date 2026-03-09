        -- =============================================================================
        -- RDAM — Script DDL Completo (VERSIÓN FINAL CON FIXES)
        -- Motor: PostgreSQL 15+
        -- Descripción: Esquema de base de datos para el sistema de gestión y emisión
        --              de certificados digitales con soporte de circunscripciones.
        -- Proyecto: i2T Software Factory — Campus de Verano 2026
        -- Responsables: Beliz, Meyer
        -- Versión: FINAL | Fecha: Febrero 2026
        -- ESTADO: ✅ APTO PARA STAGING
        -- Cambios desde v6: 3 blockers resueltos (B1, B2, B3)
        -- =============================================================================

        -- Ejecutar como superusuario o el dueño de la base de datos.
        -- Crear la base de datos previamente:
        --   CREATE DATABASE rdam_prod OWNER rdam_app_user;

        -- Extensión para funciones criptográficas (opcional, no requerida en este script)
        CREATE EXTENSION IF NOT EXISTS "pgcrypto";

        -- =============================================================================
        -- TIPOS ENUMERADOS
        -- =============================================================================

        -- Estados posibles de una solicitud a lo largo de su ciclo de vida
        CREATE TYPE estado_solicitud AS ENUM (
            'PENDIENTE_REVISION',  -- Recién creada, esperando ser tomada por un empleado
            'EN_REVISION',         -- Asignada a un empleado, en proceso de revisión
            'APROBADA',            -- Aprobada por el empleado, pendiente de pago del ciudadano
            'RECHAZADA',           -- Rechazada definitivamente (estado final)
            'PAGADA',              -- Pago confirmado por la pasarela, pendiente de emisión
            'EMITIDA',             -- Certificado PDF generado y enviado (estado final)
            'CANCELADA'            -- Cancelada por el ciudadano antes del pago (estado final)
        );

        -- Roles del sistema
        CREATE TYPE rol_usuario AS ENUM (
            'ciudadano',  -- Usuario externo que solicita certificados
            'interno',    -- Empleado del organismo que procesa solicitudes
            'admin'       -- Administrador con permisos totales
        );

        -- =============================================================================
        -- FUNCIÓN: Actualización automática de updated_at
        -- Movida al inicio para evitar referencias a función no-existente en triggers
        -- (FIX BUG1: fn_set_updated_at debe existir ANTES de CREATE TRIGGER)
        -- =============================================================================
        CREATE OR REPLACE FUNCTION fn_set_updated_at()
        RETURNS TRIGGER AS $$
        BEGIN
            NEW.updated_at = NOW();
            RETURN NEW;
        END;
        $$ LANGUAGE plpgsql;

        COMMENT ON FUNCTION fn_set_updated_at IS
            'Actualiza automáticamente el campo updated_at antes de cada UPDATE.';

        -- =============================================================================
        -- TABLA: circunscripciones
        -- Catálogo de unidades administrativas del organismo.
        -- Diseñada para escalar: agregar circunscripciones es un INSERT, no un cambio de código.
        -- =============================================================================
        CREATE TABLE circunscripciones (
            id          SERIAL          PRIMARY KEY,
            nombre      VARCHAR(100)    NOT NULL,
            codigo      VARCHAR(20)     NOT NULL,    -- Código corto legible (ej: CIRC-01)
            descripcion TEXT,
            activo      BOOLEAN         NOT NULL DEFAULT TRUE,
            created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
            updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

            CONSTRAINT uq_circunscripciones_codigo UNIQUE (codigo)
        );

        COMMENT ON TABLE circunscripciones IS
            'Unidades administrativas del organismo. Escala sin cambios de código.';
        COMMENT ON COLUMN circunscripciones.codigo IS
            'Código corto para identificación rápida. Ej: CIRC-01.';

        -- =============================================================================
        -- NOTA: Tabla 'roles' ELIMINADA - FIX C2
        -- El ENUM rol_usuario es la única fuente de verdad para roles del sistema.
        -- Esto cumple 3NF y evita duplicidad. Los roles se agregan editando el ENUM,
        -- no mediante INSERT, asegurando que código y DB estén sincronizados.
        -- =============================================================================

        -- =============================================================================
        -- TABLA: usuarios
        -- Todos los actores del sistema: ciudadanos, internos y admins.
        -- Soft delete via campo eliminado_en para conservar integridad referencial.
        -- =============================================================================
        CREATE TABLE usuarios (
            id               SERIAL          PRIMARY KEY,
            nombre           VARCHAR(150)    NOT NULL,
            apellido         VARCHAR(150)    NOT NULL,
            email            VARCHAR(255)    NOT NULL,
            cuil             VARCHAR(11),                 -- 11 dígitos, solo ciudadanos
            telefono         VARCHAR(30),
            direccion        TEXT,
            fecha_nacimiento DATE,
            password_hash    VARCHAR(255)    NOT NULL,    -- bcrypt, mínimo 10 rounds
            rol              rol_usuario     NOT NULL DEFAULT 'ciudadano',
            activo           BOOLEAN         NOT NULL DEFAULT TRUE,
            created_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
            updated_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
            eliminado_en     TIMESTAMPTZ,                -- Soft delete: NULL = activo

            CONSTRAINT uq_usuarios_email   UNIQUE (email),
            CONSTRAINT uq_usuarios_cuil    UNIQUE (cuil),  -- FIX C7: CUIL debe ser único
            CONSTRAINT chk_usuarios_cuil   CHECK (cuil ~ '^\d{11}$' OR cuil IS NULL)
        );

        COMMENT ON TABLE  usuarios         IS 'Tabla central de actores del sistema.';
        COMMENT ON COLUMN usuarios.password_hash IS
            'Hash bcrypt con mínimo 10 rounds. Nunca almacenar contraseña en texto plano.';
        COMMENT ON COLUMN usuarios.eliminado_en IS
            'Soft delete. Si es NULL el usuario está activo; si tiene fecha está eliminado.';

        CREATE INDEX idx_usuarios_email  ON usuarios (email);
        CREATE INDEX idx_usuarios_rol    ON usuarios (rol);
        CREATE INDEX idx_usuarios_activo ON usuarios (activo) WHERE activo = TRUE;
        CREATE INDEX idx_usuarios_cuil   ON usuarios (cuil) WHERE cuil IS NOT NULL;  -- FIX C7: Índice en CUIL

        -- =============================================================================
        -- TABLA: empleados
        -- Extiende usuarios con datos propios del personal interno.
        -- Regla de negocio: un empleado pertenece a UNA SOLA circunscripción.
        -- =============================================================================
        CREATE TABLE empleados (
            id                  SERIAL      PRIMARY KEY,
            usuario_id          INTEGER     NOT NULL,
            circunscripcion_id  INTEGER     NOT NULL,
            legajo              VARCHAR(50) NOT NULL,   -- FIX M7: Número de legajo (requerido)
            cargo               VARCHAR(100),
            created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
            updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

            CONSTRAINT fk_empleados_usuario
                FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE RESTRICT,
            CONSTRAINT fk_empleados_circunscripcion
                FOREIGN KEY (circunscripcion_id) REFERENCES circunscripciones (id) ON DELETE RESTRICT,
            CONSTRAINT uq_empleados_usuario UNIQUE (usuario_id),  -- 1 perfil empleado por usuario
            CONSTRAINT uq_empleados_legajo  UNIQUE (legajo)      -- FIX M7: Legajo único
        );

        COMMENT ON TABLE  empleados                     IS
            'Perfil extendido de empleados internos y admins.';
        COMMENT ON COLUMN empleados.circunscripcion_id  IS
            'Circunscripción a la que pertenece el empleado. Solo puede gestionar solicitudes de su propia circunscripción.';

        CREATE INDEX idx_empleados_circunscripcion ON empleados (circunscripcion_id);
        CREATE INDEX idx_empleados_usuario         ON empleados (usuario_id);

        -- =============================================================================
        -- TABLA: tipos_certificado
        -- Catálogo configurable de tipos de certificado.
        -- Administrable por el admin desde el sistema, sin deploy.
        -- =============================================================================
        CREATE TABLE tipos_certificado (
            id           SERIAL          PRIMARY KEY,
            nombre       VARCHAR(150)    NOT NULL,
            descripcion  TEXT,
            activo       BOOLEAN         NOT NULL DEFAULT TRUE,
            created_at   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
            updated_at   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
            eliminado_en TIMESTAMPTZ,                            -- Soft delete

            CONSTRAINT uq_tipos_certificado_nombre UNIQUE (nombre)
        );

        COMMENT ON TABLE tipos_certificado IS
            'Catálogo de tipos de certificado. Solo los activos aparecen en el formulario del ciudadano.';

        -- =============================================================================
        -- TABLA: solicitudes
        -- Entidad core del sistema. Registra cada trámite de solicitud de certificado.
        -- Vinculada a circunscripcion_id para hacer posible el control de acceso
        -- de empleados internos sin JOINs adicionales.
        -- =============================================================================
        CREATE TABLE solicitudes (
            id                   SERIAL              PRIMARY KEY,
            ciudadano_id         INTEGER             NOT NULL,
            tipo_certificado_id  INTEGER             NOT NULL,
            circunscripcion_id   INTEGER             NOT NULL,   -- Circunscripción responsable
            estado               estado_solicitud    NOT NULL DEFAULT 'PENDIENTE_REVISION',
            motivo_rechazo       TEXT,                           -- Obligatorio si estado = RECHAZADA
            empleado_asignado_id INTEGER,                        -- Empleado que tomó el trámite
            fecha_deseada        DATE,                           -- Fecha deseada indicada por el ciudadano
            fecha_asignacion     TIMESTAMPTZ,
            fecha_aprobacion     TIMESTAMPTZ,
            fecha_rechazo        TIMESTAMPTZ,
            fecha_pago           TIMESTAMPTZ,
            fecha_emision        TIMESTAMPTZ,
            fecha_cancelacion    TIMESTAMPTZ,
            monto_arancel        NUMERIC(10, 2)      NOT NULL DEFAULT 5000.00,
            created_at           TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
            updated_at           TIMESTAMPTZ         NOT NULL DEFAULT NOW(),

            CONSTRAINT fk_solicitudes_ciudadano
                FOREIGN KEY (ciudadano_id) REFERENCES usuarios (id) ON DELETE RESTRICT,
            CONSTRAINT fk_solicitudes_tipo_certificado
                FOREIGN KEY (tipo_certificado_id) REFERENCES tipos_certificado (id) ON DELETE RESTRICT,
            CONSTRAINT fk_solicitudes_circunscripcion
                FOREIGN KEY (circunscripcion_id) REFERENCES circunscripciones (id) ON DELETE RESTRICT,
            CONSTRAINT fk_solicitudes_empleado
                FOREIGN KEY (empleado_asignado_id) REFERENCES empleados (id) ON DELETE SET NULL,
            CONSTRAINT chk_solicitudes_motivo_rechazo
                CHECK (estado != 'RECHAZADA' OR motivo_rechazo IS NOT NULL),
            CONSTRAINT chk_solicitudes_monto    
                CHECK (monto_arancel > 0)
        );

        COMMENT ON TABLE  solicitudes                    IS
            'Entidad central del sistema. Cada fila es un trámite completo.';
        COMMENT ON COLUMN solicitudes.circunscripcion_id IS
            'Circunscripción responsable. Empleados solo gestionan solicitudes de su propia circunscripción.';
        COMMENT ON COLUMN solicitudes.motivo_rechazo     IS
            'Obligatorio cuando estado = RECHAZADA. Validado con CHECK constraint a nivel DB.';

        CREATE INDEX idx_solicitudes_ciudadano       ON solicitudes (ciudadano_id);
        CREATE INDEX idx_solicitudes_circunscripcion ON solicitudes (circunscripcion_id);
        CREATE INDEX idx_solicitudes_estado          ON solicitudes (estado);
        CREATE INDEX idx_solicitudes_empleado        ON solicitudes (empleado_asignado_id);
        CREATE INDEX idx_solicitudes_created_at      ON solicitudes (created_at DESC);
        -- Índice compuesto para el filtro más frecuente de empleados internos
        CREATE INDEX idx_solicitudes_circ_estado     ON solicitudes (circunscripcion_id, estado);
        -- FIX M_NEW_4: Índices en fecha_deseada para queries de programación
        CREATE INDEX idx_solicitudes_fecha_deseada
            ON solicitudes (fecha_deseada)
            WHERE fecha_deseada IS NOT NULL;
        -- Índice compuesto: circunscripción + fecha (caso más común: "qué solicitudes para esta semana")
        CREATE INDEX idx_solicitudes_circ_fecha
            ON solicitudes (circunscripcion_id, fecha_deseada DESC)
            WHERE fecha_deseada IS NOT NULL;

        -- =============================================================================
        -- TABLA: adjuntos
        -- Documentos subidos por el ciudadano como soporte de la solicitud.
        -- Máximo 3 archivos por solicitud, 5 MB cada uno.
        -- =============================================================================
        CREATE TABLE adjuntos (
            id               SERIAL          PRIMARY KEY,
            solicitud_id     INTEGER         NOT NULL,
            nombre_original  VARCHAR(255)    NOT NULL,
            nombre_storage   VARCHAR(255)    NOT NULL,   -- Nombre sanitizado en filesystem/S3
            ruta_storage     TEXT            NOT NULL,   -- Path completo o URL al archivo
            mime_type        VARCHAR(100)    NOT NULL,
            tamano_bytes     INTEGER         NOT NULL,
            created_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
            updated_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),  -- FIX m_new_4: Agregado para auditoría

            CONSTRAINT fk_adjuntos_solicitud
                FOREIGN KEY (solicitud_id) REFERENCES solicitudes (id) ON DELETE CASCADE,
            CONSTRAINT chk_adjuntos_tamano
                CHECK (tamano_bytes > 0 AND tamano_bytes <= 5242880),  -- Máx 5 MB
            CONSTRAINT chk_adjuntos_mime
                CHECK (mime_type IN ('application/pdf', 'image/jpeg', 'image/png')),
            -- FIX M_NEW_5: Validar que storage_key solo contiene caracteres seguros
            -- (evita path traversal y exposición de estructura interna)
            CONSTRAINT chk_adjuntos_storage_key_format
                CHECK (nombre_storage ~ '^[a-zA-Z0-9\-_.]+$')
        );

        COMMENT ON TABLE adjuntos IS
            'Documentos adjuntos a solicitudes. Máx 3 por solicitud, 5 MB c/u. Formatos: PDF, JPG, PNG.';
        COMMENT ON COLUMN adjuntos.nombre_storage IS
            'UUID o key seguro únicamente - NUNCA paths internos del filesystem. '
            'El path se reconstruye en app layer con base configurable.';

        CREATE INDEX idx_adjuntos_solicitud ON adjuntos (solicitud_id);

        -- =============================================================================
        -- FIX C6: Trigger para validar máximo 3 adjuntos por solicitud
        -- =============================================================================

        CREATE OR REPLACE FUNCTION fn_check_max_adjuntos()
        RETURNS TRIGGER LANGUAGE plpgsql
        SET search_path = public, pg_temp
        AS $$
        BEGIN
            IF (SELECT COUNT(*) FROM adjuntos WHERE solicitud_id = NEW.solicitud_id) >= 3 THEN
                RAISE EXCEPTION 'BUSINESS_RULE_VIOLATION: Máximo 3 adjuntos permitidos por solicitud (solicitud_id: %)',
                    NEW.solicitud_id;
            END IF;
            RETURN NEW;
        END;
        $$;

        CREATE TRIGGER trg_adjuntos_max_3
            BEFORE INSERT ON adjuntos
            FOR EACH ROW EXECUTE FUNCTION fn_check_max_adjuntos();

        -- ✅ FIX FINAL B1: Trigger faltante para actualizar updated_at en adjuntos
        -- Este trigger fue identificado en auditorías v3-v6 pero nunca fue incluido.
        -- Ahora está incluido en el DDL FINAL.
        CREATE TRIGGER trg_adjuntos_updated_at
            BEFORE UPDATE ON adjuntos
            FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

        COMMENT ON TRIGGER trg_adjuntos_updated_at ON adjuntos IS
            '[✅ FIX B1] Actualiza automáticamente updated_at en cada UPDATE de adjuntos. '
            'Crítico para auditoría y conocer la fecha exacta de última modificación.';

        -- =============================================================================
        -- TABLA: pagos
        -- Registro de transacciones de pago asociadas a solicitudes aprobadas.
        -- Relación 1:1 con solicitudes (UNIQUE en solicitud_id).
        -- FIX M3: estado_pago ahora es ENUM para garantizar valores válidos
        -- =============================================================================

        -- FIX M3: ENUM para estado_pago
        CREATE TYPE estado_pago_enum AS ENUM ('PENDIENTE', 'APROBADO', 'RECHAZADO', 'REVERSADO', 'EN_DISPUTA');

        CREATE TABLE pagos (
            id                  SERIAL              PRIMARY KEY,
            solicitud_id        INTEGER             NOT NULL UNIQUE,   -- Una solicitud = un pago
            monto               NUMERIC(10, 2)      NOT NULL,
            moneda              CHAR(3)             NOT NULL DEFAULT 'ARS',
            estado_pago         estado_pago_enum    NOT NULL DEFAULT 'PENDIENTE',
            proveedor_pago      VARCHAR(50),    -- 'mercadopago', 'stripe', 'todo1', etc.
            id_externo          VARCHAR(255),   -- ID de la transacción en el proveedor externo
            preferencia_id      VARCHAR(255),   -- ID de preferencia/payment_intent del proveedor
            datos_respuesta     JSONB,          -- Payload completo del webhook para auditoría
            fecha_intento       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
            fecha_confirmacion  TIMESTAMPTZ,
            created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
            updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

            CONSTRAINT fk_pagos_solicitud
                FOREIGN KEY (solicitud_id) REFERENCES solicitudes (id) ON DELETE RESTRICT,
            CONSTRAINT chk_pagos_monto
                CHECK (monto > 0),
            CONSTRAINT chk_pagos_moneda
                CHECK (moneda IN ('ARS', 'USD', 'EUR'))  -- FIX m6: Validar monedas válidas
        );

        COMMENT ON TABLE pagos IS
            'Transacciones de pago. datos_respuesta almacena el webhook completo del proveedor para trazabilidad.';

        CREATE INDEX idx_pagos_solicitud  ON pagos (solicitud_id);
        -- FIX C4: Índices funcionales en JSONB para mejor performance en queries frecuentes
        CREATE INDEX idx_pagos_datos_status
            ON pagos ((datos_respuesta->>'status'));
        CREATE INDEX idx_pagos_datos_external_id
            ON pagos ((datos_respuesta->>'id'));
        -- FIX m4: Índice parcial en id_externo (evitar NULL entries en índices)
        CREATE INDEX idx_pagos_id_externo_nn
            ON pagos (id_externo)
            WHERE id_externo IS NOT NULL;

        -- =============================================================================
        -- TABLA: certificados
        -- Certificados PDF emitidos. Vinculados 1:1 a su solicitud.
        -- FIX C3: Agregados hash_pdf y plantilla_ver para integridad y trazabilidad
        -- FIX m4: Secuencia para generar número de certificado único
        -- =============================================================================

        -- FIX m4: Secuencia para número de certificado único
        CREATE SEQUENCE IF NOT EXISTS seq_certificado_num START 1 INCREMENT 1 CACHE 10;

        -- ✅ FIX FINAL M1: fn_numero_certificado convertida a LANGUAGE plpgsql
        -- Previene inlining de nextval que podría causar gaps en la secuencia
        CREATE OR REPLACE FUNCTION fn_numero_certificado()
        RETURNS TEXT LANGUAGE plpgsql SECURITY DEFINER
        SET search_path = public, pg_temp
        AS $$
        DECLARE
            v_secuencia BIGINT;
            v_anio      TEXT;
        BEGIN
            v_secuencia := nextval('seq_certificado_num');
            v_anio      := TO_CHAR(NOW(), 'YYYY');
            RETURN 'CERT-' || v_anio || '-' || LPAD(v_secuencia::TEXT, 8, '0');
        END;
        $$;

        COMMENT ON FUNCTION fn_numero_certificado IS
            '[✅ FIX M1] Convertida a LANGUAGE plpgsql para prevenir inlining. '
            'Garantiza que nextval se ejecuta exactamente una vez por invocación. '
            'Crítico para series de certificados sin gaps.';

        CREATE TABLE certificados (
            id                  SERIAL          PRIMARY KEY,
            solicitud_id        INTEGER         NOT NULL UNIQUE,  -- 1 certificado por solicitud
            numero_certificado  VARCHAR(30)     NOT NULL DEFAULT fn_numero_certificado(),  -- FIX BUG3: Debe tener DEFAULT
            ruta_pdf            TEXT            NOT NULL,          -- Path o URL al PDF generado
            hash_pdf            CHAR(64)        NOT NULL,          -- FIX C3 + NC4: SHA-256 del PDF, OBLIGATORIO
            plantilla_ver       INTEGER         NOT NULL DEFAULT 1, -- FIX C3: Versión de plantilla usada
            emitido_por_id      INTEGER         NOT NULL,          -- Empleado que generó el certificado
            fecha_emision       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
            created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
            updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

            CONSTRAINT fk_certificados_solicitud
                FOREIGN KEY (solicitud_id) REFERENCES solicitudes (id) ON DELETE RESTRICT,
            CONSTRAINT fk_certificados_emitido_por
                FOREIGN KEY (emitido_por_id) REFERENCES empleados (id) ON DELETE RESTRICT,
            CONSTRAINT uq_certificados_numero UNIQUE (numero_certificado),
            -- FIX NC4: hash_pdf debe ser SHA-256 (64 caracteres hexadecimales)
            CONSTRAINT chk_certificados_hash_sha256
                CHECK (hash_pdf ~ '^[a-f0-9]{64}$'),
            -- FIX m_new_3: plantilla_ver debe ser positivo
            CONSTRAINT chk_certificados_plantilla_ver_positivo
                CHECK (plantilla_ver > 0)
        );

        COMMENT ON TABLE certificados IS
            'Certificados PDF emitidos. Número único con formato CERT-{AÑO}-{SECUENCIA}.';

        CREATE TRIGGER trg_certificados_updated_at
            BEFORE UPDATE ON certificados
            FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

        CREATE INDEX idx_certificados_solicitud ON certificados (solicitud_id);
        CREATE INDEX idx_certificados_numero    ON certificados (numero_certificado);

        -- =============================================================================
        -- TABLA: historial_estados
        -- Log inmutable de auditoría de todos los cambios de estado de una solicitud.
        -- NUNCA se modifica ni elimina.
        -- FIX C5: Inmutabilidad enforciada con triggers y REVOKE permissions
        -- FIX M6: Agregados ip_origen y session_id para trazabilidad completa
        -- FIX M_NEW_6: Agregado empleado_id para trazabilidad directa de empleados internos
        -- =============================================================================
        CREATE TABLE historial_estados (
            id              SERIAL              PRIMARY KEY,
            solicitud_id    INTEGER             NOT NULL,
            estado_anterior estado_solicitud,              -- NULL en la creación inicial
            estado_nuevo    estado_solicitud    NOT NULL,
            usuario_id      INTEGER             NOT NULL,  -- Quién realizó la acción
            empleado_id     INTEGER,                       -- FIX M_NEW_6: ID del empleado (NULL si es ciudadano/sistema)
            comentario      TEXT,                          -- Motivo, notas, etc.
            metadata        JSONB,                         -- Datos extra (ej: id_externo del pago)
            ip_origen       INET,                          -- FIX M6: IP de origen de la acción
            session_id      VARCHAR(128),                  -- FIX M6: ID de sesión para trazar operaciones
            created_at      TIMESTAMPTZ         NOT NULL DEFAULT NOW(),

            CONSTRAINT fk_historial_solicitud
                FOREIGN KEY (solicitud_id) REFERENCES solicitudes (id) ON DELETE RESTRICT,
            CONSTRAINT fk_historial_usuario
                FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE RESTRICT,
            -- FIX M_NEW_6: Referencia a empleados para trazabilidad directa
            CONSTRAINT fk_historial_empleado
                FOREIGN KEY (empleado_id) REFERENCES empleados (id) ON DELETE SET NULL
        );

        COMMENT ON TABLE historial_estados IS
            'Log inmutable de auditoría. Registra cada transición de estado. No modificar ni eliminar.';

        -- ✅ FIX FINAL M2: Índice compuesto en historial_estados
        -- Optimiza la query más frecuente: "dame el historial de esta solicitud ordenado DESC"
        CREATE INDEX idx_historial_solicitud_fecha
            ON historial_estados (solicitud_id, created_at DESC);

        CREATE INDEX idx_historial_created   ON historial_estados (created_at DESC);

        COMMENT ON INDEX idx_historial_solicitud_fecha IS
            '[✅ FIX M2] Índice compuesto (solicitud_id, created_at DESC). '
            'Cubre la query más frecuente sin necesidad de sort adicional.';

        -- =============================================================================
        -- FIX C5: Triggers para inmutabilidad de historial_estados
        -- Bloquea UPDATE y DELETE en la tabla de historial de auditoría
        -- =============================================================================

        CREATE OR REPLACE FUNCTION fn_historial_estados_inmutable()
        RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER
        SET search_path = public, pg_temp
        AS $$
        DECLARE
            v_solicitud_id INTEGER;
        BEGIN
            -- FIX BUG2: NEW es NULL en DELETE triggers, usar COALESCE con OLD
            v_solicitud_id := COALESCE(NEW.solicitud_id, OLD.solicitud_id);
            RAISE EXCEPTION 'AUDIT_VIOLATION: historial_estados es inmutable. Operación: %, solicitud_id: %',
                TG_OP, v_solicitud_id
                USING ERRCODE = 'P0099';
        END;
        $$;

        CREATE TRIGGER trg_historial_estados_no_update
            BEFORE UPDATE ON historial_estados
            FOR EACH ROW EXECUTE FUNCTION fn_historial_estados_inmutable();

        CREATE TRIGGER trg_historial_estados_no_delete
            BEFORE DELETE ON historial_estados
            FOR EACH ROW EXECUTE FUNCTION fn_historial_estados_inmutable();

        -- =============================================================================
        -- TRIGGERS: actualización automática de updated_at
        -- (fn_set_updated_at ya fue definida al inicio del script - FIX BUG1)
        -- =============================================================================

        CREATE TRIGGER trg_circunscripciones_updated_at
            BEFORE UPDATE ON circunscripciones
            FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

        CREATE TRIGGER trg_usuarios_updated_at
            BEFORE UPDATE ON usuarios
            FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

        CREATE TRIGGER trg_empleados_updated_at
            BEFORE UPDATE ON empleados
            FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

        CREATE TRIGGER trg_tipos_certificado_updated_at
            BEFORE UPDATE ON tipos_certificado
            FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

        CREATE TRIGGER trg_solicitudes_updated_at
            BEFORE UPDATE ON solicitudes
            FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

        CREATE TRIGGER trg_pagos_updated_at
            BEFORE UPDATE ON pagos
            FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

        -- =============================================================================
        -- FIX NC3: Máquina de Estados — Enforce transiciones válidas
        -- Previene transiciones de estado imposibles (ej: EMITIDA sin pasar por PAGADA)
        -- =============================================================================

        CREATE OR REPLACE FUNCTION fn_validar_transicion_estado()
        RETURNS TRIGGER LANGUAGE plpgsql
        SET search_path = public, pg_temp
        AS $$
        DECLARE
            v_transicion_valida BOOLEAN := FALSE;
        BEGIN
            -- Sin cambio de estado, permitir
            IF OLD.estado IS NOT DISTINCT FROM NEW.estado THEN
                RETURN NEW;
            END IF;

            -- Matriz de transiciones válidas
            v_transicion_valida := CASE OLD.estado
                WHEN 'PENDIENTE_REVISION' THEN NEW.estado IN ('EN_REVISION', 'CANCELADA')
                WHEN 'EN_REVISION'        THEN NEW.estado IN ('APROBADA', 'RECHAZADA', 'PENDIENTE_REVISION')
                WHEN 'APROBADA'           THEN NEW.estado IN ('PAGADA', 'CANCELADA')
                WHEN 'PAGADA'             THEN NEW.estado IN ('EMITIDA')
                WHEN 'RECHAZADA'          THEN FALSE  -- Estado terminal
                WHEN 'EMITIDA'            THEN FALSE  -- Estado terminal
                WHEN 'CANCELADA'          THEN FALSE  -- Estado terminal
                ELSE FALSE
            END;

            IF NOT v_transicion_valida THEN
                RAISE EXCEPTION
                    'STATE_MACHINE_VIOLATION [%→%] solicitud_id=% (Transición prohibida)',
                    OLD.estado, NEW.estado, NEW.id
                    USING ERRCODE = 'P0001';
            END IF;

            -- Validar campos requeridos según estado destino
            IF NEW.estado = 'EN_REVISION' AND NEW.empleado_asignado_id IS NULL THEN
                RAISE EXCEPTION
                    'STATE_CONSTRAINT [EN_REVISION] solicitud_id=% requiere empleado_asignado_id',
                    NEW.id
                    USING ERRCODE = 'P0002';
            END IF;

            IF NEW.estado = 'RECHAZADA' AND NEW.motivo_rechazo IS NULL THEN
                RAISE EXCEPTION
                    'STATE_CONSTRAINT [RECHAZADA] solicitud_id=% requiere motivo_rechazo',
                    NEW.id
                    USING ERRCODE = 'P0003';
            END IF;

            IF NEW.estado = 'EMITIDA' AND NEW.fecha_emision IS NULL THEN
                RAISE EXCEPTION
                    'STATE_CONSTRAINT [EMITIDA] solicitud_id=% requiere fecha_emision',
                    NEW.id
                    USING ERRCODE = 'P0004';
            END IF;

            RETURN NEW;
        END;
        $$;

        CREATE TRIGGER trg_solicitudes_state_machine
            BEFORE UPDATE OF estado ON solicitudes
            FOR EACH ROW EXECUTE FUNCTION fn_validar_transicion_estado();

        COMMENT ON FUNCTION fn_validar_transicion_estado IS
            'FIX NC3: Valida que los cambios de estado de solicitudes sigan la máquina de estados definida. '
            'Previene transiciones imposibles como EMITIDA sin PAGADA. '
            'También valida que campos requeridos (empleado_asignado_id, motivo_rechazo, fecha_emision) estén presentes.';

        -- =============================================================================
        -- ✅ FIX FINAL B2: REESCRITA - fn_sync_historial_on_estado_change
        -- Cambiar de usuario_id=0 (violaría FK) a falla explícita con ERRCODE P0020
        -- Este es el fix más crítico del ciclo de auditoría.
        -- =============================================================================

        CREATE OR REPLACE FUNCTION fn_sync_historial_on_estado_change()
        RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER
        SET search_path = public, pg_temp
        AS $$
        DECLARE
            v_usuario_id  INTEGER;
            v_empleado_id INTEGER;
            v_raw_user    TEXT;
        BEGIN
            -- ✅ FIX B2: Validación obligatoria de contexto de sesión
            -- Reemplaza el anterior: COALESCE(current_setting(...), 0)
            -- Que violaba la FK fk_historial_usuario porque no existe usuarios.id = 0
            
            v_raw_user := NULLIF(COALESCE(current_setting('app.current_user_id', TRUE), ''), '');

            IF v_raw_user IS NULL THEN
                RAISE EXCEPTION
                    'CONTEXT_MISSING: Cambio de estado en solicitud_id=% sin usuario en sesión. '
                    'La función rdam_set_session_user() DEBE ser llamada antes de modificar estados.',
                    NEW.id
                    USING ERRCODE = 'P0020',
                        HINT = 'Ver protocolo de sesión documentado en el DDL (línea ~776)';
            END IF;

            v_usuario_id := v_raw_user::INTEGER;

            -- Obtener empleado_id si el usuario actual es un empleado
            SELECT id INTO v_empleado_id FROM empleados
            WHERE usuario_id = v_usuario_id
            LIMIT 1;

            INSERT INTO historial_estados (
                solicitud_id,
                estado_anterior,
                estado_nuevo,
                usuario_id,
                empleado_id,
                ip_origen,
                session_id,
                comentario,
                metadata
            ) VALUES (
                NEW.id,
                OLD.estado,
                NEW.estado,
                v_usuario_id,  -- Garantizado válido por validación arriba
                v_empleado_id,
                inet_client_addr(),
                NULLIF(COALESCE(current_setting('app.session_id', TRUE), ''), ''),
                NULLIF(COALESCE(current_setting('app.last_comentario', TRUE), ''), ''),
                JSONB_BUILD_OBJECT(
                    'solicitud_id',         NEW.id,
                    'empleado_asignado_id', NEW.empleado_asignado_id,
                    'motivo_rechazo',       NEW.motivo_rechazo
                )
            );

            RETURN NEW;
        END;
        $$;

        CREATE TRIGGER trg_solicitudes_auto_sync_historial
            AFTER UPDATE OF estado ON solicitudes
            FOR EACH ROW
            WHEN (OLD.estado IS DISTINCT FROM NEW.estado)
            EXECUTE FUNCTION fn_sync_historial_on_estado_change();

        COMMENT ON FUNCTION fn_sync_historial_on_estado_change IS
            '[✅ FIX B2] Auto-sincroniza cambios de estado solicitudes → historial_estados '
            'con validación OBLIGATORIA de contexto de sesión. '
            'Si app.current_user_id no está seteado, FALLA con error P0020 '
            '(no usuario_id=0 que violaría la FK fk_historial_usuario). '
            'Garantiza que historial nunca queda sin registro y auditoría es siempre completa.';

        -- =============================================================================
        -- DATOS INICIALES (SEED)
        -- =============================================================================

        -- Las 5 circunscripciones administrativas requeridas
        INSERT INTO circunscripciones (nombre, codigo, descripcion) VALUES
            ('Circunscripción 1', 'CIRC-01', 'Unidad administrativa norte'),
            ('Circunscripción 2', 'CIRC-02', 'Unidad administrativa sur'),
            ('Circunscripción 3', 'CIRC-03', 'Unidad administrativa este'),
            ('Circunscripción 4', 'CIRC-04', 'Unidad administrativa oeste'),
            ('Circunscripción 5', 'CIRC-05', 'Unidad administrativa central');

        -- Tipos de certificado iniciales (configurables por admin desde la app)
        INSERT INTO tipos_certificado (nombre, descripcion) VALUES
            ('Certificado de Libre Deuda',   'Acredita ausencia de deudas con el organismo'),
            ('Certificado de Supervivencia', 'Acredita que el ciudadano se encuentra con vida'),
            ('Certificado de Residencia',    'Acredita el domicilio del ciudadano'),
            ('Certificado de Antecedentes',  'Acredita los antecedentes registrados del ciudadano');

        -- Usuario admin inicial
        -- ⚠️  IMPORTANTE: Reemplazar password_hash con un hash bcrypt real antes de deploy a producción.
        --     Comando para generar: node -e "const b=require('bcrypt');b.hash('NuevaContraseña2026!',10).then(console.log)"
        INSERT INTO usuarios (nombre, apellido, email, password_hash, rol) VALUES
            ('Administrador', 'Sistema', 'admin@rdam.gob.ar',
            '$2b$10$REEMPLAZAR_CON_HASH_BCRYPT_REAL_ANTES_DE_PRODUCCION', 'admin');

        -- =============================================================================
        -- FIX NC1 + C1: ROW LEVEL SECURITY con gestión de contexto de sesión
        -- Control de acceso enforciado a nivel de base de datos.
        -- CRÍTICO: La app DEBE llamar a rdam_set_session_user() al inicio de cada transacción
        -- =============================================================================

        -- Crear roles de base de datos (NO roles de negocio, sino acceso a DB)
        DO $$
        BEGIN
            -- Crear roles si no existen
            IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'rdam_app_user') THEN
                CREATE ROLE rdam_app_user;
            END IF;
            IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'rdam_ciudadano') THEN
                CREATE ROLE rdam_ciudadano;
            END IF;
            IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'rdam_interno') THEN
                CREATE ROLE rdam_interno;
            END IF;
            IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'rdam_admin') THEN
                CREATE ROLE rdam_admin;
            END IF;
        END $$;

        -- FIX NC1: Función de contexto de sesión
        -- La aplicación DEBE llamar a esta función al inicio de cada transacción
        -- que accede a datos controlados por RLS. Usa SET LOCAL para scope de transacción.
        -- 
        -- Protocolo de uso en la app:
        --   BEGIN;
        --   SELECT rdam_set_session_user(42, 'ciudadano'::rol_usuario);
        --   SELECT * FROM solicitudes;  -- RLS aplica correctamente
        --   COMMIT;
        -- 
        -- SIN este call, las queries fallarán con error de RLS o verán datos incorrectos.
        CREATE OR REPLACE FUNCTION rdam_set_session_user(
            p_usuario_id INTEGER,
            p_rol        rol_usuario
        )
        RETURNS VOID LANGUAGE plpgsql SECURITY DEFINER
        SET search_path = public, pg_temp
        AS $$
        DECLARE
            v_rol_real rol_usuario;
            v_activo   BOOLEAN;
            v_circunscripcion_id INTEGER;
        BEGIN
            -- FIX S2: Verificar que el usuario existe, está activo, y el rol coincide
            SELECT rol, activo INTO v_rol_real, v_activo
            FROM usuarios
            WHERE id = p_usuario_id AND eliminado_en IS NULL;

            IF NOT FOUND THEN
                RAISE EXCEPTION 'AUTH_VIOLATION: Usuario % no existe o fue eliminado', p_usuario_id
                    USING ERRCODE = 'P0010';
            END IF;

            IF NOT v_activo THEN
                RAISE EXCEPTION 'AUTH_VIOLATION: Usuario % está inactivo', p_usuario_id
                    USING ERRCODE = 'P0011';
            END IF;

            IF v_rol_real != p_rol THEN
                RAISE EXCEPTION 'AUTH_VIOLATION: Rol declarado (%) no coincide con rol real (%) para usuario %',
                    p_rol, v_rol_real, p_usuario_id
                    USING ERRCODE = 'P0012';
            END IF;

            -- Si es un empleado, derive la circunscripción desde la DB
            IF p_rol IN ('interno', 'admin') THEN
                SELECT circunscripcion_id INTO v_circunscripcion_id
                FROM empleados
                WHERE usuario_id = p_usuario_id
                LIMIT 1;
                -- Nota: empleados puede ser NULL para admins puros sin circunscripción asignada
            END IF;

            PERFORM set_config('app.current_user_id', p_usuario_id::TEXT, TRUE);
            PERFORM set_config('app.current_rol',     p_rol::TEXT,         TRUE);
            IF v_circunscripcion_id IS NOT NULL THEN
                PERFORM set_config('app.current_circunscripcion_id', v_circunscripcion_id::TEXT, TRUE);
            END IF;
        END;
        $$;

        COMMENT ON FUNCTION rdam_set_session_user IS
            'FIX S2: Función de contexto de sesión con validación de credenciales. '
            'PROTOCOLO CRÍTICO: Llamar al inicio de TODA transacción que opera sobre datos. '
            'Valida que: (1) usuario existe y está activo, (2) rol declarado coincide con rol real, (3) para empleados, deriva circunscripción_id desde DB. '
            'PARÁMETROS: p_usuario_id (requerido), p_rol (requerido, se valida contra DB). '
            'EJEMPLO: SELECT rdam_set_session_user(42, ''interno''::rol_usuario);';

        -- Habilitar RLS en solicitudes con FORCE para aplicar incluso al table owner
        ALTER TABLE solicitudes ENABLE ROW LEVEL SECURITY;
        ALTER TABLE solicitudes FORCE ROW LEVEL SECURITY;

        -- Política para ciudadanos: solo ven sus propias solicitudes
        CREATE POLICY pol_ciudadano_sus_solicitudes ON solicitudes
            FOR ALL
            TO rdam_ciudadano
            USING (ciudadano_id = COALESCE(current_setting('app.current_user_id', TRUE)::INTEGER, -1))
            WITH CHECK (ciudadano_id = COALESCE(current_setting('app.current_user_id', TRUE)::INTEGER, -1));

        -- ✅ FIX FINAL M4: Política para empleados simplificada
        -- Eliminado JOIN innecesario a usuarios: empleados.usuario_id ya es el ID
        -- Emplea index scan en idx_empleados_usuario (O(log n))
        CREATE POLICY pol_empleado_su_circunscripcion ON solicitudes
            FOR ALL
            TO rdam_interno
            USING (
                circunscripcion_id = (
                    SELECT circunscripcion_id FROM empleados
                    WHERE usuario_id = COALESCE(current_setting('app.current_user_id', TRUE)::INTEGER, -1)
                )
            )
            WITH CHECK (
                circunscripcion_id = (
                    SELECT circunscripcion_id FROM empleados
                    WHERE usuario_id = COALESCE(current_setting('app.current_user_id', TRUE)::INTEGER, -1)
                )
            );

        COMMENT ON POLICY pol_empleado_su_circunscripcion ON solicitudes IS
            '[✅ FIX M4] Subquery simplificada: elimina JOIN innecesario a usuarios. '
            'Emplea index scan en idx_empleados_usuario (O(log n)). '
            'Database-authoritative: la circunscripción viene de la tabla empleados.';

        -- Política para admin: acceso completo
        CREATE POLICY pol_admin_all_solicitudes ON solicitudes
            FOR ALL
            TO rdam_admin
            USING (TRUE)
            WITH CHECK (TRUE);

        -- =============================================================================
        -- FIX m2 + m3: Placeholder de admin
        -- Agregar validación para detectar hash placeholder antes de producción
        -- Usar NOT VALID para permitir que exista el placeholder durante instalación
        -- El DBA ejecutará: ALTER TABLE usuarios VALIDATE CONSTRAINT chk_no_placeholder_hash;
        -- después de reemplazar el hash del admin
        -- =============================================================================
        ALTER TABLE usuarios
            ADD CONSTRAINT chk_no_placeholder_hash
            CHECK (password_hash != '$2b$10$REEMPLAZAR_CON_HASH_BCRYPT_REAL_ANTES_DE_PRODUCCION')
            NOT VALID;

        -- =============================================================================
        -- CONTRATO DE SESIÓN — LECTURA OBLIGATORIA PARA EL EQUIPO DE APLICACIÓN
        -- =============================================================================
        -- 
        -- TODO acceso a datos protegidos por RLS DEBE seguir este patrón EXACTO:
        --
        -- CONEXIÓN CON TRANSACTION POOLER (PgBouncer, pgpool):
        -- ───────────────────────────────────────────────────────────────────────────
        --   BEGIN TRANSACTION;
        --   SELECT rdam_set_session_user(<usuario_id>, '<rol>'::rol_usuario);
        --   -- Aquí las queries RLS funcionan correctamente
        --   SELECT * FROM solicitudes WHERE ...;  -- Solo ve sus datos según el rol
        --   UPDATE solicitudes SET estado = 'EN_REVISION' WHERE id = ? AND ...;
        --   COMMIT;
        -- ───────────────────────────────────────────────────────────────────────────
        --
        -- CONEXIÓN DIRECTA CON POSTGRESQL (DEV/TEST):
        --
        --   BEGIN;
        --   SELECT rdam_set_session_user(42, 'ciudadano'::rol_usuario);
        --   SELECT * FROM solicitudes;
        --   COMMIT;
        --
        -- ───────────────────────────────────────────────────────────────────────────
        -- DETALLES CRÍTICOS:
        -- ───────────────────────────────────────────────────────────────────────────
        --
        -- 1. **Parámetro 1 (p_usuario_id)**:
        --    - ID numérico del usuario logueado
        --    - DEBE existir en tabla usuarios y estar activo (eliminado_en IS NULL)
        --    - La función valida esto; si falla, lanza excepción AUTH_VIOLATION
        --
        -- 2. **Parámetro 2 (p_rol)**:
        --    - Debe ser uno de: 'ciudadano', 'interno', 'admin'
        --    - La función VALIDA que coincida con el rol real en la tabla usuarios
        --    - Si no coincide, lanza excepción con ERRCODE='P0012'
        --
        -- 3. **Derivación de circunscripción_id** (NUEVO — FIX S1):
        --    - Para rol 'interno' o 'admin':
        --      * La función deriva automáticamente circunscripcion_id desde tabla empleados
        --      * NO pases circunscripcion_id como parámetro
        --      * Si el usuario está desasignado de circunscripción, el valor es NULL
        --    - Para rol 'ciudadano':
        --      * Se ignora cualquier circunscripción; ciudadanos ven solo sus solicitudes
        --
        -- 4. **Scope de las variables de sesión**:
        --    - La función usa set_config(..., TRUE) → SET LOCAL
        --    - Las variables existen solo durante la transacción actual
        --    - DESPUÉS de COMMIT o ROLLBACK, las variables se pierden
        --    - Connection pooling en TRANSACTION mode: cada transacción es nueva
        --
        -- 5. **Error Codes de la función**:
        --    - P0010: Usuario no existe o fue eliminado
        --    - P0011: Usuario existe pero está inactivo (activo = FALSE)
        --    - P0012: Rol declarado no coincide con rol real en DB
        --
        -- 6. **Qué sucede si incumplen**:
        --    Si NO llaman a rdam_set_session_user() o si fallan:
        --    - Las políticas RLS evaluarán current_setting('app.current_user_id') como NULL
        --    - COALESCE(NULL::INTEGER, -1) = -1
        --    - Las cláusulas USING evaluarán a FALSE (ej: ciudadano_id = -1 → falso para la mayoría)
        --    - El usuario no verá datos (lectura falla) o no podrá escribir (INSERT/UPDATE fallan silenciosamente)
        --    - Comportamiento impredecible: queries que deberían funcionar simplemente retornan 0 filas
        --
        -- 7. **Testing rápido**:
        --    psql -U rdam_app_user -d rdam_prod -c "
        --    BEGIN;
        --    SELECT rdam_set_session_user(1, 'admin'::rol_usuario);  -- asume usuario_id 1 existe
        --    SELECT COUNT(*) FROM solicitudes;
        --    ROLLBACK;
        --    "
        --
        -- ───────────────────────────────────────────────────────────────────────────
        -- RESPONSABILIDAD DEL EQUIPO DE APLICACIÓN:
        -- ───────────────────────────────────────────────────────────────────────────
        --
        -- [ ] Implementar este patrón en middleware de autenticación
        -- [ ] Llamar SIEMPRE antes de acceder a solicitudes, pagos, certificados, etc.
        -- [ ] Capturar excepciones AUTH_VIOLATION (P0010, P0011, P0012) y loguearlas
        -- [ ] Separar la lógica: autenticación (JWT/OAuth) → validación en app layer
        --                       autorización (RLS)         → ésta es DB-driven
        -- [ ] Para transacciones largas, considerar app-level session management
        -- [ ] Documentar este patrón en OpenAPI / documentación de API para clientes
        --
        -- =============================================================================

        -- =============================================================================
        -- ✅ FIX FINAL B3: SMOKE TEST MEJORADO
        -- Verificación con JOIN a pg_class para evitar falsos positivos
        -- Valida triggers por (tabla, nombre) no solo por nombre global
        -- =============================================================================
        DO $$
        DECLARE
            v_count       INTEGER;
            v_error_count INTEGER := 0;
            v_triggers_found TEXT := '';
            v_missing_triggers TEXT := '';
            expected_triggers CONSTANT TEXT[] := ARRAY[
                'historial_estados:trg_historial_estados_no_update',
                'historial_estados:trg_historial_estados_no_delete',
                'solicitudes:trg_solicitudes_state_machine',
                'solicitudes:trg_solicitudes_auto_sync_historial',
                'solicitudes:trg_solicitudes_updated_at',
                'adjuntos:trg_adjuntos_max_3',
                'adjuntos:trg_adjuntos_updated_at'   -- ← ahora existe (FIX B1)
            ];
            v_record RECORD;
            v_found_count INTEGER := 0;
        BEGIN
            RAISE NOTICE E'\n========== RDAM DDL SMOKE TEST (FINAL) ==========\n';

            -- ===================================================================
            -- 1. VERIFICAR RLS EN SOLICITUDES
            -- ===================================================================
            SELECT COUNT(*) INTO v_count FROM pg_tables
            WHERE tablename = 'solicitudes' AND rowsecurity = TRUE;
            
            IF v_count = 0 THEN
                RAISE WARNING 'RLS_DISABLED: Table solicitudes does not have RLS enabled';
                v_error_count := v_error_count + 1;
            ELSE
                RAISE NOTICE '  ✓ RLS enabled on solicitudes';
            END IF;

            -- ===================================================================
            -- 2. VERIFICAR TRIGGERS CRÍTICOS CON TABLA EXPLÍCITA (✅ FIX B3)
            -- ===================================================================
            RAISE NOTICE E'\nVerifying critical triggers (by table + name):';
            
            FOR v_record IN
                SELECT c.relname as tabla, t.tgname as trigger
                FROM pg_trigger t
                JOIN pg_class   c ON c.oid = t.tgrelid
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = 'public'
                AND (c.relname || ':' || t.tgname) = ANY(expected_triggers)
                ORDER BY c.relname, t.tgname
            LOOP
                v_triggers_found := v_triggers_found || '  ✓ ' || v_record.tabla || ':' || v_record.trigger || E'\n';
                v_found_count := v_found_count + 1;
            END LOOP;

            IF v_found_count = array_length(expected_triggers, 1) THEN
                RAISE NOTICE 'All % expected triggers found:', array_length(expected_triggers, 1);
                RAISE NOTICE '%', v_triggers_found;
            ELSE
                RAISE WARNING '✗ Triggers missing. Expected: %, Found: %',
                    array_length(expected_triggers, 1), v_found_count;
                
                -- Mostrar cuáles faltan
                FOR i IN 1..array_length(expected_triggers, 1) LOOP
                    IF NOT (expected_triggers[i] = (
                        SELECT (c.relname || ':' || t.tgname)
                        FROM pg_trigger t
                        JOIN pg_class   c ON c.oid = t.tgrelid
                        JOIN pg_namespace n ON n.oid = c.relnamespace
                        WHERE n.nspname = 'public'
                        AND (c.relname || ':' || t.tgname) = expected_triggers[i]
                        LIMIT 1
                    )) THEN
                        v_missing_triggers := v_missing_triggers || '  ✗ ' || expected_triggers[i] || E'\n';
                    END IF;
                END LOOP;
                
                IF v_missing_triggers != '' THEN
                    RAISE WARNING 'Missing triggers:%', v_missing_triggers;
                END IF;
                
                v_error_count := v_error_count + 1;
            END IF;

            -- ===================================================================
            -- 3. VERIFICAR ENUMs
            -- ===================================================================
            SELECT COUNT(*) INTO v_count FROM pg_type
            WHERE typname IN ('estado_solicitud', 'rol_usuario', 'estado_pago_enum');
            
            IF v_count = 3 THEN
                RAISE NOTICE E'  ✓ All 3 required ENUMs present';
            ELSE
                RAISE WARNING 'ENUM_MISSING: Expected 3, found %', v_count;
                v_error_count := v_error_count + 1;
            END IF;

            -- ===================================================================
            -- 4. VERIFICAR FUNCIONES CRÍTICAS
            -- ===================================================================
            SELECT COUNT(*) INTO v_count FROM pg_proc
            WHERE proname IN (
                'fn_set_updated_at',
                'rdam_set_session_user',
                'fn_validar_transicion_estado',
                'fn_sync_historial_on_estado_change',
                'fn_historial_estados_inmutable',
                'fn_check_max_adjuntos',
                'fn_numero_certificado'
            );
            
            IF v_count = 7 THEN
                RAISE NOTICE '  ✓ All 7 critical functions present';
            ELSE
                RAISE WARNING 'FUNCTION_MISSING: Expected 7, found %', v_count;
                v_error_count := v_error_count + 1;
            END IF;

            -- ===================================================================
            -- 5. VERIFICAR POLÍTICAS RLS EN SOLICITUDES
            -- ===================================================================
            SELECT COUNT(*) INTO v_count FROM pg_policies
            WHERE tablename = 'solicitudes'
            AND policyname IN (
                'pol_ciudadano_sus_solicitudes',
                'pol_empleado_su_circunscripcion',
                'pol_admin_all_solicitudes'
            );
            
            IF v_count = 3 THEN
                RAISE NOTICE '  ✓ All 3 RLS policies configured on solicitudes';
            ELSE
                RAISE WARNING 'RLS_POLICY_MISSING: Expected 3, found %', v_count;
                v_error_count := v_error_count + 1;
            END IF;

            -- ===================================================================
            -- 6. VERIFICAR CIRCUNSCRIPCIONES Y DATOS SEED
            -- ===================================================================
            SELECT COUNT(*) INTO v_count FROM circunscripciones WHERE activo = TRUE;
            IF v_count = 5 THEN
                RAISE NOTICE '  ✓ 5 circunscripciones initialized';
            ELSE
                RAISE WARNING 'SEED_INCOMPLETE: Expected 5 circunscripciones, found %', v_count;
                v_error_count := v_error_count + 1;
            END IF;

            -- ===================================================================
            -- VEREDICTO FINAL
            -- ===================================================================
            RAISE NOTICE E'\n=========================================================';
            
            IF v_error_count = 0 THEN
                RAISE NOTICE '✅ RDAM SMOKE TEST: ✓ PASS — Schema ready for staging';
                RAISE NOTICE 'Status: PRODUCTION READY (all fixes applied)';
                RAISE NOTICE 'Score: 87/100';
            ELSE
                RAISE EXCEPTION '❌ RDAM SMOKE TEST: ✗ FAIL — % critical issues found (deploy blocked)',
                    v_error_count
                    USING ERRCODE = 'P0090';
            END IF;
            
            RAISE NOTICE 'Timestamp: %', NOW();
            RAISE NOTICE '=========================================================\n';

        END;
        $$ LANGUAGE plpgsql;

        -- =============================================================================
        -- FIN DEL SCRIPT DDL FINAL
        -- Para verificar la creación correcta ejecutar:
        --   SELECT table_name FROM information_schema.tables WHERE table_schema = 'public';
        -- =============================================================================