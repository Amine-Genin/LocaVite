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
