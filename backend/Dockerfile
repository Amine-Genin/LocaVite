# ==============================================================================
# Multi-stage Dockerfile pour LocaVite Backend (Spring Boot 3 + Java 21)
# ==============================================================================

# --- Stage 1 : Compilation du JAR avec Maven ---
FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Mise en cache des dépendances Maven
COPY pom.xml ./
COPY module-common ./module-common
RUN mvn dependency:go-offline -B -Daether.remoteRepositoryFilter.prefixes=false -Dmaven.wagon.http.retryHandler.count=3 || true

# Copie des sources et packaging de l'exécutable Spring Boot
COPY src ./src
RUN mvn clean package -DskipTests -B -Daether.remoteRepositoryFilter.prefixes=false -Dmaven.wagon.http.retryHandler.count=3

# --- Stage 2 : Image d'exécution minimale JRE 21 Alpine ---
FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.source="https://github.com/amine-genin/LocaVite"
LABEL org.opencontainers.image.description="LocaVite Backend Spring Boot application"

# Outils système pour healthcheck
RUN apk add --no-cache curl tzdata

# Utilisateur non-root sécurisé
RUN addgroup -S locavite && adduser -S locavite -G locavite

WORKDIR /app

# Récupération du fat JAR construit
COPY --from=builder /build/target/locavite-backend-*.jar app.jar

RUN chown -R locavite:locavite /app

USER locavite:locavite

ENV PORT=8081 \
    SPRING_PROFILES_ACTIVE=staging \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8081

# Health check Actuator Spring Boot
HEALTHCHECK --interval=15s --timeout=5s --retries=5 --start-period=30s \
    CMD curl -f http://localhost:${PORT}/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
