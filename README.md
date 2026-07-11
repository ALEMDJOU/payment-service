# Yowyob Payment

API de paiement hexagonale (WebFlux + R2DBC) : wallets, transactions (recharge, paiement wallet, paiement direct Stripe), auth **kernel-core**, Kafka, Redis.

## Démarrage

```bash
cd yowyob-payment/docker
cp .env.example .env
# Renseigner KERNEL_CLIENT_ID, KERNEL_API_KEY, KERNEL_TENANT_ID
docker compose up --build
```

Le service `liquibase` applique les migrations **avant** le démarrage de l'application (`app` dépend de `liquibase` avec `service_completed_successfully`). Ne lancez pas l'API sans schéma à jour.

### Déploiement / base existante

Sur une base PostgreSQL déjà provisionnée (hors Docker Compose), exécutez Liquibase **avant** de démarrer l'application :

```bash
cd yowyob-payment/docker
export $(grep -v '^#' .env | xargs)

docker run --rm \
  -v "$(pwd)/liquibase/changelog:/liquibase/changelog" \
  liquibase/liquibase:4.27 \
  --url="jdbc:postgresql://${DB_HOST:-localhost}:${DB_PORT:-5432}/${DB_NAME}" \
  --username="${DB_USER}" \
  --password="${DB_PASSWORD}" \
  --changeLogFile=db.changelog-master.xml \
  --search-path=/liquibase/changelog \
  update
```

Pour une base créée avec une ancienne version du schéma (sans `callback_url` / `metadata` sur `yy-pay-transactions`), le changeset `004-alter-transactions-callback-metadata` ajoute les colonnes manquantes de façon idempotente.

- API : <http://localhost:8080>
- **Guide consommateur** : <http://localhost:8080/docs> → `/docs/guide.md`
- Swagger : <http://localhost:8080/swagger-ui.html>
- Santé : <http://localhost:8080/actuator/health>

## Développement local

```bash
cd yowyob-payment
export $(grep -v '^#' docker/.env.example | xargs)
# Définir KERNEL_CLIENT_ID, KERNEL_API_KEY, KERNEL_TENANT_ID
./mvnw spring-boot:run
```

## Authentification kernel

L'auth locale (login/register JWT HS256) a été supprimée. Tous les appels métier passent par **kernel-core** :

| Header | Description |
|--------|-------------|
| `Authorization` | `Bearer <JWT RS256>` (obtenu via `POST /api/auth/login` sur kernel-core) |
| `X-Client-Id` | Identifiant application |
| `X-Api-Key` | Clé API application |
| `X-Tenant-Id` | Identifiant tenant |
| `X-Organization-Id` | Organisation courante (claim `oid`) |

Admin : permission configurable via `PAYMENTS_ADMIN_PERMISSIONS` (défaut `payments:admin`).

## Endpoints principaux

| Méthode | Chemin | Auth |
|---------|--------|------|
| GET | `/api/v1/wallets/me` | JWT + headers kernel |
| POST | `/api/v1/transactions` | JWT + headers kernel |
| POST | `/api/v1/transactions/direct` | Client credentials (sans Bearer) |
| POST | `/api/v1/stripe/webhooks` | Signature Stripe |
| GET | `/docs` | Public (redirige vers guide) |

## Transactions et Stripe Checkout

### Recharge wallet via Stripe

```http
POST /api/v1/transactions
Authorization: Bearer <jwt>
X-Client-Id: <client-id>
X-Api-Key: <api-key>
X-Tenant-Id: <tenant-id>
X-Organization-Id: <org-uuid>
Content-Type: application/json

{
  "type": "RECHARGE",
  "walletId": "550e8400-e29b-41d4-a716-446655440000",
  "amount": 1000.00,
  "method": "STRIPE"
}
```

Réponse : transaction `PENDING` avec `stripeCheckoutUrl`.

### Paiement wallet

```json
{ "type": "WALLET_PAYMENT", "walletId": "...", "amount": 500.00, "method": "WALLET" }
```

### Paiement direct (sans wallet)

```http
POST /api/v1/transactions/direct
X-Client-Id: <client-id>
X-Api-Key: <api-key>
X-Tenant-Id: <tenant-id>
X-Organization-Id: <org-uuid>
Content-Type: application/json

{
  "amount": 5000.00,
  "method": "STRIPE",
  "userId": "550e8400-e29b-41d4-a716-446655440000"
}
```

## Erreurs API

Toutes les erreurs retournent un JSON explicite (`status`, `code`, `message`, `fieldErrors`, `timestamp`).

## Tests

```bash
./mvnw verify
```

Les tests d'intégration R2DBC (`TransactionR2dbcAdapterIT`) utilisent Testcontainers PostgreSQL et valident la persistance des colonnes `callback_url` et `metadata`.

## Stack

- Java 21, Spring Boot 3.2, WebFlux, R2DBC, Liquibase (Docker), Redis, Kafka, Stripe Checkout
- Auth : kernel-core JWKS RS256
- `stripe listen --forward-to localhost:8080/api/v1/stripe/webhooks`
