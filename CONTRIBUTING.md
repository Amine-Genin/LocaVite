# 🤝 Guide de contribution

Merci de contribuer à LocaVite ! Ce document décrit les conventions communes afin de garder un historique lisible et une qualité constante.

## 🌿 Nommage des branches

Les branches doivent suivre le format :

```text
<type>/DGL-XX-description-courte
```

Exemples : `feature/DGL-42-authentification`, `fix/DGL-57-calcul-prix`.

Types recommandés : `feature`, `fix`, `docs`, `refactor`, `test`, `chore` et `ci`.

## ✍️ Conventional Commits

Les messages de commit suivent le format `type(scope): description`, avec une description concise à l'impératif.

Types autorisés :

- `feat` : nouvelle fonctionnalité
- `fix` : correction de bug
- `docs` : documentation
- `test` : ajout ou modification de tests
- `refactor` : refactorisation sans changement fonctionnel
- `chore` : maintenance
- `ci` : intégration et déploiement continus

Exemple : `feat(reservation): ajouter la vérification de disponibilité`

## 🔍 Règles de Pull Request

Chaque Pull Request doit :

1. Référencer un ticket Jira.
2. Décrire clairement l'objectif et les changements.
3. Être relue par au moins **un reviewer**.
4. Passer l'ensemble de la CI avec un statut vert.
5. Être fusionnée avec la stratégie **Squash and merge**.

## ✅ Definition of Done

Une tâche est considérée comme terminée lorsque :

- les tests pertinents sont ajoutés ou mis à jour ;
- la couverture de tests est supérieure à **70 %** ;
- la documentation associée est à jour ;
- le code respecte les conventions du projet ;
- la pipeline CI est verte ;
- la Pull Request a reçu au moins une approbation ;
- aucun secret ou fichier de configuration local n'est ajouté au dépôt.
