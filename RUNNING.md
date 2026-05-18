# Running the backend

Demo-ready stack: PostgreSQL + Valkey (Redis) + HashiCorp Vault + Spring Boot 4 on Java 25.

## Prerequisites

- Docker Desktop (or any Docker-compatible runtime)
- JDK 25 (Temurin / Liberica)
- A POSIX shell or PowerShell

## 1. Start the infrastructure

From the `backend/` directory:

```bash
docker compose up -d postgres redis vault
```

This boots three services:

| Service  | Port             | Purpose                              |
|----------|------------------|--------------------------------------|
| postgres | 127.0.0.1:5432   | Schema lives here (managed by Liquibase) |
| redis    | 127.0.0.1:6379   | Refresh-token store                  |
| vault    | 127.0.0.1:8200   | Dev-mode Vault, root token = `root`  |

Wait until all three are healthy:

```bash
docker compose ps
```

## 2. Run the app

```bash
./gradlew bootRun
```

What happens on first boot:

1. Liquibase applies the migrations in `src/main/resources/db/liquibase/migrations/` (users, accounts, transactions, transaction_entries, …).
2. `VaultJwtKeys` reads `secret/backend/jwt` from Vault.
3. If empty, it generates a fresh ECDSA P-256 keypair and writes both PEMs into Vault, then loads them. Subsequent restarts read what's there.
4. The app starts on `:8080`.

Look for these log lines:

```
Vault path 'secret/backend/jwt' is empty — generating a fresh ECDSA P-256 keypair and seeding it.
Seeded JWT EC keys at Vault path 'secret/backend/jwt'
```

(or, on subsequent runs:)

```
Loaded JWT EC keys from Vault path 'secret/backend/jwt'
```

## 3. Try the API

### Swagger UI

Open <http://localhost:8080/swagger-ui.html>. Operations are grouped by tag: **auth**, **transactions**, **users**. The spec served at `/v3/api-docs` is generated from the same OpenAPI yaml that `openapi-generator` consumes.

### Manual curl

The endpoints set HTTP-only cookies, so `--cookie-jar` is needed for any flow that touches secured paths.

```bash
# 1. Log in (assumes a user already exists in the DB)
curl -i -c jar.txt -X POST http://localhost:8080/api/v1/auth/login \
     -H 'Content-Type: application/json' \
     -d '{"email":"demo@example.com","password":"super-secret"}'

# 2. Current user (projection — no passwd_hash)
curl -s -b jar.txt http://localhost:8080/api/v1/users/me

# 3. Create a transaction (debit + credit entries)
curl -s -b jar.txt -X POST http://localhost:8080/api/v1/transactions \
     -H 'Content-Type: application/json' \
     -d '{
       "idempotencyKey": "demo-001",
       "transactionType": "TRANSFER",
       "amount": 250.00,
       "currency": "USD",
       "description": "demo",
       "debitAccountId":  "<uuid of source account>",
       "creditAccountId": "<uuid of destination account>"
     }'

# 4. Read it back (response includes the @OneToMany entries via projection)
curl -s -b jar.txt http://localhost:8080/api/v1/transactions/<tx-uuid>
```

## 4. Inspecting Vault (defense demo)

To show the stored keys live:

```bash
# PowerShell
./scripts/vault-inspect.ps1
# bash
./scripts/vault-inspect.sh
```

Or directly via the Vault CLI (inside the container):

```bash
docker exec -e VAULT_TOKEN=root -e VAULT_ADDR=http://127.0.0.1:8200 \
    vault vault kv get secret/backend/jwt
```

To prove Vault is the live source — delete the secret, restart the app, and watch it regenerate fresh keys:

```bash
docker exec -e VAULT_TOKEN=root -e VAULT_ADDR=http://127.0.0.1:8200 \
    vault vault kv delete secret/backend/jwt
# then re-run ./gradlew bootRun
```

## 5. Seeding demo data

The app boots with no users or accounts. For the defense demo, insert a user and two accounts directly:

```sql
-- requires DB_PASSWORD env var on the shell
INSERT INTO roles (id, name) VALUES (1, 'ROLE_USER') ON CONFLICT DO NOTHING;

WITH new_user AS (
    INSERT INTO users (email, passwd_hash, first_name, last_name, kyc_status)
    VALUES ('demo@example.com',
            -- argon2 hash of "super-secret-12"
            '$argon2id$v=19$m=16384,t=2,p=1$dummysalt$dummyhash',
            'Demo', 'User', 'VERIFIED')
    RETURNING id
)
INSERT INTO user_roles (user_id, role_id) SELECT id, 1 FROM new_user;

INSERT INTO accounts (user_id, account_number, account_type, currency, balance, status)
SELECT id, 'ACC-001', 'CHECKING', 'USD', 1000.00, 'ACTIVE' FROM users WHERE email = 'demo@example.com';

INSERT INTO accounts (user_id, account_number, account_type, currency, balance, status)
SELECT id, 'ACC-002', 'SAVINGS',  'USD', 0.00,    'ACTIVE' FROM users WHERE email = 'demo@example.com';
```

The password hash above is a placeholder — generate a real one with the same `Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()` configuration the app uses (see [SecurityConfig.passwordEncoder()](src/main/java/ru/rxyvea/backend/security/SecurityConfig.java)), or temporarily expose a signup endpoint.

## Troubleshooting

| Symptom                                                | Fix                                                                                  |
|--------------------------------------------------------|--------------------------------------------------------------------------------------|
| App fails with "Vault unreachable" / connection refused | `docker compose ps` — is the `vault` container healthy? Check `VAULT_URI` in `.env`. |
| `Schema-validation` errors at startup                  | An entity drifted from the migration. Wipe DB: `docker compose down -v` and try again. |
| `vault kv get` says `No value found`                   | The app hasn't booted yet, or the path is wrong. Defaults to `secret/backend/jwt`.   |
| Swagger UI shows 401                                   | Already permitted in [SecurityConfig](src/main/java/ru/rxyvea/backend/security/SecurityConfig.java); ensure your tweak hasn't reverted it. |
| Generated sources missing in IDE                       | Run `./gradlew openApiGenerate` once, then refresh Gradle.                           |

## What lives where (defense crib sheet)

- **Vault integration**: [VaultConfig.java](src/main/java/ru/rxyvea/backend/security/vault/VaultConfig.java), [VaultJwtKeys.java](src/main/java/ru/rxyvea/backend/security/vault/VaultJwtKeys.java), [VaultProperties.java](src/main/java/ru/rxyvea/backend/security/vault/VaultProperties.java).
- **JWT key consumers**: [JwtService.java](src/main/java/ru/rxyvea/backend/security/JwtService.java) — receives `ECPublicKey` / `ECPrivateKey` beans wired by `VaultJwtKeys`.
- **One-to-many ledger**: [Transaction.java](src/main/java/ru/rxyvea/backend/model/Transaction.java) (parent) and [TransactionEntry.java](src/main/java/ru/rxyvea/backend/model/TransactionEntry.java) (child, `@ManyToOne`).
- **Projections**: [UserView.java](src/main/java/ru/rxyvea/backend/repository/projection/UserView.java), [TransactionView.java](src/main/java/ru/rxyvea/backend/repository/projection/TransactionView.java).
- **OpenAPI spec**: [api.yaml](src/main/resources/openapi/api.yaml). Generated server interfaces and DTOs land in `build/generated/openapi/src/main/java/...` after `./gradlew openApiGenerate`.
- **Controllers as generated-API implementations**: [AuthController](src/main/java/ru/rxyvea/backend/api/v1/auth/AuthController.java) `implements AuthApi`, [UserController](src/main/java/ru/rxyvea/backend/api/v1/users/UserController.java) `implements UsersApi`, [TransactionController](src/main/java/ru/rxyvea/backend/api/v1/transactions/TransactionController.java) `implements TransactionsApi`.
