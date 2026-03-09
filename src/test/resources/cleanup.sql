-- Cleanup test data (order matters due to FK constraints)
-- Temporarily disable immutability trigger on historial_estados
ALTER TABLE historial_estados DISABLE TRIGGER trg_historial_estados_no_delete;
DELETE FROM historial_estados WHERE solicitud_id IN (SELECT id FROM solicitudes WHERE ciudadano_id >= 100);
ALTER TABLE historial_estados ENABLE TRIGGER trg_historial_estados_no_delete;
DELETE FROM pagos WHERE solicitud_id IN (SELECT id FROM solicitudes WHERE ciudadano_id >= 100);
DELETE FROM certificados WHERE solicitud_id IN (SELECT id FROM solicitudes WHERE ciudadano_id >= 100);
DELETE FROM adjuntos WHERE solicitud_id IN (SELECT id FROM solicitudes WHERE ciudadano_id >= 100);
DELETE FROM solicitudes WHERE ciudadano_id >= 100;
DELETE FROM empleados WHERE usuario_id >= 100;
DELETE FROM usuarios WHERE id >= 100;
DELETE FROM usuarios WHERE cuil = '20111111111';
