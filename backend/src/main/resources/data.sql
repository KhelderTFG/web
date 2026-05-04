-- ============================================================
-- KHELDER — Datos de prueba para desarrollo
-- Se ejecuta automáticamente al arrancar Spring Boot
-- ============================================================

-- Limpieza previa para evitar duplicados en reinicios
-- El orden importa por las foreign keys
TRUNCATE TABLE safe_zone        CASCADE;
TRUNCATE TABLE reminder         CASCADE;
TRUNCATE TABLE gps_location     CASCADE;
TRUNCATE TABLE alert            CASCADE;
TRUNCATE TABLE biometric_record CASCADE;
TRUNCATE TABLE caregiver_patient CASCADE;
TRUNCATE TABLE medical_history  CASCADE;
TRUNCATE TABLE smartwatch       CASCADE;
TRUNCATE TABLE patient          CASCADE;
TRUNCATE TABLE caregiver        CASCADE;

-- ============================================================
-- CUIDADORES
-- Contraseña de todos: "password123"
-- Hash BCrypt de "password123"
-- ============================================================

INSERT INTO caregiver (caregiver_id, name, email, password, phone) VALUES
(
    '536e686c-7f3a-4469-80e9-383750059361',
    'Carlos Martínez',
    'carlos@khelder.com',
    '$2a$10$uU7l2VxL11msFaegHAU0feMR/p16jYjLAU1dpX6PEczzuLwZTfMvq',
    '600111222'
),
(
    '9f7e7747-0744-4860-9975-d5e855c4d516',
    'Ana López',
    'ana@khelder.com',
    '$2a$10$uU7l2VxL11msFaegHAU0feMR/p16jYjLAU1dpX6PEczzuLwZTfMvq',
    '600333444'
);

-- ============================================================
-- PACIENTES
-- ============================================================
-- María:  7928e23f-e89a-4f51-87a3-535316335f60
-- José:   36979607-4e78-436f-9818-43d937060592
-- Carmen: f04085f1-395d-4f10-9f5e-441639f7831d

INSERT INTO patient (patient_id, full_name, date_of_birth) VALUES
('7928e23f-e89a-4f51-87a3-535316335f60', 'María Antonia García', '1945-03-15'),
('36979607-4e78-436f-9818-43d937060592', 'José Manuel Rodríguez', '1938-07-22'),
('f04085f1-395d-4f10-9f5e-441639f7831d', 'Carmen Sánchez Pérez', '1950-11-08');

-- ============================================================
-- HISTORIAL MÉDICO
-- ============================================================

INSERT INTO medical_history (history_id, patient_id, blood_type, allergies, chronic_conditions, emergency_instructions) VALUES
(
    gen_random_uuid(), '7928e23f-e89a-4f51-87a3-535316335f60', 'A+', 
    'Penicilina, Ibuprofeno', 'Hipertensión arterial, Diabetes tipo 2', 
    'Contactar con hijo Carlos: 600111222.'
),
(
    gen_random_uuid(), '36979607-4e78-436f-9818-43d937060592', 'O-', 
    'Ninguna conocida', 'Insuficiencia cardíaca leve, Artrosis', 
    'Contactar con hija Ana: 600333444.'
),
(
    gen_random_uuid(), 'f04085f1-395d-4f10-9f5e-441639f7831d', 'B+', 
    'Sulfamidas', 'Osteoporosis, Hipotiroidismo', 
    'Contactar con médico Dr. Ruiz: Hospital Virgen del Rocío.'
);

-- ============================================================
-- SMARTWATCHES
-- ============================================================
-- Watch Maria:  b1689531-158a-4d74-9721-a75908906560
-- Watch José:   008064d4-061a-4f51-93c6-626a57890a50
-- Watch Carmen: d476356c-486a-4933-9189-9833630f9d9a

INSERT INTO smartwatch (device_id, patient_id, battery_level, connection_status, last_ping) VALUES
('b1689531-158a-4d74-9721-a75908906560', '7928e23f-e89a-4f51-87a3-535316335f60', 78, true, NOW() - INTERVAL '2 min'),
('008064d4-061a-4f51-93c6-626a57890a50', '36979607-4e78-436f-9818-43d937060592', 45, true, NOW() - INTERVAL '5 min'),
('d476356c-486a-4933-9189-9833630f9d9a', 'f04085f1-395d-4f10-9f5e-441639f7831d', 12, false, NOW() - INTERVAL '2 hours');

-- ============================================================
-- ASIGNACIONES CUIDADOR ↔ PACIENTE
-- ============================================================

INSERT INTO caregiver_patient (caregiver_id, patient_id) VALUES
('536e686c-7f3a-4469-80e9-383750059361', '7928e23f-e89a-4f51-87a3-535316335f60'), -- Carlos -> María
('536e686c-7f3a-4469-80e9-383750059361', '36979607-4e78-436f-9818-43d937060592'), -- Carlos -> José
('9f7e7747-0744-4860-9975-d5e855c4d516', '36979607-4e78-436f-9818-43d937060592'), -- Ana -> José
('9f7e7747-0744-4860-9975-d5e855c4d516', 'f04085f1-395d-4f10-9f5e-441639f7831d'); -- Ana -> Carmen

-- ============================================================
-- REGISTROS BIOMÉTRICOS
-- ============================================================

INSERT INTO biometric_record (record_id, device_id, heart_rate, spo2, steps, temperature, timestamp) VALUES
(gen_random_uuid(), 'b1689531-158a-4d74-9721-a75908906560', 72.0, 98.0, 1200, 36.5, NOW() - INTERVAL '6 hours'),
(gen_random_uuid(), 'b1689531-158a-4d74-9721-a75908906560', 148.0, 96.0, 4500, 36.8, NOW() - INTERVAL '2 hours'),
(gen_random_uuid(), '008064d4-061a-4f51-93c6-626a57890a50', 42.0, 91.0, 1900, 36.1, NOW() - INTERVAL '2 hours'),
(gen_random_uuid(), 'd476356c-486a-4933-9189-9833630f9d9a', 80.0, 97.0, 500, 36.6, NOW() - INTERVAL '3 hours');

-- ============================================================
-- UBICACIONES GPS
-- ============================================================

INSERT INTO gps_location (location_id, device_id, coordinates, timestamp) VALUES
(gen_random_uuid(), 'b1689531-158a-4d74-9721-a75908906560', ST_SetSRID(ST_MakePoint(-5.9845, 37.3891), 4326), NOW() - INTERVAL '30 min'),
(gen_random_uuid(), '008064d4-061a-4f51-93c6-626a57890a50', ST_SetSRID(ST_MakePoint(-5.9950, 37.3800), 4326), NOW() - INTERVAL '1 hour');

-- ============================================================
-- ALERTAS
-- ============================================================

INSERT INTO alert (alert_id, device_id, timestamp, alert_type, status, latitude, longitude, heart_rate, battery_level) VALUES
(gen_random_uuid(), 'b1689531-158a-4d74-9721-a75908906560', NOW() - INTERVAL '2 hours', 'HEART_RATE_HIGH', 'ACTIVE', 37.3891, -5.9845, 148.0, NULL),
(gen_random_uuid(), '008064d4-061a-4f51-93c6-626a57890a50', NOW() - INTERVAL '2 hours', 'HEART_RATE_LOW', 'ACTIVE', 37.3800, -5.9950, 42.0, NULL),
(gen_random_uuid(), 'd476356c-486a-4933-9189-9833630f9d9a', NOW() - INTERVAL '2 hours', 'BATTERY_LOW', 'ACTIVE', NULL, NULL, NULL, 12);

-- ============================================================
-- RECORDATORIOS
-- ============================================================

INSERT INTO reminder (reminder_id, patient_id, caregiver_id, message, scheduled_date, status) VALUES
(gen_random_uuid(), '7928e23f-e89a-4f51-87a3-535316335f60', '536e686c-7f3a-4469-80e9-383750059361', 'Pastilla tensión', NOW() + INTERVAL '1 hour', 'PENDING'),
(gen_random_uuid(), '36979607-4e78-436f-9818-43d937060592', '536e686c-7f3a-4469-80e9-383750059361', 'Bisoprolol corazón', NOW() + INTERVAL '30 min', 'PENDING'),
(gen_random_uuid(), 'f04085f1-395d-4f10-9f5e-441639f7831d', '9f7e7747-0744-4860-9975-d5e855c4d516', 'Levotiroxina ayunas', NOW() - INTERVAL '3 hours', 'MISSED');

-- ============================================================
-- ZONAS SEGURAS
-- ============================================================

INSERT INTO safe_zone (zone_id, patient_id, caregiver_id, centroid, radius_meters) VALUES
(gen_random_uuid(), '7928e23f-e89a-4f51-87a3-535316335f60', '536e686c-7f3a-4469-80e9-383750059361', ST_SetSRID(ST_MakePoint(-5.9845, 37.3891), 4326), 200),
(gen_random_uuid(), '36979607-4e78-436f-9818-43d937060592', '536e686c-7f3a-4469-80e9-383750059361', ST_SetSRID(ST_MakePoint(-5.9950, 37.3800), 4326), 300);