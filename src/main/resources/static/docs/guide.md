# Guide consommateur — Yowyob Payment API

Ce guide décrit comment intégrer l'API de paiement Yowyob avec l'authentification **kernel-core** (`https://kernel-core.yowyob.com`).

> Documentation kernel complète : voir le guide auth kernel pour login, headers et JWKS.

---

## 1. Prérequis — Authentification kernel

L'API payment **ne gère plus** de login/register local. Toute authentification passe par le kernel.

### Obtenir un JWT

```http
POST https://kernel-core.yowyob.com/api/auth/login
Content-Type: application/json
X-Client-Id: <votre-client-id>
X-Api-Key: <votre-api-key>
X-Tenant-Id: <votre-tenant-id>

{
  "email": "user@example.com",
  "password": "..."
}
```

Réponse : `{ "accessToken": "<JWT RS256>", "organizations": [...], ... }`

### Headers obligatoires (routes métier JWT)

| Header | Description |
|--------|-------------|
| `Authorization` | `Bearer <accessToken>` |
| `X-Client-Id` | Identifiant application kernel |
| `X-Api-Key` | Clé API application |
| `X-Tenant-Id` | Identifiant tenant |
| `X-Organization-Id` | Organisation courante (doit correspondre au claim `oid` du JWT) |

### Claims JWT utilisés

| Claim | Usage payment |
|-------|---------------|
| `sub` | Identifiant utilisateur (`userId`) |
| `oid` | Identifiant organisation (`organizationId`) |
| `tid` | Identifiant tenant |
| `permissions` | Permissions kernel (ex. `payments:admin` pour admin) |

---

## 2. Parcours consommateur

```
1. Login kernel          → JWT RS256
2. GET /api/v1/wallets/me → portefeuille (création lazy si absent)
3. POST /api/v1/transactions → recharge ou paiement
```

### Étape 1 — Récupérer ou créer le portefeuille

```http
GET /api/v1/wallets/me
Authorization: Bearer <jwt>
X-Client-Id: <client-id>
X-Api-Key: <api-key>
X-Tenant-Id: <tenant-id>
X-Organization-Id: <organization-uuid>
```

Réponse :

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "...",
  "organizationId": "...",
  "balance": 0.00,
  "status": "ACTIVE",
  "createdAt": "...",
  "updatedAt": "..."
}
```

### Étape 2 — Recharge via Stripe Checkout

```http
POST /api/v1/transactions
Authorization: Bearer <jwt>
X-Client-Id: ...
X-Api-Key: ...
X-Tenant-Id: ...
X-Organization-Id: ...

{
  "type": "RECHARGE",
  "walletId": "550e8400-e29b-41d4-a716-446655440000",
  "amount": 1000.00,
  "method": "STRIPE",
  "callbackUrl": "https://merchant.example.com/webhooks/payment",
  "metadata": { "orderId": "ORD-42" }
}
```

Réponse : transaction `PENDING` avec `stripeCheckoutUrl` — rediriger l'utilisateur vers cette URL.

### Étape 3 — Recharge immédiate (solde interne)

```json
{
  "type": "RECHARGE",
  "walletId": "...",
  "amount": 1000.00,
  "method": "WALLET"
}
```

### Étape 4 — Paiement via wallet

```json
{
  "type": "WALLET_PAYMENT",
  "walletId": "...",
  "amount": 500.00,
  "method": "WALLET"
}
```

---

## 3. Paiement direct (client credentials)

Pour les paiements serveur-à-serveur **sans JWT utilisateur** :

```http
POST /api/v1/transactions/direct
X-Client-Id: <client-id>
X-Api-Key: <api-key>
X-Tenant-Id: <tenant-id>
X-Organization-Id: <organization-uuid>

{
  "amount": 5000.00,
  "method": "STRIPE",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "organizationId": "...",
  "callbackUrl": "https://merchant.example.com/webhooks/payment",
  "metadata": { "invoiceId": "INV-99" }
}
```

> Pas de header `Authorization` requis pour cette route.

Réponse : transaction `PENDING` avec `stripeCheckoutUrl`.

---

## 4. Webhooks consommateur (HTTP sortant)

Fournissez `callbackUrl` (HTTPS) à la création de la transaction. Le service vous notifie via **POST JSON** aux étapes :

| Événement | Déclencheur |
|-----------|-------------|
| `TRANSACTION_PENDING` | Session Stripe créée (`stripeCheckoutUrl` inclus) |
| `TRANSACTION_SUCCEEDED` | Paiement finalisé ou crédit wallet |
| `TRANSACTION_FAILED` | Échec paiement ou débit |
| `TRANSACTION_CANCELLED` | Annulation Stripe (`GET /stripe/cancel`) |

Le champ `metadata` est renvoyé tel quel dans chaque notification. Livraison **asynchrone** avec retry (outbox). **Kafka** publie aussi les événements en parallèle.

---

## 5. Webhooks Stripe (entrant)

Configurer Stripe pour envoyer les événements à :

```
POST /api/v1/stripe/webhooks
```

Signature vérifiée via `STRIPE_WEBHOOK_SECRET`.

À la réception de `checkout.session.completed`, le wallet est crédité (recharge) ou la transaction est finalisée (paiement direct).

Callbacks navigateur :

- Succès : `GET /api/v1/stripe/success`
- Annulation : `GET /api/v1/stripe/cancel`

---

## 6. Codes d'erreur métier

| Code HTTP | Message type | Cause |
|-----------|--------------|-------|
| 401 | Authentification requise | JWT ou headers kernel manquants/invalides |
| 403 | Accès refusé | Permissions insuffisantes |
| 404 | Portefeuille/transaction introuvable | Ressource inexistante ou accès non autorisé |
| 422 | Méthode non supportée | `MOMO`, `PAYPAL` non implémentés |
| 400 | Montant invalide | Hors limites min/max configurées |
| 400 | Solde insuffisant | Débit wallet impossible |

Toutes les erreurs retournent un JSON structuré : `status`, `code`, `message`, `fieldErrors`, `timestamp`.

---

## 7. Références

- **Swagger interactif** : [/swagger-ui.html](/swagger-ui.html)
- **Santé API** : [/actuator/health](/actuator/health)
- **Kernel auth** : `https://kernel-core.yowyob.com`
- **JWKS** : `https://kernel-core.yowyob.com/.well-known/jwks.json`

## Variables d'environnement (côté payment)

```env
KERNEL_BASE_URL=https://kernel-core.yowyob.com
KERNEL_CLIENT_ID=<votre-client-id>
KERNEL_API_KEY=<votre-api-key>
KERNEL_TENANT_ID=<votre-tenant-id>
PAYMENTS_ADMIN_PERMISSIONS=payments:admin
```
