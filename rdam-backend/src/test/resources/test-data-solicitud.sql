-- Test data for SolicitudIntegrationTest
-- Lookup tables (circunscripciones, tipos_certificado) already exist from init.sql
-- Password "Test1234" hashed with BCrypt

-- Ciudadano principal (id=100)
INSERT INTO usuarios (id, nombre, apellido, email, cuil, password_hash, rol, activo, created_at, updated_at)
VALUES (100, 'Test', 'Ciudadano', 'ciudadano-test@test.com', '20222222222',
        '$2a$10$e0ekfmlPM1Ih3CV7yuonqOXk25uGdds3SdKnPeJZRYyjFORoCZlmu',
        'ciudadano', true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Empleado interno (usuario id=101)
INSERT INTO usuarios (id, nombre, apellido, email, cuil, password_hash, rol, activo, created_at, updated_at)
VALUES (101, 'Test', 'Interno', 'interno-test@test.com', '20222222223',
        '$2a$10$e0ekfmlPM1Ih3CV7yuonqOXk25uGdds3SdKnPeJZRYyjFORoCZlmu',
        'interno', true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Empleado vinculado al usuario interno (circunscripcion_id=1)
INSERT INTO empleados (usuario_id, circunscripcion_id, legajo, cargo, created_at, updated_at)
VALUES (101, 1, 'LEG-TEST-001', 'Analista', NOW(), NOW())
ON CONFLICT (usuario_id) DO NOTHING;

-- Otro ciudadano para test de acceso denegado (id=102)
INSERT INTO usuarios (id, nombre, apellido, email, cuil, password_hash, rol, activo, created_at, updated_at)
VALUES (102, 'Otro', 'Ciudadano', 'otro-ciudadano@test.com', '20333333333',
        '$2a$10$e0ekfmlPM1Ih3CV7yuonqOXk25uGdds3SdKnPeJZRYyjFORoCZlmu',
        'ciudadano', true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;
