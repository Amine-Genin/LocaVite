ALTER TABLE vehicles ADD COLUMN kilometrage INTEGER NOT NULL DEFAULT 0 CHECK (kilometrage >= 0);
ALTER TABLE vehicles ADD COLUMN derniere_maintenance DATE;
ALTER TABLE vehicles ADD COLUMN prochaine_maintenance DATE;

CREATE TABLE vehicle_status_history (
    id BIGSERIAL PRIMARY KEY,
    vehicle_id BIGINT NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    ancien_statut VARCHAR(20),
    nouveau_statut VARCHAR(20) NOT NULL,
    raison VARCHAR(500),
    date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT NOT NULL
);
CREATE INDEX idx_vehicle_status_history_vehicle_date ON vehicle_status_history(vehicle_id, date DESC);

CREATE TABLE vehicle_maintenance (
    id BIGSERIAL PRIMARY KEY,
    vehicle_id BIGINT NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    description VARCHAR(500) NOT NULL,
    kilometrage INTEGER NOT NULL CHECK (kilometrage >= 0),
    prochaine_maintenance DATE,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_vehicle_maintenance_vehicle_date ON vehicle_maintenance(vehicle_id, date DESC);

CREATE TABLE vehicle_incidents (
    id BIGSERIAL PRIMARY KEY,
    vehicle_id BIGINT NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    description VARCHAR(1000) NOT NULL,
    gravite VARCHAR(20) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_vehicle_incidents_vehicle_date ON vehicle_incidents(vehicle_id, date DESC);
