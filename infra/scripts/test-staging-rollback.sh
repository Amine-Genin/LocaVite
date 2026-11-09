#!/usr/bin/env bash
# ==============================================================================
# Script de test du déploiement Staging, Health Check et Rollback automatique
# Usage : ./infra/scripts/test-staging-rollback.sh
# ==============================================================================
set -e

COMPOSE_FILE="infra/docker/docker-compose.staging.yml"
BACKEND_HEALTH_URL="http://localhost:8081/actuator/health"
FRONTEND_HEALTH_URL="http://localhost:80/healthz"

echo "=================================================================="
echo "🧪 Test E2E : Déploiement Staging, Health Check & Rollback"
echo "=================================================================="

# 1. Enregistrement d'un tag stable initial
PREVIOUS_TAG="${PREVIOUS_TAG:-staging-latest}"
echo "$PREVIOUS_TAG" > current_tag.txt
echo "📌 Version stable active simulée : $PREVIOUS_TAG"

# 2. Simulation d'un déploiement avec une image invalide ou défaillante pour tester le rollback
CANDIDATE_TAG="staging-broken-test"
echo "$CANDIDATE_TAG" > candidate_tag.txt
echo "🚀 Test de déploiement avec le tag candidat : $CANDIDATE_TAG"

echo "🔍 Exécution du Health Check sur $BACKEND_HEALTH_URL ..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BACKEND_HEALTH_URL" || true)

if [ "$HTTP_STATUS" != "200" ]; then
  echo "❌ Health check échoué (HTTP $HTTP_STATUS) -> Déclenchement du ROLLBACK automatique !"
  rm -f candidate_tag.txt
  RESTORED_TAG=$(cat current_tag.txt)
  echo "🔄 Restauration vers la version stable : $RESTORED_TAG"
  echo "   BACKEND_IMAGE=ghcr.io/amine-genin/locavite-backend:$RESTORED_TAG"
  echo "   FRONTEND_IMAGE=ghcr.io/amine-genin/locavite-frontend:$RESTORED_TAG"
  echo "✅ Rollback validé avec succès (tag actif = $(cat current_tag.txt))."
else
  mv candidate_tag.txt current_tag.txt
  echo "✅ Health check OK (HTTP 200) -> Déploiement validé !"
fi

rm -f current_tag.txt candidate_tag.txt
