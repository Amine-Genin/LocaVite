# ⚙️ Infrastructure LocaVite

Le dossier `infra/` regroupe l'ensemble des ressources nécessaires au développement, au déploiement et à l'exploitation de la plateforme **LocaVite**.

## 🏗️ Composants

| Dossier | Contenu |
|---|---|
| `docker/` | Configurations Docker Compose pour le développement local et le staging |
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
| **PostgreSQL** | `postgres:16-alpine` | `5433` (ou `5432`) | Base de données relationnelle | `pg_isready` |
| **Redis** | `redis:7-alpine` | `6379` | Cache et sessions applicatives | `redis-cli ping` |
| **MinIO** | `minio/minio` | `9000` (API) / `9001` (Console) | Stockage objet compatible S3 (documents, CIN, permis) | `mc ready local` |
| **MailHog** | `mailhog/mailhog:v1.0.1` | `1025` (SMTP) / `8025` (UI Web) | Serveur SMTP de test pour les e-mails | Port HTTP |

Tous les services communiquent au sein d'un réseau bridge dédié : `locavite-network`.

### 🚀 Démarrage rapide en local

Depuis la racine du projet :

```bash
# 1. Démarrer tous les services en arrière-plan
docker compose -f infra/docker/docker-compose.yml up -d

# 2. Vérifier l'état et les healthchecks des conteneurs
docker compose -f infra/docker/docker-compose.yml ps
```

---

## 🚀 Pipeline de Déploiement Continu Staging (CD)

Le workflow [`.github/workflows/deploy-staging.yml`](../.github/workflows/deploy-staging.yml) orchestre le déploiement automatique sur l'environnement de staging.

### 🔄 Schéma du Cycle de Déploiement

```text
[Push sur main / Merge PR]
           │
           ▼
[Build Docker Backend & Frontend]
  - Tag: staging-{sha} & staging-latest
  - Multi-stage builds optimisés
           │
           ▼
[Push vers GitHub Container Registry (ghcr.io)]
  - ghcr.io/amine-genin/locavite-backend:staging-{sha}
  - ghcr.io/amine-genin/locavite-frontend:staging-{sha}
           │
           ▼
[Copie Compose & Déploiement SSH sur Serveur Staging]
  - SCP infra/docker/docker-compose.staging.yml
  - Enregistrement candidate_tag.txt
  - docker pull & docker compose up -d
           │
           ▼
[Health Check Post-Déploiement]
  - Boucle de vérification curl http://localhost:8081/actuator/health
  - Vérification Frontend http://localhost:80/healthz
           │
     ┌─────┴─────┐
     ▼           ▼
[Succès]      [Échec du Health Check]
  - Activer tag   - ROLLBACK AUTOMATIQUE
  - Notify Slack    vers le tag précédent
                  - Rétablissement conteneurs
                  - Alerte Slack / Discord (Erreur)
```

### 🔐 Secrets GitHub Actions Requis

Pour activer le déploiement sur le serveur staging, configurez les secrets suivants dans votre dépôt GitHub (`Settings > Secrets and variables > Actions`) :

| Nom du Secret | Description | Exemple |
|---|---|---|
| `STAGING_HOST` | IP ou nom de domaine FQDN du serveur staging | `staging.locavite.ma` ou `198.51.100.42` |
| `STAGING_USER` | Utilisateur Linux disposant des droits Docker | `deploy` ou `ubuntu` |
| `STAGING_SSH_KEY` | Clé privée SSH (OpenSSH Ed25519 ou RSA) | `-----BEGIN OPENSSH PRIVATE KEY-----...` |
| `STAGING_PORT` | Port SSH du serveur distant (optionnel, défaut 22) | `22` |
| `SLACK_WEBHOOK` | Webhook Slack ou Discord pour les notifications | `https://hooks.slack.com/services/...` |
| `GITHUB_TOKEN` | Fourni automatiquement par GitHub Actions | Utilisé pour s'authentifier à `ghcr.io` |

---

### 📦 Déploiement manuel sur le serveur Staging

Si vous devez intervenir directement sur le serveur staging :

```bash
# 1. Se connecter au serveur
ssh deploy@staging.locavite.ma

# 2. Se positionner dans le répertoire applicatif
cd /opt/locavite/staging

# 3. Authentifier Docker auprès de GHCR
echo $CR_PAT | docker login ghcr.io -u <votre-github-user> --password-stdin

# 4. Télécharger les dernières images
docker pull ghcr.io/amine-genin/locavite-backend:staging-latest
docker pull ghcr.io/amine-genin/locavite-frontend:staging-latest

# 5. Démarrer ou mettre à jour la stack
docker compose -f docker-compose.staging.yml up -d

# 6. Vérifier la santé du backend et du frontend
curl -f http://localhost:8081/actuator/health
curl -f http://localhost:80/healthz
```

---

### 🛡️ Procédure de Rollback

Le rollback est **automatique** dans le pipeline CI/CD si le Health Check échoue après 12 tentatives (60 secondes).

Pour déclencher un rollback **manuellement** sur le serveur :

```bash
cd /opt/locavite/staging

# Consulter le tag précédent enregistré
PREVIOUS_TAG=$(cat current_tag.txt)

# Rétablir la version précédente
BACKEND_IMAGE="ghcr.io/amine-genin/locavite-backend:${PREVIOUS_TAG}" \
FRONTEND_IMAGE="ghcr.io/amine-genin/locavite-frontend:${PREVIOUS_TAG}" \
docker compose -f docker-compose.staging.yml up -d --remove-orphans
```

---

### 🛑 Arrêt et maintenance locale

```bash
# Arrêter les conteneurs (données conservées dans les volumes)
docker compose -f infra/docker/docker-compose.yml down

# Arrêter les conteneurs ET supprimer les volumes (réinitialisation complète)
docker compose -f infra/docker/docker-compose.yml down -v

# Consulter les logs d'un service spécifique
docker compose -f infra/docker/docker-compose.yml logs -f postgres
docker compose -f infra/docker/docker-compose.yml logs -f minio
```
