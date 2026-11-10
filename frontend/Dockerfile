# ==============================================================================
# Multi-stage Dockerfile pour LocaVite Frontend (Angular 18 + Nginx)
# ==============================================================================

# --- Stage 1 : Build de l'application Angular ---
FROM node:20-alpine AS builder

WORKDIR /app

# Copie des manifestes de dépendances pour tirer parti du cache Docker
COPY package*.json ./
RUN npm ci --ignore-scripts

# Copie des sources et compilation de production
COPY . .
RUN npm run build -- --configuration production

# Consolidation du dossier des assets construits
RUN mkdir -p /public && \
    if [ -d "dist/locavite/browser" ]; then \
        cp -r dist/locavite/browser/* /public/; \
    else \
        cp -r dist/locavite/* /public/; \
    fi

# --- Stage 2 : Runtime léger avec Nginx Alpine ---
FROM nginx:1.27-alpine

LABEL org.opencontainers.image.source="https://github.com/amine-genin/LocaVite"
LABEL org.opencontainers.image.description="LocaVite Frontend Angular application"

# Configuration Nginx SPA + Reverse Proxy API
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Déploiement des artefacts compilés
COPY --from=builder /public /usr/share/nginx/html

# Permissions pour l'exécution avec l'utilisateur non-root nginx
RUN chown -R nginx:nginx /usr/share/nginx/html /var/cache/nginx /var/log/nginx /etc/nginx/conf.d && \
    touch /var/run/nginx.pid && \
    chown -R nginx:nginx /var/run/nginx.pid

USER nginx

EXPOSE 80

# Health check HTTP
HEALTHCHECK --interval=15s --timeout=5s --retries=3 --start-period=5s \
    CMD wget --quiet --tries=1 --spider http://localhost/healthz || exit 1

CMD ["nginx", "-g", "daemon off;"]
