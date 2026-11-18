-- ==============================================================================
-- Migration V22 : Création de la table des annonces de vente de voitures (SaleListing)
-- ==============================================================================

CREATE TABLE sale_listings (
    id                        BIGSERIAL PRIMARY KEY,
    code                      VARCHAR(30)    NOT NULL,
    vehicle_id                BIGINT         NOT NULL,
    agence_id                 BIGINT         NOT NULL,
    titre                     VARCHAR(255)   NOT NULL,
    description               TEXT           NOT NULL,
    prix_vente                NUMERIC(12, 2) NOT NULL CHECK (prix_vente > 0),
    prix_nego                 BOOLEAN        NOT NULL DEFAULT FALSE,
    etat                      VARCHAR(20)    NOT NULL,
    kilometrage               INTEGER        CHECK (kilometrage IS NULL OR kilometrage >= 0),
    premier_proprietaire      BOOLEAN        NOT NULL DEFAULT TRUE,
    date_mise_en_circulation  DATE,
    numero_chassis            VARCHAR(50),
    nombre_proprietaires      INTEGER        DEFAULT 1 CHECK (nombre_proprietaires IS NULL OR nombre_proprietaires >= 0),
    statut                    VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    deleted                   BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at                TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at              TIMESTAMP WITH TIME ZONE,
    sold_at                   TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_sale_listings_code UNIQUE (code),
    CONSTRAINT fk_sale_listings_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_sale_listings_vin ON sale_listings(numero_chassis);
CREATE INDEX IF NOT EXISTS idx_sale_listings_vehicle ON sale_listings(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_sale_listings_agence ON sale_listings(agence_id);
CREATE INDEX IF NOT EXISTS idx_sale_listings_statut ON sale_listings(statut);
CREATE INDEX IF NOT EXISTS idx_sale_listings_etat ON sale_listings(etat);
CREATE INDEX IF NOT EXISTS idx_sale_listings_deleted ON sale_listings(deleted);
