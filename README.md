# LocaVite 🚗

![Statut](https://img.shields.io/badge/statut-Sprint%202%20en%20cours-blue)
![Projet](https://img.shields.io/badge/projet-académique-blue)
![Licence](https://img.shields.io/badge/licence-MIT-green)
[![Backend CI](https://github.com/Amine-Genin/LocaVite/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/Amine-Genin/LocaVite/actions/workflows/backend-ci.yml)
[![Frontend CI](https://github.com/Amine-Genin/LocaVite/actions/workflows/frontend-ci.yml/badge.svg)](https://github.com/Amine-Genin/LocaVite/actions/workflows/frontend-ci.yml)

### Qualité du code Backend (SonarCloud)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Amine-Genin_LocaVite-backend&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Amine-Genin_LocaVite-backend)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=Amine-Genin_LocaVite-backend&metric=coverage)](https://sonarcloud.io/summary/new_code?id=Amine-Genin_LocaVite-backend)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=Amine-Genin_LocaVite-backend&metric=bugs)](https://sonarcloud.io/summary/new_code?id=Amine-Genin_LocaVite-backend)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=Amine-Genin_LocaVite-backend&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=Amine-Genin_LocaVite-backend)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=Amine-Genin_LocaVite-backend&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=Amine-Genin_LocaVite-backend)

### Qualité du code Frontend (SonarCloud)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Amine-Genin_LocaVite-frontend&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Amine-Genin_LocaVite-frontend)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=Amine-Genin_LocaVite-frontend&metric=coverage)](https://sonarcloud.io/summary/new_code?id=Amine-Genin_LocaVite-frontend)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=Amine-Genin_LocaVite-frontend&metric=bugs)](https://sonarcloud.io/summary/new_code?id=Amine-Genin_LocaVite-frontend)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=Amine-Genin_LocaVite-frontend&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=Amine-Genin_LocaVite-frontend)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=Amine-Genin_LocaVite-frontend&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=Amine-Genin_LocaVite-frontend)

## 📋 Description

LocaVite est une plateforme web dédiée à la **location et à la vente de voitures au Maroc**. Elle met en relation les clients et les gestionnaires de véhicules au travers d'une expérience moderne, sécurisée et adaptée au marché marocain.

La plateforme prévoit notamment la consultation d'un catalogue, la réservation de véhicules, le paiement en ligne, la gestion des ventes, les notifications et un espace d'administration.

## 👥 Équipe

| Membre | Rôle | Responsabilités principales |
|---|---|---|
| À compléter | Scrum Master / Backend | Organisation agile, architecture et développement backend |
| À compléter | Développeur Backend | Modules métier et API |
| À compléter | Développeur Backend | Modules métier, données et intégrations |
| À compléter | Développeur Frontend | Interface Angular et expérience utilisateur |
| À compléter | DevOps | CI/CD, conteneurisation, déploiement et supervision |

## 🏗️ Architecture des dossiers

```text
LocaVite/
├── backend/       # Application Spring Boot modulaire
├── frontend/      # Application Angular
├── infra/         # Docker, Kubernetes, Terraform, Ansible et monitoring
└── docs/          # Documentation, ADR, diagrammes et maquettes
```

## 🧰 Stack technique

| Domaine | Technologies |
|---|---|
| Backend | Java, Spring Boot 3, Maven |
| Frontend | Angular 18, TypeScript |
| Base de données | PostgreSQL |
| Cache | Redis |
| Stockage objet | MinIO |
| Conteneurisation | Docker |
| Orchestration | Kubernetes |
| CI/CD | GitHub Actions |
| Qualité de code | SonarCloud / SonarQube |
| Gestion de projet | Jira |

## 🔍 Qualité du code (SonarQube & SonarCloud)

L'analyse de la qualité du code est intégrée aux pipelines CI/CD de GitHub Actions pour chaque Pull Request et push sur la branche principale :

- **Plateforme SaaS recommandée** : [SonarCloud](https://sonarcloud.io/) (gratuit pour les dépôts publics)
- **Token CI** : secret GitHub `SONAR_TOKEN`
- **Politique de Quality Gate** :
  - **0 nouveau bug**
  - **0 nouvelle vulnérabilité**
  - **Couverture de tests sur le nouveau code > 70%**
- **Blocage de la PR** : Grâce à `sonar.qualitygate.wait=true`, si le Quality Gate échoue, le job CI échoue et bloque la fusion de la PR sur GitHub.
- **Alternative locale auto-hébergée (Docker)** :
  ```bash
  docker compose -f infra/docker/docker-compose.sonar.yml up -d
  ```
  Console accessible sur [http://localhost:9000](http://localhost:9000) (admin / admin).

## 📌 État actuel (Sprint 2)

Le projet est **en cours de Sprint 2** (gestion des véhicules et catalogue).

| Sprint | Statut | Contenu livré |
|---|---|---|
| Sprint 0 | Fait | Dépôt, architecture, CI GitHub Actions, SonarCloud, Docker Compose |
| Sprint 1 | Fait | Inscription, connexion JWT, profil, rôles `CLIENT` / `AGENCE` / `ADMIN` |
| Sprint 2 | **En cours** | Liste véhicules, fiche détaillée, calendrier de disponibilité, **formulaire d’ajout admin** |
| Sprint 3 | À venir | Parcours de réservation (aujourd’hui : placeholder `/reservation/:id`) |

Routes frontend utiles :

| URL | Accès |
|---|---|
| http://localhost:4200/login | Public |
| http://localhost:4200/vehicles | Catalogue public |
| http://localhost:4200/vehicles/:id | Fiche véhicule |
| http://localhost:4200/admin/vehicles | Flotte (rôles `AGENCE` ou `ADMIN`) |
| http://localhost:4200/admin/vehicles/new | Formulaire d’ajout (rôles `AGENCE` ou `ADMIN`) |

## 🚀 Démarrage rapide

### Prérequis

- Git
- Java 21
- Maven 3.9+
- Node.js 20+ et npm
- Docker et Docker Compose

### Installation

```bash
git clone https://github.com/Amine-Genin/LocaVite.git
cd LocaVite
```

### 1. Infrastructure (PostgreSQL, Redis, MinIO, MailHog)

```bash
docker compose -f infra/docker/docker-compose.yml up -d
docker compose -f infra/docker/docker-compose.yml ps
```

PostgreSQL est exposé sur le port **5433** (aligné avec `backend/src/main/resources/application.yml`).

### 2. Backend (port 8081)

Si `mvn spring-boot:run` échoue sur Windows (chemin avec accents, `ClassNotFoundException`), lancer le JAR :

```bash
cd backend
mvn -DskipTests package
java -jar target/locavite-backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=dev
```

Le profil `dev` crée le compte admin local :

- **Email** : `admin.dev@locavite.ma`
- **Mot de passe** : `AdminDev2026!`

Swagger : http://localhost:8081/swagger-ui.html

### 3. Frontend (port 4200)

```bash
cd frontend
npm ci
npm start
```

L’app Angular proxy `/api` vers `http://localhost:8081`.

## 🧪 Tests

### Frontend (PowerShell, depuis `frontend/`)

```powershell
npm run lint
npm run test
npm run build
```

E2E Cypress (le front doit tourner sur le port 4200) :

```powershell
npx cypress run --spec cypress/e2e/vehicle-create.cy.js
npx cypress run --spec cypress/e2e/vehicle-detail.cy.js
# ou interactif :
npx cypress open
```

### Backend (depuis `backend/`)

```powershell
mvn -q test
```

### Checklist manuelle — formulaire ajout véhicule (DGL-47)

1. Démarrer Docker + backend (`dev`) + frontend.
2. Ouvrir http://localhost:4200/admin/vehicles/new **sans** être connecté → redirection `/login`.
3. Se connecter en `CLIENT` (inscription) puis ouvrir `/admin/vehicles/new` → redirection `/login`.
4. Se connecter avec `admin.dev@locavite.ma` / `AdminDev2026!`.
5. Aller sur http://localhost:4200/admin/vehicles puis **Ajouter un véhicule**.
6. Vérifier les 5 sections : Identité, Caractéristiques, Prix, Localisation, Photos.
7. Laisser le formulaire incomplet → **Publier** et **Brouillon** restent désactivés.
8. Remplir marque (autocomplete), modèle, année, catégorie, carburant, transmission, places (2–9), matricule, description, prix/jour, prix de vente, ville.
9. Glisser 2–3 photos (JPG/PNG/WEBP, max 5 Mo), choisir la photo principale (étoile), réordonner, supprimer une photo.
10. **Publier le véhicule** → notification de succès + redirection `/admin/vehicles`.
11. Recommencer et **Enregistrer en brouillon**.
12. Contrôler le véhicule créé dans le catalogue public `/vehicles` et la fiche `/vehicles/:id`.

### Checklist manuelle — fiche véhicule (DGL-46)

1. http://localhost:4200/vehicles → cliquer une carte.
2. Galerie, caractéristiques, prix, calendrier, Réserver (`/reservation/:id`), Contacter l’agence, favori, véhicules similaires.
3. URL inconnue `/vehicles/999999` → état introuvable.

## 🗺️ Roadmap

- [x] **Sprint 0** — Initialisation du dépôt, cadrage, architecture et outillage
- [x] **Sprint 1** — Authentification, utilisateurs et rôles
- [ ] **Sprint 2** — Gestion des véhicules et catalogue *(en cours)*
- [ ] **Sprint 3** — Réservations et disponibilités
- [ ] **Sprint 4** — Paiement et facturation
- [ ] **Sprint 5** — Vente de véhicules
- [ ] **Sprint 6** — Notifications et administration
- [ ] **Sprint 7** — Industrialisation DevOps, observabilité et sécurité
- [ ] **Sprint 8** — Tests finaux, documentation et livraison

## 📄 Licence

LocaVite est un projet académique distribué sous licence MIT. Voir le fichier [LICENSE](LICENSE).
