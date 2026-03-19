-- Test data for AuthIntegrationTest
-- Password "Test1234" hashed with BCrypt
INSERT INTO usuarios (nombre, apellido, email, cuil, password_hash, rol, activo, created_at, updated_at)
VALUES ('Test', 'Ciudadano', 'test-ciudadano@test.com', '20111111111',
        '$2a$10$e0ekfmlPM1Ih3CV7yuonqOXk25uGdds3SdKnPeJZRYyjFORoCZlmu',
        'ciudadano', true, NOW(), NOW())
ON CONFLICT (cuil) DO NOTHING;
