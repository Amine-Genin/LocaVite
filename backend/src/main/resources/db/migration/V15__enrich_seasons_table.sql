-- Migration V15 : Enrichissement de la table seasons pour les tarifs saisonniers et overrides agence/véhicule

ALTER TABLE seasons ADD COLUMN IF NOT EXISTS nom VARCHAR(100);
ALTER TABLE seasons ADD COLUMN IF NOT EXISTS priorite INTEGER DEFAULT 1;
ALTER TABLE seasons ADD COLUMN IF NOT EXISTS agence_id BIGINT;
ALTER TABLE seasons ADD COLUMN IF NOT EXISTS vehicle_id BIGINT;
ALTER TABLE seasons ADD COLUMN IF NOT EXISTS is_recurring BOOLEAN DEFAULT FALSE;

-- Synchroniser nom avec name pour les lignes déjà existantes
UPDATE seasons SET nom = name WHERE nom IS NULL;

-- Saisons pré-définies globales (agence_id IS NULL)
-- Basse saison : Octobre à Mars (coeff 1.00, priorité 1)
INSERT INTO seasons (name, nom, date_debut, date_fin, coefficient, priorite, is_recurring)
VALUES ('BASSE_SAISON_HIVER', 'Basse saison', DATE '2026-10-01', DATE '2027-03-31', 1.00, 1, true);

-- Moyenne saison : Avril à Juin (coeff 1.20, priorité 2)
INSERT INTO seasons (name, nom, date_debut, date_fin, coefficient, priorite, is_recurring)
VALUES ('MOYENNE_SAISON_PRINTEMPS', 'Moyenne saison printemps', DATE '2026-04-01', DATE '2026-06-30', 1.20, 2, true);

-- Moyenne saison : Septembre (coeff 1.20, priorité 2)
INSERT INTO seasons (name, nom, date_debut, date_fin, coefficient, priorite, is_recurring)
VALUES ('MOYENNE_SAISON_AUTOMNE', 'Moyenne saison septembre', DATE '2026-09-01', DATE '2026-09-30', 1.20, 2, true);

-- Haute saison : Juillet et Août (été) (coeff 1.50, priorité 3)
INSERT INTO seasons (name, nom, date_debut, date_fin, coefficient, priorite, is_recurring)
VALUES ('HAUTE_SAISON_ETE', 'Haute saison été', DATE '2026-07-01', DATE '2026-08-31', 1.50, 3, true);

-- Très haute saison : Fin d'année (coeff 2.00, priorité 4)
INSERT INTO seasons (name, nom, date_debut, date_fin, coefficient, priorite, is_recurring)
VALUES ('TRES_HAUTE_FIN_ANNEE', 'Très haute saison fin d''année', DATE '2026-12-20', DATE '2027-01-05', 2.00, 4, true);

-- Très haute saison : Aïd el-Fitr (coeff 2.00, priorité 4)
INSERT INTO seasons (name, nom, date_debut, date_fin, coefficient, priorite, is_recurring)
VALUES ('TRES_HAUTE_AID_FITR', 'Très haute saison Aïd el-Fitr', DATE '2026-03-18', DATE '2026-03-25', 2.00, 4, false);

-- Très haute saison : Aïd el-Kébir (coeff 2.00, priorité 4)
INSERT INTO seasons (name, nom, date_debut, date_fin, coefficient, priorite, is_recurring)
VALUES ('TRES_HAUTE_AID_KEBIR', 'Très haute saison Aïd el-Kébir', DATE '2026-05-25', DATE '2026-06-05', 2.00, 4, false);

CREATE INDEX IF NOT EXISTS idx_seasons_agence_id ON seasons (agence_id);
CREATE INDEX IF NOT EXISTS idx_seasons_vehicle_id ON seasons (vehicle_id);
CREATE INDEX IF NOT EXISTS idx_seasons_priorite ON seasons (priorite DESC);
