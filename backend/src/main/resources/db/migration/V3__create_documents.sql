-- Migration V3 : Documents d'identité (permis, CIN, passeport)

CREATE TABLE IF NOT EXISTS documents (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    type        VARCHAR(20)  NOT NULL,
    path        VARCHAR(512) NOT NULL UNIQUE,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    uploaded_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verified_at TIMESTAMP,
    verified_by BIGINT,
    CONSTRAINT fk_documents_user        FOREIGN KEY (user_id)     REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_documents_verified_by FOREIGN KEY (verified_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_documents_type   CHECK (type IN ('PERMIS', 'CIN', 'PASSEPORT')),
    CONSTRAINT chk_documents_status CHECK (status IN ('PENDING', 'VALIDATED', 'REJECTED'))
);

CREATE INDEX IF NOT EXISTS idx_documents_user_id ON documents (user_id);