-- =============================================================================
-- V2: Migracion de estados y numero de tramite
-- Compatible con PostgreSQL 15
-- =============================================================================

BEGIN;

-- =============================================================================
-- PARTE 1: Migracion de estados
-- =============================================================================

-- 1a. Agregar nuevos valores al enum estado_solicitud
ALTER TYPE estado_solicitud ADD VALUE IF NOT EXISTS 'PENDIENTE';
ALTER TYPE estado_solicitud ADD VALUE IF NOT EXISTS 'PAGADO';
ALTER TYPE estado_solicitud ADD VALUE IF NOT EXISTS 'PUBLICADO';
ALTER TYPE estado_solicitud ADD VALUE IF NOT EXISTS 'PUBLICADO_VENCIDO';
ALTER TYPE estado_solicitud ADD VALUE IF NOT EXISTS 'VENCIDO';
ALTER TYPE estado_solicitud ADD VALUE IF NOT EXISTS 'RECHAZADO';
ALTER TYPE estado_solicitud ADD VALUE IF NOT EXISTS 'CANCELADO';

COMMIT;

-- Los ADD VALUE de enum no pueden estar dentro de una transaccion que haga UPDATE
-- con los nuevos valores. Se necesita un COMMIT intermedio.

BEGIN;

-- 1b. Migrar datos en tabla solicitudes
UPDATE solicitudes SET estado = 'PENDIENTE'  WHERE estado IN ('PENDIENTE_REVISION', 'EN_REVISION', 'APROBADA');
UPDATE solicitudes SET estado = 'PAGADO'     WHERE estado = 'PAGADA';
UPDATE solicitudes SET estado = 'PUBLICADO'  WHERE estado = 'EMITIDA';
UPDATE solicitudes SET estado = 'RECHAZADO'  WHERE estado = 'RECHAZADA';
UPDATE solicitudes SET estado = 'CANCELADO'  WHERE estado = 'CANCELADA';

-- 1c. Migrar datos en tabla historial_estados (estado_anterior y estado_nuevo)
UPDATE historial_estados SET estado_anterior = 'PENDIENTE'  WHERE estado_anterior IN ('PENDIENTE_REVISION', 'EN_REVISION', 'APROBADA');
UPDATE historial_estados SET estado_anterior = 'PAGADO'     WHERE estado_anterior = 'PAGADA';
UPDATE historial_estados SET estado_anterior = 'PUBLICADO'  WHERE estado_anterior = 'EMITIDA';
UPDATE historial_estados SET estado_anterior = 'RECHAZADO'  WHERE estado_anterior = 'RECHAZADA';
UPDATE historial_estados SET estado_anterior = 'CANCELADO'  WHERE estado_anterior = 'CANCELADA';

UPDATE historial_estados SET estado_nuevo = 'PENDIENTE'  WHERE estado_nuevo IN ('PENDIENTE_REVISION', 'EN_REVISION', 'APROBADA');
UPDATE historial_estados SET estado_nuevo = 'PAGADO'     WHERE estado_nuevo = 'PAGADA';
UPDATE historial_estados SET estado_nuevo = 'PUBLICADO'  WHERE estado_nuevo = 'EMITIDA';
UPDATE historial_estados SET estado_nuevo = 'RECHAZADO'  WHERE estado_nuevo = 'RECHAZADA';
UPDATE historial_estados SET estado_nuevo = 'CANCELADO'  WHERE estado_nuevo = 'CANCELADA';

-- =============================================================================
-- PARTE 2: Actualizar trigger de maquina de estados
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

    -- Matriz de transiciones validas (nuevo flujo)
    v_transicion_valida := CASE OLD.estado
        WHEN 'PENDIENTE'         THEN NEW.estado IN ('PAGADO', 'RECHAZADO', 'CANCELADO')
        WHEN 'PAGADO'            THEN NEW.estado IN ('PUBLICADO', 'VENCIDO')
        WHEN 'PUBLICADO'         THEN NEW.estado IN ('PUBLICADO_VENCIDO')
        WHEN 'PUBLICADO_VENCIDO' THEN FALSE  -- Estado terminal
        WHEN 'VENCIDO'           THEN FALSE  -- Estado terminal
        WHEN 'RECHAZADO'         THEN FALSE  -- Estado terminal
        WHEN 'CANCELADO'         THEN FALSE  -- Estado terminal
        ELSE FALSE
    END;

    IF NOT v_transicion_valida THEN
        RAISE EXCEPTION
            'STATE_MACHINE_VIOLATION [%->%] solicitud_id=% (Transicion prohibida)',
            OLD.estado, NEW.estado, NEW.id
            USING ERRCODE = 'P0001';
    END IF;

    -- Validar campos requeridos segun estado destino
    IF NEW.estado = 'RECHAZADO' AND NEW.motivo_rechazo IS NULL THEN
        RAISE EXCEPTION
            'STATE_CONSTRAINT [RECHAZADO] solicitud_id=% requiere motivo_rechazo',
            NEW.id
            USING ERRCODE = 'P0003';
    END IF;

    IF NEW.estado = 'PUBLICADO' AND NEW.fecha_emision IS NULL THEN
        RAISE EXCEPTION
            'STATE_CONSTRAINT [PUBLICADO] solicitud_id=% requiere fecha_emision',
            NEW.id
            USING ERRCODE = 'P0004';
    END IF;

    RETURN NEW;
END;
$$;

-- =============================================================================
-- PARTE 3: Numero de tramite
-- =============================================================================

-- 3a. Agregar columna numero_tramite
ALTER TABLE solicitudes ADD COLUMN numero_tramite VARCHAR(20) UNIQUE;

-- 3b. Generar valores para registros existentes
UPDATE solicitudes
SET numero_tramite = 'RDAM-' || TO_CHAR(created_at, 'YYYYMMDD') || '-' || LPAD(id::text, 4, '0');

-- 3c. Hacer NOT NULL
ALTER TABLE solicitudes ALTER COLUMN numero_tramite SET NOT NULL;

COMMIT;
