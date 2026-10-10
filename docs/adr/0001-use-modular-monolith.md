# ADR 0001 — Choisir un monolithe modulaire

- **Statut :** Accepté
- **Date :** 2026

## 📋 Contexte

LocaVite est un projet académique réalisé par une équipe de cinq personnes : un Scrum Master également en charge du backend, deux développeurs backend, un développeur frontend et un DevOps.

La plateforme doit couvrir plusieurs domaines métier — authentification, véhicules, réservations, paiements, ventes et notifications — tout en restant livrable dans un délai et avec des ressources limités. L'équipe doit pouvoir développer, tester et déployer une première version sans supporter immédiatement la complexité opérationnelle de nombreux services indépendants.

## ✅ Décision

Nous choisissons une architecture de **monolithe modulaire** basée sur Spring Boot 3. Chaque domaine métier sera isolé dans un module dédié, avec des responsabilités, interfaces et dépendances explicites. Le module `app-web` assemblera l'application et les modules partagés seront regroupés dans `module-common`.

Cette organisation conserve un déploiement applicatif unique tout en préparant une évolution ultérieure vers des services séparés si les besoins fonctionnels, les performances ou l'organisation de l'équipe le justifient.

## ⚖️ Conséquences

### Conséquences positives

- Développement et déploiement plus simples pour une petite équipe.
- Coûts d'infrastructure et de supervision réduits.
- Tests d'intégration et débogage facilités.
- Transactions et échanges entre modules plus simples à coordonner.
- Frontières métier explicites permettant une évolution progressive.
- Livraison rapide d'une première version fonctionnelle.

### Conséquences négatives

- Un déploiement unique peut limiter l'indépendance des mises en production.
- Une erreur ou une saturation du processus peut affecter plusieurs modules.
- La montée en charge ne peut pas être ajustée indépendamment par domaine.
- La discipline d'architecture est nécessaire pour éviter les dépendances croisées.
- Une migration future vers des microservices demandera un travail dédié.

## 🔎 Alternatives considérées

### Microservices dès le départ

Cette option offrirait une scalabilité et une indépendance de déploiement par domaine, mais introduirait trop tôt une complexité opérationnelle : réseau, observabilité distribuée, sécurité interservices, gestion des contrats et déploiements multiples.

### Monolithe non modulaire

Cette option serait simple au démarrage, mais favoriserait le couplage et rendrait les évolutions ainsi qu'une éventuelle extraction de services plus difficiles.

### Serverless

Cette option n'est pas retenue car elle ne correspond pas aux objectifs pédagogiques de maîtrise de Spring Boot, de la conteneurisation et de Kubernetes, et elle introduirait une dépendance importante au fournisseur cloud.
