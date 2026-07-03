# Yowyob Payment

API de paiement hexagonale (WebFlux + R2DBC) : wallets, transactions (recharge, paiement wallet, paiement direct Stripe), auth JWT, Kafka, Redis.

## Démarrage

```bash
cd yowyob-payment/docker
cp .env.example .env
docker compose up --build
```

- API : <http://localhost:8080>
- Swagger : <http://localhost:8080/swagger-ui.html>
- Santé : <http://localhost:8080/actuator/health>

## Développement local

```bash
cd yowyob-payment
export $(grep -v '^#' docker/.env.example | xargs)
./mvnw spring-boot:run
```

## Endpoints principaux

| Méthode | Chemin | Auth |
|---------|--------|------|
| POST | `/api/v1/auth/register` | Public |
| POST | `/api/v1/auth/login` | Public |
| POST | `/api/v1/transactions/recharge` | JWT |
| POST | `/api/v1/transactions/wallet-payment` | JWT |
| POST | `/api/v1/transactions/direct` | Header `X-Api-Key` |
| POST | `/api/v1/stripe/webhooks` | Signature Stripe |

## Transactions et Stripe Checkout

Stripe Checkout est intégré directement dans les endpoints transactions via `method=STRIPE` :

### Recharge wallet via Stripe

```http
POST /api/v1/transactions/recharge
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "walletId": "550e8400-e29b-41d4-a716-446655440000",
  "amount": 1000.00,
  "method": "STRIPE"
}
```

Réponse : transaction `PENDING` avec `stripeCheckoutUrl` - rediriger l'utilisateur vers cette URL. Le webhook Stripe crédite le wallet à la confirmation.

### Recharge wallet immédiate (solde interne)

```json
{ "walletId": "...", "amount": 1000.00, "method": "WALLET" }
```

### Paiement direct (sans wallet)

```http
POST /api/v1/transactions/direct
X-Api-Key: <clé-api-application>
Content-Type: application/json

{
  "amount": 5000.00,
  "method": "STRIPE",
  "userId": "550e8400-e29b-41d4-a716-446655440000"
}
```

Réponse : transaction `PENDING` avec `stripeCheckoutUrl`.

## Erreurs API

Toutes les erreurs retournent un JSON explicite (`status`, `code`, `message`, `fieldErrors`, `timestamp`). Aucun message générique.

## Tests

```bash
./mvnw test
```

## Dépréciation

Le microservice `yowyob-pay/user-payment-service-main` est remplacé par ce monolithe (`auth` + `wallets` + `transactions` intégrés).

Le projet `payment-service/` (gateways MyCoolPay/PayPal/Kafka avancé) reste un legacy séparé non modifié.

## Stack

- Java 21, Spring Boot 3.2, WebFlux, R2DBC, Liquibase (Docker), Redis, Kafka, Stripe Checkout
