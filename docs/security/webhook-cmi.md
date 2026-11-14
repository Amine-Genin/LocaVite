# Sécurisation du Webhook CMI (`/api/payments/cmi/webhook`)

Ce document décrit l'architecture de défense en profondeur mise en place pour sécuriser les notifications serveur-à-serveur envoyées par le **Centre Monétique Interbancaire (CMI)** vers la plateforme **LocaVite** (`POST /api/payments/cmi/webhook`) afin de prévenir les fraudes, usurpations et attaques par rejeu.

---

## 1. Vue d'ensemble des couches de sécurité

Chaque requête entrante sur `/api/payments/cmi/webhook` traverse séquentiellement le filtre `CmiWebhookFilter` et le service `CmiPaymentService` :

| Ordre | Contrôle | Composant | Comportement en cas d'échec |
| :---: | :--- | :--- | :--- |
| **1** | **Journalisation d'audit complète** | `CmiWebhookFilter` | Log structuré `AUDIT_WEBHOOK_CMI` (IP réelle, en-têtes HTTP masqués, payload masqué PCI-DSS) |
| **2** | **HTTPS obligatoire (TLS 1.2 / 1.3)** | Nginx + `CmiWebhookFilter` | `403 Forbidden` + alerte `https_required` |
| **3** | **Whitelist IP CMI (IPv4 / CIDR)** | `CmiIpWhitelistService` | `403 Forbidden` + alerte `ip_not_whitelisted` |
| **4** | **Rate Limiting (`10 req/min` par IP)** | `CmiWebhookRateLimiter` | `429 Too Many Requests` (`Retry-After: 60`) + alerte `rate_limit_exceeded` |
| **5** | **Signature cryptographique HMAC-SHA512** | `CmiHashService` | `401 Unauthorized` + alerte `invalid_signature` |
| **6** | **Idempotence (`webhook_events`)** | `WebhookEventRepository` | `200 OK` (`ACTION=POSTAUTH`, `duplicate=true`) sans retraitement métier |

---

## 2. Vérification de la signature HMAC-SHA512

Le calcul de signature respecte la spécification CMI / Nestpay :
1. Tous les paramètres reçus (hors `hash`, `encoding`, `hashAlgorithm`, `hashData`) sont triés par ordre alphabétique insensible à la casse.
2. Les caractères spéciaux `\` et `|` sont échappés (`\\`, `\|`).
3. Le condensat est calculé avec l'algorithme **HMAC-SHA512** en utilisant la clé secrète marchand (`CMI_STORE_KEY`), puis encodé en Base64.
4. En cas de signature absente ou invalide sur `/api/payments/cmi/webhook`, le filtre rejette immédiatement la requête avec le code **HTTP `401 Unauthorized`**.

---

## 3. Whitelist IP CMI

Seules les adresses IP officielles des serveurs de notification CMI sont autorisées à appeler l'endpoint webhook.
- La configuration est pilotée par la variable d'environnement `CMI_IP_WHITELIST` (dans `application.yml` sous `cmi.ip-whitelist`).
- Supporte les adresses IPv4/IPv6 exactes ainsi que les plages CIDR (ex. `196.200.160.0/24`, `41.137.0.0/16`).
- Toute adresse non présente dans la liste reçoit un **HTTP `403 Forbidden`**.

```yaml
cmi:
  ip-whitelist: ${CMI_IP_WHITELIST:127.0.0.1,0:0:0:0:0:0:0:1,::1,196.200.160.0/24,41.137.0.0/16}
```

---

## 4. Rate Limiting (10 requêtes / minute par IP)

Le composant `CmiWebhookRateLimiter` applique une limite stricte de **10 requêtes par minute et par adresse IP** :
- **Mode distribué (Redis)** : utilise la clé `cmi:webhook:ratelimit:<ip>` avec un TTL de 60 secondes.
- **Mode repli local (In-Memory Bucket)** : fenêtre glissante thread-safe en mémoire si Redis est momentanément indisponible.
- Au-delà de 10 requêtes dans la fenêtre d'une minute, l'API retourne **HTTP `429 Too Many Requests`** avec l'en-tête `Retry-After: 60`.

---

## 5. Idempotence via la table `webhook_events`

La migration Flyway `V19__create_webhook_events.sql` crée la table `webhook_events` :

| Colonne | Type | Description |
| :--- | :--- | :--- |
| `id` | `BIGINT IDENTITY` | Clé primaire auto-incrémentée |
| `event_id` | `VARCHAR(150) UNIQUE` | Clé d'idempotence unique (`event_id`, `X-CMI-Event-Id` ou `CMI:<oid>:<TransId>:<ProcReturnCode>`) |
| `source` | `VARCHAR(50)` | Source de l'événement (`CMI`) |
| `payload` | `TEXT` | Payload JSON assaini (numéros de carte masqués) |
| `ip` | `VARCHAR(64)` | Adresse IP cliente ayant émis la notification |
| `received_at` | `TIMESTAMPTZ` | Horodatage de réception |
| `processed_at` | `TIMESTAMPTZ` | Horodatage de fin de traitement |

Si CMI renvoie plusieurs fois la même notification (même `event_id`), `CmiPaymentService` détecte l'entrée existante dans `webhook_events`, ignore le retraitement (aucun double paiement ni double événement publié) et répond `ACTION=POSTAUTH` (`200 OK`) pour acquitter la réception auprès de CMI.

---

## 6. HTTPS obligatoire (TLS 1.2+) et Reverse Proxy Nginx (`X-Forwarded-For`)

### Configuration Nginx
Le reverse proxy Nginx assure la terminaison TLS 1.2 / TLS 1.3 et propage l'adresse IP réelle de la passerelle CMI via `X-Forwarded-For` et le protocole via `X-Forwarded-Proto` :

```nginx
server {
    listen 443 ssl http2;
    server_name api.locavite.ma;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_prefer_server_ciphers on;
    ssl_ciphers HIGH:!aNULL:!MD5:!3DES;

    location = /api/payments/cmi/webhook {
        proxy_pass http://backend:8081/api/payments/cmi/webhook;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        proxy_read_timeout 30s;
    }
}
```

### Configuration Spring Boot
Dans `application.yml` :
```yaml
server:
  forward-headers-strategy: framework
  ssl:
    enabled-protocols: TLSv1.2,TLSv1.3

cmi:
  webhook-rate-limit-per-minute: ${CMI_WEBHOOK_RATE_LIMIT:10}
  webhook-require-https: ${CMI_WEBHOOK_REQUIRE_HTTPS:true} # true en staging/production
```

---

## 7. Supervision et Alerting (Prometheus & Alertmanager)

Le service `CmiSecurityAlertService` expose les compteurs Micrometer/Prometheus suivants sur `/actuator/metrics` :
- `cmi_webhook_requests_total{status="accepted|rejected_..."}`
- `cmi_webhook_security_failures_total{reason="invalid_signature|ip_not_whitelisted|rate_limit_exceeded|https_required"}`

Chaque échec génère également une trace critique structurée exploitable par Loki / ELK :
```text
SECURITY_ALERT [CMI_WEBHOOK] timestamp=... reason=invalid_signature ip=196.200.160.12 details="Signature HMAC-SHA512 absente ou invalide pour oid=PAY-XXXXXX"
```

Les règles d'alerte Prometheus associées sont définies dans `infra/monitoring/prometheus-cmi-alerts.yml`.
