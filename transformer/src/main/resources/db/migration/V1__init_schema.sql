-- Roles
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE
);

INSERT INTO roles (name) VALUES ('ADMIN'), ('ENGINEER'), ('VIEWER');

-- Users (mirrors Supabase auth.users; id = Supabase auth UUID)
CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(150),
    role_id BIGINT NOT NULL REFERENCES roles(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Transformers
CREATE TABLE transformers (
    id BIGSERIAL PRIMARY KEY,
    asset_tag VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    location VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    capacity_kva NUMERIC(12,2),
    installation_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'HEALTHY',
    health_score DOUBLE PRECISION,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Devices
CREATE TABLE devices (
    id BIGSERIAL PRIMARY KEY,
    device_uid VARCHAR(100) NOT NULL UNIQUE,
    transformer_id BIGINT NOT NULL REFERENCES transformers(id) ON DELETE CASCADE,
    firmware_version VARCHAR(30),
    status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE',
    last_seen_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Sensor readings
CREATE TABLE sensor_readings (
    id BIGSERIAL PRIMARY KEY,
    transformer_id BIGINT NOT NULL REFERENCES transformers(id) ON DELETE CASCADE,
    device_id BIGINT REFERENCES devices(id) ON DELETE SET NULL,
    temperature_celsius DOUBLE PRECISION,
    oil_level_percent DOUBLE PRECISION,
    vibration_mm DOUBLE PRECISION,
    load_current_amps DOUBLE PRECISION,
    voltage_volts DOUBLE PRECISION,
    humidity_percent DOUBLE PRECISION,
    anomaly_score DOUBLE PRECISION,
    recorded_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_reading_transformer_time ON sensor_readings (transformer_id, recorded_at);

-- Alerts
CREATE TABLE alerts (
    id BIGSERIAL PRIMARY KEY,
    transformer_id BIGINT NOT NULL REFERENCES transformers(id) ON DELETE CASCADE,
    severity VARCHAR(20) NOT NULL,
    source VARCHAR(30) NOT NULL,
    message VARCHAR(500) NOT NULL,
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_alerts_open ON alerts (acknowledged, created_at);

-- Maintenance records
CREATE TABLE maintenance_records (
    id BIGSERIAL PRIMARY KEY,
    transformer_id BIGINT NOT NULL REFERENCES transformers(id) ON DELETE CASCADE,
    description VARCHAR(1000) NOT NULL,
    performed_by VARCHAR(150),
    performed_at TIMESTAMPTZ NOT NULL,
    next_due_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
