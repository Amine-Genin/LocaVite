-- ==============================================================================
-- LocaVite E2E Test Database Seed Script
-- Initialise les utilisateurs, véhicules, documents et réservations de test E2E.
-- ==============================================================================

-- 1. Nettoyage des réservations et données de test
DELETE FROM reservation_options;
DELETE FROM reservation_contracts;
DELETE FROM caution_transactions;
DELETE FROM reservation_extensions;
DELETE FROM refunds;
DELETE FROM reservations;
DELETE FROM documents WHERE user_id IN (SELECT id FROM users WHERE email IN ('ahmed.benali@locavite.ma', 'client@locavite.ma'));

-- 2. Insertion / Vérification des rôles
INSERT INTO roles (name) VALUES ('CLIENT') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('ADMIN') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('AGENCE') ON CONFLICT (name) DO NOTHING;

-- 3. Insertion des utilisateurs de test
INSERT INTO users (id, nom, prenom, email, telephone, password)
VALUES 
  (1, 'Admin', 'Dev', 'admin.dev@locavite.ma', '+212600000000', '$2a$10$w6K0fVqB6E4qE1jBv4cXeO5B3R3KqV0VwQ9fUe8P9n3j3T8jJk1.W'),
  (2, 'Benali', 'Ahmed', 'ahmed.benali@locavite.ma', '0612345678', '$2a$10$w6K0fVqB6E4qE1jBv4cXeO5B3R3KqV0VwQ9fUe8P9n3j3T8jJk1.W'),
  (3, 'Agence', 'Casablanca', 'agence@locavite.ma', '+212600000014', '$2a$10$w6K0fVqB6E4qE1jBv4cXeO5B3R3KqV0VwQ9fUe8P9n3j3T8jJk1.W')
ON CONFLICT (email) DO UPDATE 
SET nom = EXCLUDED.nom, prenom = EXCLUDED.prenom, telephone = EXCLUDED.telephone;

-- Associer les rôles
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.email = 'admin.dev@locavite.ma' AND r.name IN ('ADMIN', 'AGENCE')
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.email = 'ahmed.benali@locavite.ma' AND r.name = 'CLIENT'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.email = 'agence@locavite.ma' AND r.name = 'AGENCE'
ON CONFLICT DO NOTHING;

-- 4. Documents validés pour Ahmed Benali
INSERT INTO documents (id, user_id, type, path, status, uploaded_at, verified_at)
VALUES
  (10, 2, 'PERMIS', 'permis_ahmed.pdf', 'VALIDATED', NOW(), NOW()),
  (11, 2, 'CIN', 'cin_ahmed.pdf', 'VALIDATED', NOW(), NOW())
ON CONFLICT (path) DO UPDATE SET status = 'VALIDATED';

-- 5. Marques et Catégories
INSERT INTO brands (id, name) VALUES (1, 'Renault'), (2, 'Dacia'), (3, 'Peugeot') ON CONFLICT (name) DO NOTHING;
INSERT INTO categories (id, name) VALUES (1, 'CITADINE'), (2, 'SUV'), (3, 'BERLINE') ON CONFLICT (name) DO NOTHING;

-- 6. Véhicules de test
INSERT INTO vehicles (id, marque, modele, annee, categorie, ville, prix_jour, prix_vente, carburant, transmission, places, description, matricule, statut, agence_id)
VALUES
  (1, 'Dacia', 'Duster', 2024, 'SUV', 'Casablanca', 350.00, 195000.00, 'DIESEL', 'MANUELLE', 5, 'SUV compact économique et polyvalent.', '78945-A-1', 'PUBLISHED', 3),
  (2, 'Renault', 'Clio 5', 2023, 'Citadine', 'Rabat', 280.00, 145000.00, 'ESSENCE', 'MANUELLE', 5, 'Citadine moderne très confortable.', '12345-B-2', 'PUBLISHED', 3)
ON CONFLICT (matricule) DO UPDATE 
SET statut = 'PUBLISHED', prix_jour = EXCLUDED.prix_jour;

-- 7. Réservations initiales
INSERT INTO reservations (id, code, user_id, vehicle_id, date_debut, date_fin, lieu_prise_en_charge, lieu_retour, prix_total, caution, statut, created_at, updated_at)
VALUES
  (101, 'LV-TEST01', 2, 1, CURRENT_DATE + INTERVAL '5 days', CURRENT_DATE + INTERVAL '10 days', 'Aéroport Casablanca', 'Aéroport Casablanca', 1750.00, 3000.00, 'PENDING', NOW(), NOW()),
  (102, 'LV-TEST02', 2, 2, CURRENT_DATE - INTERVAL '2 days', CURRENT_DATE + INTERVAL '3 days', 'Gare Rabat Ville', 'Gare Rabat Ville', 1400.00, 3000.00, 'ACTIVE', NOW(), NOW()),
  (103, 'LV-TEST03', 2, 1, CURRENT_DATE - INTERVAL '15 days', CURRENT_DATE - INTERVAL '10 days', 'Aéroport Casablanca', 'Aéroport Casablanca', 1750.00, 3000.00, 'COMPLETED', NOW(), NOW()),
  (104, 'LV-TEST04', 2, 2, CURRENT_DATE - INTERVAL '25 days', CURRENT_DATE - INTERVAL '22 days', 'Agence Casablanca', 'Agence Casablanca', 840.00, 3000.00, 'CANCELLED', NOW(), NOW()),
  (201, 'LV-ADM01', 2, 1, CURRENT_DATE + INTERVAL '7 days', CURRENT_DATE + INTERVAL '12 days', 'Aéroport Casablanca', 'Aéroport Casablanca', 1750.00, 3000.00, 'PENDING', NOW(), NOW()),
  (202, 'LV-ADM02', 2, 2, CURRENT_DATE - INTERVAL '1 day', CURRENT_DATE + INTERVAL '4 days', 'Gare Rabat Ville', 'Gare Rabat Ville', 1400.00, 3000.00, 'ACTIVE', NOW(), NOW())
ON CONFLICT (code) DO UPDATE
SET statut = EXCLUDED.statut, updated_at = NOW();

-- 8. Options de réservation de test
INSERT INTO reservation_options (reservation_id, option_name)
VALUES
  (101, 'GPS'),
  (201, 'GPS')
ON CONFLICT (reservation_id, option_name) DO NOTHING;
