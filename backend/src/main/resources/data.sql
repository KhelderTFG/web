-- ============================================================
-- KHELDER — Datos iniciales
-- Solo usuarios y pacientes base — sin datos operacionales
-- ============================================================

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
TRUNCATE TABLE pairing_code     CASCADE;

-- ============================================================
-- CUIDADORES
-- Contraseña de todos: "password123"
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

INSERT INTO patient (patient_id, full_name, date_of_birth) VALUES
('7928e23f-e89a-4f51-87a3-535316335f60', 'María Antonia García', '1945-03-15'),
('36979607-4e78-436f-9818-43d937060592', 'José Manuel Rodríguez', '1938-07-22'),
('f04085f1-395d-4f10-9f5e-441639f7831d', 'Carmen Sánchez Pérez',  '1950-11-08');

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
-- ASIGNACIONES CUIDADOR ↔ PACIENTE
-- ============================================================

INSERT INTO caregiver_patient (caregiver_id, patient_id) VALUES
('536e686c-7f3a-4469-80e9-383750059361', '7928e23f-e89a-4f51-87a3-535316335f60'),
('536e686c-7f3a-4469-80e9-383750059361', '36979607-4e78-436f-9818-43d937060592'),
('9f7e7747-0744-4860-9975-d5e855c4d516', '36979607-4e78-436f-9818-43d937060592'),
('9f7e7747-0744-4860-9975-d5e855c4d516', 'f04085f1-395d-4f10-9f5e-441639f7831d');