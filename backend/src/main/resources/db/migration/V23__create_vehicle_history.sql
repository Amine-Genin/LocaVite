CREATE TABLE vehicle_history (
    id              BIGSERIAL PRIMARY KEY,
    sale_listing_id BIGINT       NOT NULL,
    event_type      VARCHAR(40)  NOT NULL,
    event_date      DATE         NOT NULL,
    description     TEXT         NOT NULL,
    kilometrage     INTEGER      NOT NULL CHECK (kilometrage >= 0),
    document_url    VARCHAR(512),
    verified        BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_vehicle_history_sale_listing
        FOREIGN KEY (sale_listing_id) REFERENCES sale_listings(id) ON DELETE CASCADE
);

CREATE INDEX idx_vehicle_history_listing_date
    ON vehicle_history (sale_listing_id, event_date, id);
