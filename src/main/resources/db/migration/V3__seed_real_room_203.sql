UPDATE rooms
SET label = '203호 병실',
    camera_id = 'CAM-06',
    gender = '여자',
    capacity = 2
WHERE room_id = '203호';

INSERT INTO rooms (id, room_id, label, camera_id, gender, capacity)
SELECT 'room-203', '203호', '203호 병실', 'CAM-06', '여자', 2
WHERE NOT EXISTS (
    SELECT 1 FROM rooms WHERE room_id = '203호'
);

UPDATE bed_configs
SET camera_id = 'CAM-06',
    room_id = '203호',
    location = '203호 병실',
    patient_name = '203호 환자1',
    patient_no = '24-2031',
    active = TRUE
WHERE bed_id = 'B-206';

INSERT INTO bed_configs (
    id, bed_id, camera_id, location, patient_id, active, created_at,
    room_id, patient_name, patient_no
)
SELECT
    'bed-config-206', 'B-206', 'CAM-06', '203호 병실', NULL, TRUE, CURRENT_TIMESTAMP,
    '203호', '203호 환자1', '24-2031'
WHERE NOT EXISTS (
    SELECT 1 FROM bed_configs WHERE bed_id = 'B-206'
);

UPDATE bed_configs
SET camera_id = 'CAM-06',
    room_id = '203호',
    location = '203호 병실',
    patient_name = '203호 환자2',
    patient_no = '24-2032',
    active = TRUE
WHERE bed_id = 'B-207';

INSERT INTO bed_configs (
    id, bed_id, camera_id, location, patient_id, active, created_at,
    room_id, patient_name, patient_no
)
SELECT
    'bed-config-207', 'B-207', 'CAM-06', '203호 병실', NULL, TRUE, CURRENT_TIMESTAMP,
    '203호', '203호 환자2', '24-2032'
WHERE NOT EXISTS (
    SELECT 1 FROM bed_configs WHERE bed_id = 'B-207'
);

UPDATE patients
SET name = '203호 환자1',
    age = CASE WHEN age = 0 THEN 70 ELSE age END,
    gender = 'FEMALE',
    bed_id = 'B-206',
    active = TRUE
WHERE patient_number = '24-2031';

INSERT INTO patients (
    id, patient_number, name, age, gender, bed_id, diagnosis, notes, active, created_at
)
SELECT
    'patient-2031', '24-2031', '203호 환자1', 70, 'FEMALE', 'B-206', NULL, '203호 실제 카메라 연동 대상', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM patients WHERE patient_number = '24-2031'
);

UPDATE patients
SET name = '203호 환자2',
    age = CASE WHEN age = 0 THEN 70 ELSE age END,
    gender = 'FEMALE',
    bed_id = 'B-207',
    active = TRUE
WHERE patient_number = '24-2032';

INSERT INTO patients (
    id, patient_number, name, age, gender, bed_id, diagnosis, notes, active, created_at
)
SELECT
    'patient-2032', '24-2032', '203호 환자2', 70, 'FEMALE', 'B-207', NULL, '203호 실제 카메라 연동 대상', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM patients WHERE patient_number = '24-2032'
);
