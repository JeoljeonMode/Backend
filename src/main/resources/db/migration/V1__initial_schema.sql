CREATE TABLE users (
    id VARCHAR(255) NOT NULL,
    username VARCHAR(255),
    password VARCHAR(255),
    role VARCHAR(255),
    display_name VARCHAR(255),
    active BIT NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE bed_configs (
    id VARCHAR(255) NOT NULL,
    bed_id VARCHAR(255),
    camera_id VARCHAR(255),
    location VARCHAR(255),
    patient_id VARCHAR(255),
    active BIT NOT NULL,
    created_at DATETIME(6),
    PRIMARY KEY (id)
);

CREATE TABLE patients (
    id VARCHAR(255) NOT NULL,
    patient_number VARCHAR(255),
    name VARCHAR(255),
    age INTEGER NOT NULL,
    gender VARCHAR(255),
    bed_id VARCHAR(255),
    diagnosis VARCHAR(255),
    notes VARCHAR(255),
    active BIT NOT NULL,
    created_at DATETIME(6),
    PRIMARY KEY (id)
);

CREATE TABLE monitoring_events (
    id VARCHAR(255) NOT NULL,
    occurred_at DATETIME(6),
    camera_id VARCHAR(255),
    bed_id VARCHAR(255),
    patient_name VARCHAR(255),
    patient_no VARCHAR(255),
    patient_position VARCHAR(255),
    posture VARCHAR(255),
    guardrail_up BIT NOT NULL,
    caregiver_present BIT NOT NULL,
    risk_score INTEGER NOT NULL,
    risk_level ENUM('NORMAL', 'CAUTION', 'DANGER'),
    summary VARCHAR(255),
    frame_url VARCHAR(255),
    roi_x DOUBLE PRECISION,
    roi_y DOUBLE PRECISION,
    roi_width DOUBLE PRECISION,
    roi_height DOUBLE PRECISION,
    patient_box_x DOUBLE PRECISION,
    patient_box_y DOUBLE PRECISION,
    patient_box_width DOUBLE PRECISION,
    patient_box_height DOUBLE PRECISION,
    acknowledged BIT NOT NULL,
    acknowledged_at DATETIME(6),
    PRIMARY KEY (id)
);

CREATE TABLE monitoring_event_risk_factors (
    event_id VARCHAR(255) NOT NULL,
    factor VARCHAR(255),
    factor_order INTEGER NOT NULL,
    PRIMARY KEY (event_id, factor_order),
    CONSTRAINT fk_monitoring_event_risk_factors_event
        FOREIGN KEY (event_id) REFERENCES monitoring_events (id)
);

CREATE INDEX idx_monitoring_events_occurred_at ON monitoring_events (occurred_at);
CREATE INDEX idx_monitoring_events_bed_risk_ack_occurred
    ON monitoring_events (bed_id, risk_level, acknowledged, occurred_at);
