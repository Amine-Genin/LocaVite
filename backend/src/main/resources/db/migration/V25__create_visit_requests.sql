CREATE TABLE visit_requests (
    id              BIGSERIAL PRIMARY KEY,
    sale_listing_id BIGINT        NOT NULL,
    client_id       BIGINT        NOT NULL,
    date_souhaitee  DATE          NOT NULL,
    creneau_horaire VARCHAR(50)   NOT NULL,
    lieu            VARCHAR(255)  NOT NULL,
    type_visite     VARCHAR(30)   NOT NULL,
    statut          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    notes           TEXT,
    reminder_sent_at TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_visit_requests_sale_listing
        FOREIGN KEY (sale_listing_id) REFERENCES sale_listings(id) ON DELETE CASCADE,
    CONSTRAINT fk_visit_requests_client
        FOREIGN KEY (client_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_visit_requests_listing_created
    ON visit_requests (sale_listing_id, created_at DESC);
CREATE INDEX idx_visit_requests_status_date
    ON visit_requests (statut, date_souhaitee);
