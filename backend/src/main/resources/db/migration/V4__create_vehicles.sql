CREATE TABLE vehicles (
    id            BIGSERIAL PRIMARY KEY,
    marque        VARCHAR(100)   NOT NULL,
    modele        VARCHAR(100)   NOT NULL,
    annee         INTEGER        NOT NULL CHECK (annee >= 1990),
    categorie     VARCHAR(20)    NOT NULL,
    ville         VARCHAR(100)   NOT NULL,
    prix_jour     NUMERIC(12, 2) NOT NULL CHECK (prix_jour > 0),
    prix_vente    NUMERIC(12, 2) NOT NULL CHECK (prix_vente > 0),
    carburant     VARCHAR(20)    NOT NULL,
    transmission  VARCHAR(20)    NOT NULL,
    places        INTEGER        NOT NULL CHECK (places > 0),
    description   TEXT           NOT NULL,
    matricule     VARCHAR(30)    NOT NULL,
    statut        VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    agence_id     BIGINT         NOT NULL,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uk_vehicles_matricule UNIQUE (matricule)
    -- Ajoute la FK une fois le nom de ta table users confirmé :
    -- , CONSTRAINT fk_vehicles_agence FOREIGN KEY (agence_id) REFERENCES users (id)
);

CREATE INDEX idx_vehicles_statut    ON vehicles (statut);
CREATE INDEX idx_vehicles_agence    ON vehicles (agence_id);
CREATE INDEX idx_vehicles_ville     ON vehicles (lower(ville));
CREATE INDEX idx_vehicles_marque    ON vehicles (lower(marque));
CREATE INDEX idx_vehicles_categorie ON vehicles (categorie);