# 🧩 Backend

Le backend de LocaVite sera construit avec **Spring Boot 3** et organisé en modules métier au sein d'un monolithe modulaire. Chaque module conservera une responsabilité claire et exposera ses services via les API de l'application.

## 📦 Modules prévus

| Module | Responsabilité |
|---|---|
| `module-auth` | Authentification, utilisateurs, rôles et autorisations |
| `module-vehicules` | Référentiel des véhicules, caractéristiques, disponibilités et catalogue |
| `module-reservation` | Création, suivi, modification et annulation des réservations |
| `module-paiement` | Paiements, transactions, facturation et statuts |
| `module-vente` | Annonces, processus de vente et suivi des véhicules vendus |
| `module-notification` | Notifications utilisateur et communications métier |
| `module-common` | Objets, exceptions, utilitaires et composants partagés |
| `app-web` | Point d'entrée Spring Boot, configuration et assemblage de l'application |

Les modules partageront PostgreSQL pour les données métier, Redis pour le cache et MinIO pour le stockage des médias.

## Tarification des réservations

Le taux de TVA doit être défini en pourcentage via `PRICING_VAT_RATE` (par exemple `20`). Il n'y a pas de taux implicite : un devis ou une réservation échoue explicitement tant que cette variable n'est pas renseignée.

La migration `V8__create_seasons_options.sql` initialise les options GPS (50 MAD/jour), siège bébé (30 MAD/jour) et conducteur additionnel (100 MAD/jour), ainsi qu'une saison basse à 1,0. Les périodes haute et très haute saison se configurent dans la table `seasons`; `date_fin` est exclusive. En cas de chevauchement, le coefficient le plus élevé s'applique pour chaque jour. Les saisons sont mises en cache dans Redis pendant 24 heures.

La caution se règle sur chaque ligne de `categories` dans `caution` (2 000 à 5 000 MAD, 3 000 MAD par défaut). Le total du devis comprend la caution. La TVA s'applique au prix du véhicule après remise et aux options, hors caution; la remise de longue durée porte sur le prix du véhicule.

Le devis est disponible sans authentification via `POST /api/reservations/quote` avec `vehicleId`, `dateDebut`, `dateFin` et `options`. Ce calcul est également utilisé à la création et à la modification d'une réservation.
