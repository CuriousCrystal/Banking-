# AMC Bank — multi-tenant banking SaaS

A banking back office that many banks can share: every bank (tenant) signs up itself, gets its own users, customers, accounts and loans that no other bank can see, and runs on a subscription plan. A **Spring Boot 3 REST API** (JWT, roles, Flyway migrations) with a **React (Vite) dashboard**, packaged with Docker and built by CI.

## Features

**Platform**
- **Multi-tenant** — one deployment, many banks. Every business row carries a `tenant_id`; Hibernate filters every query by it, so isolation does not depend on each query remembering a `WHERE` clause. A test suite tries to break it (lists, guessed ids, related resources)
- **Self-service onboarding** — *Register your bank* creates the bank and its first admin and signs them in
- **Subscription plans** — `FREE` (25 customers, 3 users), `PRO`, `ENTERPRISE`; going over a limit answers `402 Payment Required`. A bank can be suspended, which locks out its users at once
- **Production ready** — env-only configuration (no secrets in the repo, start-up fails without them), Flyway migrations, health probes, Prometheus metrics, OpenAPI docs, graceful shutdown, Docker Compose stack, CI

**Banking**
- **Customers** — create, search / filter by city, update, delete; email is unique per bank and the PAN format is validated
- **Accounts** — `SAVINGS` (₹1,000 minimum) and `CURRENT`; deposit, withdraw (never below zero), soft-close (status `CLOSED`, row kept for audit); optimistic locking rejects lost updates with `409`
- **Loans** — `HOME` / `CAR` / `PERSONAL` for officers and admins: at most 3 per customer, and the customer must hold at least 10% of the amount; exact `BigDecimal` EMI breakdown
- **Users and roles** — `ADMIN`, `OFFICER`, `USER`; BCrypt passwords, JWT sessions, per-role access
- **Dashboard** — live totals, charts, and how much of the plan is used

**Safety**
- Login and signup are rate limited per client IP (`429`); unknown bank, unknown user and wrong password are indistinguishable
- Unexpected errors return only a reference id — the details stay in the server log
- Every failure uses one JSON error shape

## How the multi-tenancy works

```
Browser ──► nginx (UI + /api proxy) ──► Spring Boot ──► MySQL
                                          │
   JWT carries the bank code ─► JwtAuthFilter sets TenantContext ─► Hibernate adds
   "tenant_id = ?" to every query and stamps it on every insert
```

- **Shared schema, `tenant_id` discriminator** on `app_user`, `customer`, `account` and `loan`; uniqueness (email, username, account number) is *per bank*
- The **JWT** carries the bank code (`tid`); the filter also refuses tokens of unknown or suspended banks
- Hibernate's tenant filter does not cover `em.find()`, so `TenantSafeRepository` routes `findById` through a query, and `TenantScoped` fails closed (exception, not leak) if a row of another bank is ever loaded
- **No tenant in scope** means the reserved id `-`: reads return nothing and inserts fail the foreign key — code that forgets the tenant breaks loudly instead of leaking

## Quick start

Prerequisites: **JDK 17+** and **Maven 3.9+** (backend), **Node 18+** (frontend); **Docker** for the full stack; MySQL 8 only for the `dev` profile.

```bash
git clone https://github.com/CuriousCrystal/Banking-.git
cd Banking-
```

### A) The whole stack with Docker (production-like)

```bash
cp .env.example .env          # then set MYSQL_ROOT_PASSWORD, DB_PASSWORD and JWT_SECRET (openssl rand -base64 48)
docker compose up --build
```

Open http://localhost:8080 (change it with `HTTP_PORT`) and click **Register your bank**. Nothing is pre-seeded in this mode. Only the web UI is published; the API and the health/metrics port stay on the internal network.

### B) Local development, no database (H2)

```bash
mvn spring-boot:run "-Dspring-boot.run.profiles=h2"      # API on http://localhost:8080
npm install && npm run dev                               # UI on http://localhost:5173 (second terminal)
```

Sign in as bank code `amc-bank`, user `admin` / `admin123` (the code is pre-filled in dev builds), or register your own bank. Data resets on every restart. API docs: http://localhost:8080/swagger-ui.html · H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:bfsi`, user `sa`, empty password).

### C) Local development with MySQL

```bash
export DB_PASSWORD=your-mysql-password     # PowerShell: $env:DB_PASSWORD = "your-mysql-password"
mvn spring-boot:run                        # profile "dev": MySQL on localhost:3306, database bfsi_saas
```

Flyway creates the database's tables on first start. Instead of the environment variable you can put `spring.datasource.password=...` in a git-ignored `config/application.properties`.

### Tests

```bash
mvn test        # in-memory H2, no database needed - includes the tenant-isolation tests
```

## Demo bank (dev and h2 profiles only)

`DataSeeder` creates the bank `amc-bank` (ENTERPRISE plan) with three logins and ten sample customers. **Never enabled in production** (`app.seed.enabled=false`).

| Bank code | Username | Password | Role |
|---|---|---|---|
| `amc-bank` | `admin` | `admin123` | `ADMIN` |
| `amc-bank` | `officer` | `officer123` | `OFFICER` |
| `amc-bank` | `clerk` | `clerk123` | `USER` |

| Action | USER | OFFICER | ADMIN |
|---|:-:|:-:|:-:|
| View / create / update customers and accounts, deposit, withdraw | ✅ | ✅ | ✅ |
| View and sanction loans | ❌ | ✅ | ✅ |
| Delete customers, close accounts | ❌ | ❌ | ✅ |
| Manage users | ❌ | ❌ | ✅ |

## API

Base URL `/api`. Interactive docs at `/swagger-ui.html` (dev and h2). Sign up or log in, then send `Authorization: Bearer <token>` on every other call (tokens last 1 hour).

| Method | Path | Access | Notes |
|---|---|---|---|
| GET | `/health` | public | |
| POST | `/auth/signup` | public | `{bankName, bankCode, username, password}` → new bank on the FREE plan + its ADMIN, returns a JWT (`201`) |
| POST | `/auth/login` | public | `{tenant, username, password}` → JWT. `tenant` is the bank code (optional only in dev/h2) |
| GET | `/auth/me` | any | current user, roles and bank |
| GET | `/tenant` | any | the bank: plan, limits and usage |
| GET, POST | `/customers` | any | `?city=&search=` filters; body `{name, email, city, panNumber}` |
| GET, PUT | `/customers/{id}` | any | |
| DELETE | `/customers/{id}` | ADMIN | only when no account holds a balance |
| GET, POST | `/accounts` | any | `?customerId=` and `?status=ACTIVE\|FROZEN\|CLOSED`; body `{customerId, accountType, balance}` |
| GET | `/accounts/{id}` | any | |
| PATCH | `/accounts/{id}/deposit`, `/accounts/{id}/withdraw` | any | `{amount}`; `422` for insufficient funds or a closed account |
| DELETE | `/accounts/{id}` | ADMIN | soft-close, zero balance only |
| GET, POST | `/loans` | OFFICER, ADMIN | body `{customerId, loanType, principalAmount, interestRate, tenureMonths}` |
| GET | `/loans/customer/{id}`, `/loans/{id}/emi` | OFFICER, ADMIN | EMI breakdown |
| GET, POST | `/users` | ADMIN | body `{username, password, role}` |
| PATCH | `/users/{id}/password` | ADMIN | `{password}` |
| DELETE | `/users/{id}` | ADMIN | |
| GET | `/reports/summary` | any | dashboard totals |

Errors share one shape — `{ "timestamp", "status", "error", "message", "errors": {} }` — with field-level problems inside `errors`. Notable statuses: `401` (no/expired token, bad login), `402` (plan limit), `403` (role), `409` (duplicate or concurrent update), `422` (business rule), `429` (too many login attempts). More detail and a troubleshooting table: [RUNBOOK.md](RUNBOOK.md).

## Configuration

Spring profiles pick the environment; `mvn spring-boot:run` with no profile means `dev`.

| Profile | For | Database | Secrets |
|---|---|---|---|
| `dev` (default) | local development | MySQL `bfsi_saas` on localhost | dev-only JWT key, demo data, API docs on |
| `h2` | zero-setup demo and every test | in-memory H2 (MySQL mode), same Flyway migrations | dev-only JWT key, demo data |
| `prod` | containers, servers | MySQL from the environment | **all from environment variables; the app refuses to start without them** |

Production environment variables:

| Variable | Required | Meaning |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | yes | JDBC connection |
| `JWT_SECRET` | yes | base64 HMAC key, at least 32 bytes (`openssl rand -base64 48`) |
| `CORS_ALLOWED_ORIGINS` | no | only if the UI is on another origin than `/api` (the bundled nginx makes them the same) |
| `MANAGEMENT_PORT` | no (8081) | internal port for health probes and metrics — do not publish it |
| `DB_POOL_SIZE` | no (10) | connection pool size |

Other knobs in `resources/application.properties`: `app.jwt.expiration-ms`, `app.auth.rate-limit-per-minute` (30), `app.seed.enabled`, `app.tenancy.default-slug`. Frontend build-time variables: `VITE_API_URL`, `VITE_APP_NAME`, `VITE_DEFAULT_BANK_CODE`.

## Operations

- **Probes**: `GET :8081/actuator/health/liveness` and `/readiness` (prod). The Docker image has a `HEALTHCHECK` on the liveness probe
- **Metrics**: `GET :8081/actuator/prometheus` (Micrometer) — scrape it from inside the network
- **Schema**: Flyway migrations in `resources/db/migration`; Hibernate only *validates* the schema (`ddl-auto=validate`). Change the schema by adding `V2__….sql`, never by editing `V1`
- **CI** (`.github/workflows/ci.yml`): backend tests on H2, a boot against a real MySQL 8 (proves migrations + schema validation there), frontend build, both Docker images

## Project structure

```
.
├── java/com/amc/bfsi/       Spring Boot backend (non-standard source root, set in pom.xml)
│   ├── controller/ service/ repository/ entity/ dto/
│   ├── tenant/              TenantContext + Hibernate tenant resolver
│   ├── security/            JWT filter, rate limiter, access rules
│   ├── exception/           global JSON error handling
│   ├── config/              DataSeeder, JPA + OpenAPI config
│   └── collections/         core-Java practice demos (collections, generics, exceptions)
├── resources/               application[-dev|-h2|-prod].properties, db/migration (Flyway)
├── src/                     React frontend (pages, components, API client, auth)
│   └── test/java/           backend tests (isolation, signup + plans, hardening, security, services)
├── deploy/                  nginx.conf, frontend.Dockerfile
├── Dockerfile, docker-compose.yml, .env.example
├── .github/workflows/       CI
├── pom.xml, package.json, vite.config.js, index.html
├── RUNBOOK.md               run guide, troubleshooting, Postman endpoint reference
└── BFSI_Project_Report.pdf  project report (source: report.html)
```

The backend keeps its sources in `java/` and `resources/` instead of `src/main/...`; `pom.xml` points Maven at them, so leave `<sourceDirectory>` and `<resources>` in place.

## Roadmap — what a paid launch still needs

Deliberately not built yet, in the order I would do them:

1. **Billing** — Stripe Checkout + a webhook that sets `tenant.plan` (the plan limits already exist)
2. **Platform admin** — a separate role/console to list, suspend and re-plan banks (today: change `tenant.status` / `tenant.plan` in the database)
3. **Audit trail** — who did what per bank (logins, deposits, loan sanctions) — basic compliance for banking
4. **Sessions** — refresh tokens or httpOnly-cookie sessions, SSO (OIDC), password-reset e-mail
5. **Scale** — pagination on list endpoints, a shared rate-limit store (Redis) before running several replicas, a short cache for the per-request tenant lookup
6. **Operations** — structured (JSON) logs with a request id, backups and a restore drill, per-bank custom domains and branding
