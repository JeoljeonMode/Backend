DELETE FROM monitoring_event_risk_factors
WHERE event_id IN (
    SELECT id FROM monitoring_events WHERE bed_id = 'B-207'
);

DELETE FROM monitoring_events
WHERE bed_id = 'B-207';

DELETE FROM patients
WHERE patient_number = '24-2032' OR bed_id = 'B-207';

DELETE FROM bed_configs
WHERE bed_id = 'B-207';

UPDATE rooms
SET capacity = 1
WHERE room_id = '203호';
