CREATE TABLE contact_requests (
    id              BIGSERIAL PRIMARY KEY,
    sale_listing_id BIGINT       NOT NULL,
    nom             VARCHAR(200) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    telephone       VARCHAR(50)  NOT NULL,
    message         TEXT         NOT NULL,
    statut          VARCHAR(20)  NOT NULL DEFAULT 'NEW',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    replied_at      TIMESTAMP WITH TIME ZONE,
    reply_message   TEXT,
    CONSTRAINT fk_contact_requests_sale_listing
        FOREIGN KEY (sale_listing_id) REFERENCES sale_listings(id) ON DELETE CASCADE
);

CREATE INDEX idx_contact_requests_listing_created
    ON contact_requests (sale_listing_id, created_at DESC);
