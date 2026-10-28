CREATE TABLE vehicle_photos (
    id             BIGSERIAL PRIMARY KEY,
    vehicle_id     BIGINT       NOT NULL,
    path           VARCHAR(512) NOT NULL UNIQUE,
    thumbnail_path VARCHAR(512) NOT NULL UNIQUE,
    is_primary     BOOLEAN      NOT NULL DEFAULT FALSE,
    ordre          INTEGER      NOT NULL,
    uploaded_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vehicle_photos_vehicle FOREIGN KEY (vehicle_id)
        REFERENCES vehicles (id) ON DELETE CASCADE,
    CONSTRAINT uk_vehicle_photos_ordre UNIQUE (vehicle_id, ordre)
);

CREATE INDEX idx_vehicle_photos_vehicle_id ON vehicle_photos (vehicle_id);
