ALTER TABLE rooms ADD COLUMN camera_enabled BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE rooms SET camera_enabled = TRUE WHERE room_id = '203호';
