# ⚙️ Infrastructure LocaVite

Le dossier `infra/` regroupe l'ensemble des ressources nécessaires au développement, au déploiement et à l'exploitation de la plateforme **LocaVite**.

## 🏗️ Composants

| Dossier | Contenu |
|---|---|
| `docker/` | Configurations Docker Compose pour le développement local (`docker-compose.yml`), SonarQube (`docker-compose.sonar.yml`) et le staging (`docker-compose.staging.yml`) |
| `kubernetes/` | Manifests Kubernetes / K3s (`staging-deployment.yaml`) : Namespace, Deployments, Services, Probes et Ingress |
| `scripts/` | Scripts d'automatisation et de test du health check / rollback (`test-staging-rollback.sh`) |

---

## 🐳 Environnement local (Docker Compose)

Le fichier `infra/docker/docker-compose.yml` permet de démarrer l'ensemble des dépendances d'infrastructure nécessaires au backend en local.

### 📦 Services inclus

| Service | Image | Port hôte | Rôle | Healthcheck |
|---|---|---|---|---|
| **PostgreSQL** | `postgres:16-alpine` | `5433` (ou `5432`) | Base de données relationnelle | `pg_isready` |
| **Redis** | `redis:7-alpine` | `6379` | Cache et sessions applicatives | `redis-cli ping` |
| **MinIO** | `minio/minio` | `9000` (API) / `9001` (Console) | Stockage objet compatible S3 (documents, CIN, permis, véhicules) | `mc ready local` |
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

Le workflow [`.github/workflows/deploy-staging.yml`](../.github/workflows/deploy-staging.yml) orchestre le déploiement automatique sur l'environnement de staging après chaque merge/push sur `main` une fois la CI verte.

### 🔄 Schéma du Cycle de Déploiement

```text
[Push sur main / Merge PR]
           │
           ▼
[Job 1 : verify-ci (Wait & Verify CI Green)]
  - Vérifie que Backend CI & Frontend CI sont au vert sur le commit
           │
           ▼
[Job 2 : build-and-push (Build Docker Backend & Frontend)]
  - Tag: staging-{sha} (court + complet) & staging-latest
  - Multi-stage builds optimisés avec cache GitHub Actions
           │
           ▼
[Push vers GitHub Container Registry (ghcr.io)]
  - ghcr.io/amine-genin/locavite-backend:staging-{sha}
  - ghcr.io/amine-genin/locavite-frontend:staging-{sha}
           │
           ▼
[Job 3 : deploy-staging (Déploiement SSH sur Serveur Staging)]
  - SCP infra/docker/docker-compose.staging.yml -> /opt/locavite/staging/
  - Sauvegarde du tag actif (current_tag.txt) & candidat (candidate_tag.txt)
  - docker pull & docker compose up -d --remove-orphans
           │
           ▼
[Health Check Post-Déploiement]
  - Boucle de vérification curl http://localhost:8081/actuator/health (12 essais x 5s)
  - Vérification Frontend curl http://localhost:80/healthz (6 essais x 3s)
           │
     ┌─────┴─────┐
     ▼           ▼
[Succès]      [Échec du Health Check]
  - Valider tag   - ROLLBACK AUTOMATIQUE
  - Prune images    vers le tag précédent (current_tag.txt)
  - Notify Slack  - Rétablissement conteneurs stables
                  - Alerte Slack / Discord (Erreur)
```

### 🔐 Secrets GitHub Actions Requis

Pour activer le déploiement sur le serveur staging, configurez les secrets suivants dans votre dépôt GitHub (`Settings > Secrets and variables > Actions`) :

| Nom du Secret | Description | Exemple |
|---|---|---|
| `STAGING_HOST` | IP ou nom de domaine FQDN du serveur staging | `staging.locavite.ma` ou `198.51.100.42` |
| `STAGING_USER` | Utilisateur Linux disposant des droits Docker (optionnel, défaut `deploy`) | `deploy` ou `ubuntu` |
| `STAGING_SSH_KEY` | Clé privée SSH (OpenSSH Ed25519 ou RSA) | `-----BEGIN OPENSSH PRIVATE KEY-----...` |
| `STAGING_PORT` | Port SSH du serveur distant (optionnel, défaut `22`) | `22` |
| `SLACK_WEBHOOK` | Webhook Slack ou Discord pour les notifications | `https://hooks.slack.com/services/...` |
| `GITHUB_TOKEN` | Fourni automatiquement par GitHub Actions | Utilisé pour l'API CI et `ghcr.io` |

---

### 📦 Option A : Déploiement sur VPS via SSH & Docker Compose

Si vous devez intervenir directement sur le serveur staging VPS :

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

### ☸️ Option B : Déploiement sur K3s / Kubernetes managé

Les manifests Kubernetes pour un cluster K3s ou Kubernetes managé sont disponibles dans [`infra/kubernetes/staging-deployment.yaml`](kubernetes/staging-deployment.yaml) :

```bash
# 1. Appliquer le namespace, les déploiements, services et ingress
kubectl apply -f infra/kubernetes/staging-deployment.yaml

# 2. Mettre à jour l'image vers un tag spécifique staging-{sha}
kubectl -n locavite-staging set image deployment/locavite-backend \
  backend=ghcr.io/amine-genin/locavite-backend:staging-${SHA}
kubectl -n locavite-staging set image deployment/locavite-frontend \
  frontend=ghcr.io/amine-genin/locavite-frontend:staging-${SHA}

# 3. Vérifier le rollout (Health check via readinessProbe /actuator/health/readiness)
kubectl -n locavite-staging rollout status deployment/locavite-backend --timeout=90s

# 4. En cas d'échec, rollback Kubernetes instantané
kubectl -n locavite-staging rollout undo deployment/locavite-backend
kubectl -n locavite-staging rollout undo deployment/locavite-frontend
```

---

### 🛡️ Procédure de Rollback & Tests de Validation

Le rollback est **automatique** dans le pipeline CI/CD si le Health Check (`/actuator/health` ou `/healthz`) échoue.

Pour déclencher un rollback **manuellement** sur le serveur VPS :

```bash
cd /opt/locavite/staging

# Consulter le tag précédent enregistré
PREVIOUS_TAG=$(cat current_tag.txt)

# Rétablir la version précédente
BACKEND_IMAGE="ghcr.io/amine-genin/locavite-backend:${PREVIOUS_TAG}" \
FRONTEND_IMAGE="ghcr.io/amine-genin/locavite-frontend:${PREVIOUS_TAG}" \
docker compose -f docker-compose.staging.yml up -d --remove-orphans
```

Pour tester le mécanisme de Health Check et de Rollback automatique localement :

```bash
bash infra/scripts/test-staging-rollback.sh
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
