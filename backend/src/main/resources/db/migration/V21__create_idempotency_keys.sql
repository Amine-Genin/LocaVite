-- Migration V21 : Création de la table idempotency_keys pour garantir l'idempotence des paiements

CREATE TABLE IF NOT EXISTS idempotency_keys (
    "key"        VARCHAR(255) PRIMARY KEY,
    response     TEXT         NOT NULL,
    status_code  INTEGER      NOT NULL DEFAULT 200,
    content_type VARCHAR(100) DEFAULT 'application/json',
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_idempotency_keys_created_at ON idempotency_keys (created_at DESC);
