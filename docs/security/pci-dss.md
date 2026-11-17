# PCI-DSS - Paiements LocaVite

## Périmètre et tokenisation

LocaVite délègue la saisie et le traitement des cartes à CMI via la page 3-D
Secure hébergée. Le backend ne reçoit ni ne persiste de PAN, CVV/CVC, date
d'expiration ou piste magnétique. La base conserve uniquement les identifiants
techniques CMI, les montants, les statuts et les références de transaction.
Les payloads CMI et les logs sont nettoyés par `CmiDataSanitizer`; les champs
sensibles sont remplacés par une valeur masquée.

## Transport

Les routes `/api/payments/**` exigent HTTPS lorsque
`SECURITY_REQUIRE_HTTPS=true` (valeur par défaut). Le proxy TLS doit accepter
TLS 1.2 et TLS 1.3, désactiver TLS 1.0/1.1 et transmettre `X-Forwarded-Proto`
à l'application. Les URL CMI de callback et webhook doivent être des URL
HTTPS en staging et en production.

La valeur `security.require-https=false` est réservée aux tests locaux. Elle ne
doit jamais être utilisée dans un déploiement exposé.

## Chiffrement au repos

- PostgreSQL doit être fourni par une infrastructure avec TDE ou chiffrement
  de volume géré, avec clés conservées et renouvelées dans Vault/KMS.
- Les objets MinIO sont envoyés avec SSE-S3 lorsque `MINIO_SSE_ENABLED=true`.
  Le cluster MinIO doit être configuré avec un KMS et ses volumes doivent
  également être chiffrés.
- Les mots de passe, clés CMI, JWT et identifiants MinIO sont injectés par
  variables protégées ou Vault; aucune valeur de production ne doit être
  committée.

## Audit trail

La migration `V20__create_audit_log.sql` crée `audit_log` avec l'utilisateur,
l'action, l'entité, l'identifiant d'entité, l'IP, le user-agent, l'horodatage et
un détail JSON. `@Auditable` et `AuditAspect` tracent les lectures et écritures
des paiements, remboursements et factures, y compris les échecs. Le détail est
sanitisé avant persistance et l'écriture d'audit utilise une transaction
indépendante afin de conserver la trace d'un appel métier échoué.

Les accès aux journaux doivent être limités aux opérateurs autorisés. Les
journaux sont conservés selon la politique de rétention approuvée, puis
supprimés de manière vérifiable.

## Rotation des secrets

Les secrets sont stockés dans Vault ou un gestionnaire équivalent. La rotation
CMI, JWT, base et MinIO doit être automatisée sans exposer les valeurs dans les
logs. Une rotation implique une période de chevauchement contrôlée, un test de
connectivité, puis la révocation de l'ancienne clé.

## Contrôles CI

La CI exécute:

- Trivy sur le dépôt et l'image applicative avec échec sur les vulnérabilités
  `CRITICAL`;
- OWASP Dependency-Check sur le backend;
- les tests Maven, les tests unitaires Angular, le lint et les tests Cypress
  E2E définis par les workflows.

Un résultat de scan doit être traité ou documenté avant le merge; le statut
vert de la CI ne remplace pas une revue PCI-DSS de production.
