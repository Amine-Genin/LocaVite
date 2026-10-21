# LocaVite 🚗

![Statut](https://img.shields.io/badge/statut-en%20développement-orange)
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

## 🚀 Démarrage rapide

### Prérequis

- Git
- Java 21 ou version compatible avec Spring Boot 3
- Maven 3.9+
- Node.js 20+ et npm
- Angular CLI 18
- Docker et Docker Compose
- Un accès à PostgreSQL, Redis et MinIO (local ou conteneurisé)

### Installation

```bash
git clone https://github.com/Amine-Genin/LocaVite.git
cd LocaVite
```

Les commandes de lancement du backend, du frontend et de l'infrastructure seront ajoutées au fur et à mesure de l'implémentation des modules.

## 🗺️ Roadmap

- [ ] **Sprint 0** — Initialisation du dépôt, cadrage, architecture et outillage
- [ ] **Sprint 1** — Authentification, utilisateurs et rôles
- [ ] **Sprint 2** — Gestion des véhicules et catalogue
- [ ] **Sprint 3** — Réservations et disponibilités
- [ ] **Sprint 4** — Paiement et facturation
- [ ] **Sprint 5** — Vente de véhicules
- [ ] **Sprint 6** — Notifications et administration
- [ ] **Sprint 7** — Industrialisation DevOps, observabilité et sécurité
- [ ] **Sprint 8** — Tests finaux, documentation et livraison

## 📄 Licence

LocaVite est un projet académique distribué sous licence MIT. Voir le fichier [LICENSE](LICENSE).
