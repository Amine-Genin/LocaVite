# ADR 0002 — Standardiser la gestion des dépendances

- **Statut :** Accepté
- **Date :** 2026

## 📋 Contexte

LocaVite combine un backend Spring Boot 3, un frontend Angular 18 et plusieurs services d'infrastructure. L'équipe doit disposer d'un environnement reproductible, limiter les incompatibilités de versions et faciliter les builds locaux ainsi que la CI GitHub Actions.

Les dépendances doivent être déclarées dans les gestionnaires natifs de chaque écosystème, tandis que les images de services doivent être versionnées explicitement pour éviter qu'un téléchargement ultérieur ne change le comportement de l'environnement.

## ✅ Décision

- **Maven** est utilisé pour le backend Java via `backend/pom.xml`.
- **npm** est utilisé pour le frontend Angular via `frontend/package.json`.
- Les versions Angular, des outils frontend et des bibliothèques applicatives sont épinglées dans `package.json`.
- Spring Boot 3.3.4 fournit la gestion centralisée des versions compatibles du socle Spring.
- Les versions complémentaires importantes du backend sont déclarées dans les propriétés Maven.
- Les images Docker PostgreSQL, Redis et MinIO sont épinglées dans `infra/docker/docker-compose.yml`.
- Java 21, Maven 3.9.9 et Node.js 20.18.0 sont documentés dans `.tool-versions`.
- Les services locaux sont lancés par Docker Compose ; les secrets indiqués dans Compose sont uniquement des identifiants de développement.

## ⚖️ Conséquences

### Conséquences positives

- Builds plus reproductibles entre les postes et la CI.
- Détection plus rapide des incompatibilités de dépendances.
- Onboarding facilité grâce aux versions et commandes documentées.
- Mises à jour traçables dans les fichiers de configuration.
- Environnements locaux PostgreSQL, Redis et MinIO cohérents.

### Conséquences négatives

- Les mises à jour nécessitent une modification volontaire des fichiers de dépendances.
- Les fichiers de verrouillage npm et les caches Maven doivent être maintenus dans la CI.
- La compatibilité des versions doit être vérifiée avant chaque montée de version.
- Les identifiants de développement ne doivent jamais être utilisés dans un environnement partagé ou de production.

## 🔎 Alternatives considérées

### Utiliser uniquement les versions flottantes

Cette approche simplifie les mises à jour, mais rend les builds non reproductibles et peut introduire des régressions sans modification du dépôt.

### Utiliser un gestionnaire unique pour toute la stack

Cette option ne respecte pas les pratiques natives des écosystèmes Java et Node.js et compliquerait l'intégration avec Spring Boot et Angular.

### Gérer les services avec des installations locales

Cette solution éviterait Docker, mais produirait des différences importantes de configuration entre les systèmes d'exploitation des membres de l'équipe.
