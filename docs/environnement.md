# 🛠️ Environnement de développement

<!-- Les versions sont épinglées afin de limiter les écarts entre les postes de l'équipe. -->

## 📋 Versions requises

| Outil | Version requise | Utilisation |
|---|---:|---|
| Java | 21 | Compilation et exécution du backend |
| Maven | 3.9+ | Gestion des dépendances et build Spring Boot |
| Node.js | 20 | Runtime du frontend Angular |
| npm | 10+ | Gestion des dépendances frontend |
| Angular | 18 | Framework frontend |
| Docker | 27+ | Conteneurisation et environnement local |
| Docker Compose | v2+ | Orchestration des services locaux |
| PostgreSQL | 16 | Base de données relationnelle |
| Redis | 7 | Cache et données temporaires |
| MinIO | Release épinglée dans Compose | Stockage objet compatible S3 |

## 📥 Installation

### macOS

```bash
brew install openjdk@21 maven node
brew install --cask docker
npm install -g @angular/cli@18
```

### Windows

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
winget install Apache.Maven
winget install OpenJS.NodeJS.LTS
winget install Docker.DockerDesktop
npm install -g @angular/cli@18
```

### Linux Debian/Ubuntu

```bash
sudo apt update
sudo apt install openjdk-21-jdk maven nodejs npm docker.io docker-compose-v2
npm install -g @angular/cli@18
```

<!-- Pour une installation Docker plus récente, utiliser la documentation officielle Docker. -->

## 🔎 Vérification

```bash
java --version
mvn --version
node --version
npm --version
ng version
docker --version
docker compose version
```

Les versions affichées doivent respecter le tableau ci-dessus. Le fichier `.tool-versions` permet de sélectionner automatiquement Java, Maven et Node.js avec `asdf`.

## 🚀 Démarrage des services locaux

Depuis la racine du dépôt :

```bash
docker compose -f infra/docker/docker-compose.yml up -d
docker compose -f infra/docker/docker-compose.yml ps
```

Les services sont disponibles aux adresses suivantes :

| Service | Adresse |
|---|---|
| PostgreSQL | `localhost:5432` |
| Redis | `localhost:6379` |
| MinIO API | `http://localhost:9000` |
| Console MinIO | `http://localhost:9001` |

Pour arrêter les conteneurs sans supprimer les données :

```bash
docker compose -f infra/docker/docker-compose.yml down
```

Pour supprimer également les volumes de développement :

```bash
docker compose -f infra/docker/docker-compose.yml down -v
```
