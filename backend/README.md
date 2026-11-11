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

## Contrats de location PDF

La confirmation d'une réservation génère son contrat PDF/A en français, le stocke dans le bucket MinIO `locavite-contracts` et déclenche son envoi par courriel au client. Les versions arabe et anglaise sont générées à leur première demande. Le bucket est créé au démarrage lorsque `MINIO_INIT_BUCKET=true`; son nom peut être remplacé avec `MINIO_CONTRACTS_BUCKET`.

Le client authentifié propriétaire de la réservation peut télécharger son contrat avec `GET /api/reservations/{id}/contract`. Le paramètre `lang` accepte `FR` (défaut), `AR` ou `EN`. L'API répond par une redirection vers une URL MinIO pré-signée valable 24 heures. Les autres clients reçoivent `403`; une réservation non confirmée ne possède pas encore de contrat téléchargeable.

```sh
curl -L -H "Authorization: Bearer <JWT>" \
  "http://localhost:8081/api/reservations/123/contract?lang=FR" \
  --output contrat.pdf
```

Le contrat contient le client et le statut de ses pièces d'identité, le véhicule, les dates et lieux, le détail du prix, un QR code, ainsi que les zones de signature. Les numéros de CIN et de permis ne sont pas disponibles dans le modèle actuel et ne sont donc pas imprimés. Les conditions incluses sont un modèle provisoire qui doit être validé juridiquement avant utilisation réelle. Configurez `LOCA_VITE_PUBLIC_BASE_URL` pour que le QR code pointe vers l'URL publique de l'API; `CONTRACT_FONT_PATH` permet de préciser le chemin d'une police Unicode si aucune police prise en charge n'est installée. L'e-mail requiert un serveur SMTP, configurable avec `MAIL_HOST` et `MAIL_PORT`.

## Gestion des cautions

Chaque réservation possède un montant de caution et un statut initial `PENDING`. Une agence (propriétaire du véhicule de la réservation) ou un administrateur peut gérer les mouvements avec `PUT /api/admin/reservations/{id}/caution`, en envoyant `type` (`HOLD`, `RELEASE` ou `CAPTURE`), `montant` et une `note` facultative. Le blocage porte sur le montant total configuré; une libération peut être partielle ou totale. Avant une capture de dommages, la part non retenue doit être libérée : le montant capturé correspond donc au solde restant. Chaque mouvement conserve son statut résultant dans le journal.

L'historique est disponible avec `GET /api/admin/clients/{clientId}/cautions` (limité aux véhicules de l'agence connectée) et le reçu PDF avec `GET /api/admin/reservations/{id}/caution/receipt`.

## Prolongation d'une réservation

Un client authentifié peut prolonger sa réservation en cours (`CONFIRMED` ou `ACTIVE`) via `POST /api/reservations/{id}/extend` en fournissant `{ "newDateFin": "YYYY-MM-DD", "acceptNewPrice": true }`. L'API vérifie la disponibilité du véhicule sur la période supplémentaire, recalcule le prix global (options et remises incluses), enregistre une entrée d'historique `ReservationExtension`, génère une nouvelle version du contrat PDF et notifie l'agence via `ReservationExtendedEvent`. L'historique des prolongations est inclus dans la réservation et consultable via `GET /api/reservations/{id}/extensions`.

