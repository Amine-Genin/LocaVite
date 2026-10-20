# ⚙️ Infrastructure LocaVite

Le dossier `infra/` regroupe l'ensemble des ressources nécessaires au développement, au déploiement et à l'exploitation de la plateforme **LocaVite**.

## 🏗️ Composants

| Dossier | Contenu |
|---|---|
| `docker/` | Configurations Docker Compose pour l'environnement de développement local |
| `kubernetes/` | Manifests, namespaces, services, ingress et déploiements |
| `terraform/` | Provisionnement déclaratif de l'infrastructure cloud |
| `ansible/` | Automatisation de la configuration des serveurs |
| `monitoring/` | Supervision, métriques, journaux et alertes (Prometheus / Grafana) |

---

## 🐳 Environnement local (Docker Compose)

Le fichier `infra/docker/docker-compose.yml` permet de démarrer l'ensemble des dépendances d'infrastructure nécessaires au backend en local.

### 📦 Services inclus

| Service | Image | Port hôte | Rôle | Healthcheck |
|---|---|---|---|---|
| **PostgreSQL** | `postgres:16-alpine` | `5432` | Base de données relationnelle | `pg_isready` |
| **Redis** | `redis:7-alpine` | `6379` | Cache et sessions applicatives | `redis-cli ping` |
| **MinIO** | `minio/minio` | `9000` (API) / `9001` (Console) | Stockage objet compatible S3 (documents, CIN, permis) | `mc ready local` |
| **MailHog** | `mailhog/mailhog:v1.0.1` | `1025` (SMTP) / `8025` (UI Web) | Serveur SMTP de test pour les e-mails | Port HTTP |

Tous les services communiquent au sein d'un réseau bridge dédié : `locavite-network`.

---

### 🚀 Démarrage rapide

Depuis la racine du projet :

```bash
# 1. Démarrer tous les services en arrière-plan
docker compose -f infra/docker/docker-compose.yml up -d

# 2. Vérifier l'état et les healthchecks des conteneurs
docker compose -f infra/docker/docker-compose.yml ps
```

---

### ⚙️ Variables d'environnement

Les variables sont paramétrables via le fichier `infra/docker/.env`. Un modèle prêt à l'emploi est fourni dans `infra/docker/.env.example` :

```bash
# Copier le fichier d'exemple
cp infra/docker/.env.example infra/docker/.env
```

| Variable | Valeur par défaut | Description |
|---|---|---|
| `POSTGRES_DB` | `locavite` | Nom de la base de données |
| `POSTGRES_USER` | `locavite` | Utilisateur PostgreSQL |
| `POSTGRES_PASSWORD` | `locavite_dev_password` | Mot de passe PostgreSQL |
| `POSTGRES_PORT` | `5432` | Port d'exposition PostgreSQL |
| `REDIS_PORT` | `6379` | Port d'exposition Redis |
| `MINIO_ROOT_USER` | `locavite` | Utilisateur administrateur MinIO |
| `MINIO_ROOT_PASSWORD` | `locavite_dev_password` | Mot de passe administrateur MinIO |
| `MINIO_PORT` | `9000` | Port API S3 MinIO |
| `MINIO_CONSOLE_PORT` | `9001` | Port de la Console Web MinIO |
| `MAILHOG_SMTP_PORT` | `1025` | Port SMTP de MailHog |
| `MAILHOG_UI_PORT` | `8025` | Port Web UI de MailHog |

---

### 🔌 Connexion aux services

#### 1. PostgreSQL (DBeaver, pgAdmin, IntelliJ, etc.)
* **Hôte** : `localhost`
* **Port** : `5432`
* **Base de données** : `locavite`
* **Utilisateur** : `locavite`
* **Mot de passe** : `locavite_dev_password`
* **URL JDBC** : `jdbc:postgresql://localhost:5432/locavite`

#### 2. Console MinIO
* **URL** : [http://localhost:9001](http://localhost:9001)
* **Access Key / User** : `locavite`
* **Secret Key / Password** : `locavite_dev_password`

#### 3. Interface MailHog
* **URL** : [http://localhost:8025](http://localhost:8025)

---

### 🛑 Arrêt et maintenance

```bash
# Arrêter les conteneurs (données conservées dans les volumes)
docker compose -f infra/docker/docker-compose.yml down

# Arrêter les conteneurs ET supprimer les volumes (réinitialisation complète)
docker compose -f infra/docker/docker-compose.yml down -v

# Consulter les logs d'un service spécifique
docker compose -f infra/docker/docker-compose.yml logs -f postgres
docker compose -f infra/docker/docker-compose.yml logs -f minio
```
