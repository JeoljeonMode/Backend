CREATE TABLE rooms (
    id VARCHAR(255) NOT NULL,
    room_id VARCHAR(255),
    label VARCHAR(255),
    camera_id VARCHAR(255),
    gender VARCHAR(255),
    capacity INTEGER NOT NULL,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_rooms_room_id ON rooms (room_id);
CREATE UNIQUE INDEX uk_rooms_camera_id ON rooms (camera_id);

ALTER TABLE bed_configs ADD COLUMN room_id VARCHAR(255);
ALTER TABLE bed_configs ADD COLUMN patient_name VARCHAR(255);
ALTER TABLE bed_configs ADD COLUMN patient_no VARCHAR(255);
