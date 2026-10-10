# ⚙️ Infrastructure

Le dossier `infra/` regroupera les ressources nécessaires au développement, au déploiement et à l'exploitation de LocaVite.

## 🏗️ Composants prévus

| Dossier | Contenu |
|---|---|
| `docker/` | Images et configurations de conteneurs pour les environnements locaux |
| `kubernetes/` | Manifests, namespaces, services, ingress et déploiements |
| `terraform/` | Provisionnement déclaratif de l'infrastructure |
| `ansible/` | Automatisation de la configuration des environnements |
| `monitoring/` | Supervision, métriques, journaux et alertes |

L'infrastructure intégrera notamment PostgreSQL, Redis et MinIO, avec des environnements distincts pour le développement, la validation et la production.
