# AMC Bank (BFSI) — Execution Runbook

Full-stack app: **Spring Boot 3.2.5 backend** (port 8080, MySQL, JWT security) + **React/Vite frontend** (port 5173).
All steps below were verified working on this machine on 2026-09-30.

---

## 0. Prerequisites (already installed)

| Tool | Check command | This machine |
|---|---|---|
| JDK 17+ | `java -version` | Java 25 (works fine; project targets 17) |
| Maven 3.9+ | `mvn -version` | 3.9.11 |
| Node 18+ | `node -v` | 24.20.0 |
| MySQL 8 | `sc query MySQL80` | Service `MySQL80`, RUNNING, port 3306 |

**Config that matters** — Spring profiles. `mvn spring-boot:run` without a profile means **`dev`** (`resources/application-dev.properties`):
- MySQL on `localhost:3306`, user `root`, database **`bfsi_saas`** — Flyway creates the tables on first boot. The older single-tenant `bfsi_db` is left untouched
- `spring.datasource.password=${DB_PASSWORD:}`   ← your MySQL root password is **not** stored in the repo (see below)
- URL includes `allowPublicKeyRetrieval=true` (required for MySQL 8 + `useSSL=false`)
- The demo bank `amc-bank` with three logins and ten customers is seeded (dev and h2 profiles only — never in `prod`)

**Supplying the MySQL password** (never commit it) — pick one:
- Create `config/application.properties` in the project root (git-ignored; Spring Boot loads it on top of the packaged file when the app is started from the project root):
  ```properties
  spring.datasource.password=YOUR_MYSQL_PASSWORD
  ```
- Or set the `DB_PASSWORD` environment variable before starting the backend (`$env:DB_PASSWORD="..."` in PowerShell, `export DB_PASSWORD=...` in bash).

---

## 1. One-time setup (only if starting fresh)

1. Start MySQL: `net start MySQL80` (usually already running as a Windows service)
2. Backend deps: `mvn clean compile`
3. Frontend deps: `npm install`

---

## 2. Daily run procedure

### Backend (port 8080)
```bash
mvn spring-boot:run
```
Wait for `Started BfsiApplication`, then check:
```bash
curl http://localhost:8080/api/health
# → {"status":"UP","bank":"AMC Bank","branch":"1102",...}
```
On boot the `DataSeeder` creates three demo logins and 10 sample customers (it never overwrites existing rows).

### Frontend (port 5173)
```bash
npm run dev
```
Open **http://localhost:5173** → sign in → dashboard shows live figures from MySQL.

### Demo logins

Bank code **`amc-bank`** (pre-filled on the dev login page; in dev/h2 the API assumes it when `tenant` is omitted). You can also click **Register your bank** to create a bank of your own.

| Username | Password | Role | Can do |
|---|---|---|---|
| `admin` | `admin123` | ROLE_ADMIN | Everything, incl. DELETE and user management |
| `officer` | `officer123` | ROLE_OFFICER | Loans (sanction), view/manage customers & accounts |
| `clerk` | `clerk123` | ROLE_USER | Read + create; **no** delete, **no** loans page |

JWT expires after **1 hour** (`app.jwt.expiration-ms=3600000`); the UI auto-redirects to login.

### Stop everything
- Backend: `Ctrl+C` in its terminal, or `taskkill //F //PID <pid>` where pid = `netstat -ano | findstr :8080`
- Frontend: `Ctrl+C`, or kill the pid on port 5173

---

## 3. Running from Eclipse

1. **File → Import → Maven → Existing Maven Projects** → select this folder (`D:\Projects\App\day08 - E2E Project`)
2. Wait for the Maven import + "Building workspace" to finish
3. Right-click `BfsiApplication.java` → **Run As → Spring Boot App** (backend on 8080)
4. The project uses a **non-standard layout** — source folder is `java/` and resources are in `resources/` (already wired in `pom.xml` via `<sourceDirectory>` and `<resources>`). Eclipse handles this automatically from the pom.
5. Frontend: run `npm run dev` from a terminal (Eclipse has no Vite integration)
6. If Eclipse shows stale classes after a pom change: right-click project → **Maven → Update Project** (Alt+F5)

> Note: Eclipse's bundled Java (JustJ 25) and the shell's Java 25 both work; no JDK switch needed.

---

## 4. Postman testing

Base URL: `http://localhost:8080/api`

### 4.1 Login first
`POST /auth/login`
```json
{ "tenant": "amc-bank", "username": "admin", "password": "admin123" }
```
`tenant` is the bank code; in dev/h2 it defaults to `amc-bank` when omitted, in `prod` it is required.
Response: `{ "token": "eyJ...", "username": "admin", "roles": ["ROLE_ADMIN"], "expiresInMs": 3600000, "tenant": { "id": "amc-bank", "name": "AMC Bank", "plan": "ENTERPRISE", "status": "ACTIVE" } }`

To create your own bank instead: `POST /auth/signup` with `{ "bankName": "My Bank", "bankCode": "my-bank", "username": "owner", "password": "at-least-8-chars" }` → `201` and the same response shape (FREE plan, the new user is its ADMIN).

### 4.2 Authenticated requests
Set header on every other request:
```
Authorization: Bearer <token>
```
(Tip: in Postman, paste the token into an environment variable and reference `{{token}}`.)

### 4.3 Endpoint reference (all verified)

| Method | Path | Min role | Notes |
|---|---|---|---|
| GET | `/health` | none | No token needed |
| POST | `/auth/login` | none | `{tenant, username, password}` → JWT; **429** after too many attempts from one IP |
| POST | `/auth/signup` | none | `{bankName, bankCode, username, password}` → new bank (FREE plan) + its ADMIN; returns JWT |
| GET | `/auth/me` | any | Username, roles and bank (401 without a token) |
| GET | `/tenant` | any | The bank: plan, limits (`maxCustomers`, `maxUsers`; absent = unlimited) and usage |
| GET | `/customers` | any (logged in) | `?city=Mumbai&search=ar` filters |
| GET | `/customers/{id}` | any | 404 if missing |
| POST | `/customers` | any | Validated body: name, email, city, panNumber (unique) |
| PUT | `/customers/{id}` | any | Update |
| DELETE | `/customers/{id}` | **ADMIN** | Clerk/officer → 403 |
| GET | `/accounts` | any | `?customerId=` and `?status=ACTIVE\|FROZEN\|CLOSED` filters |
| POST | `/accounts` | any | `{customerId, accountType, balance}` |
| PATCH | `/accounts/{id}/deposit` | any | `{amount}` |
| PATCH | `/accounts/{id}/withdraw` | any | `{amount}` — 422 if insufficient balance (all account types) |
| DELETE | `/accounts/{id}` | **ADMIN** | **Soft-close** — sets status CLOSED, row is kept for audit; only zero-balance accounts |
| GET | `/loans` | OFFICER/ADMIN | Clerk → 403 |
| POST | `/loans` | OFFICER/ADMIN | `{customerId, loanType, principalAmount, interestRate, tenureMonths}` |
| GET | `/loans/customer/{id}` | OFFICER/ADMIN | |
| GET | `/loans/{id}/emi` | OFFICER/ADMIN | EMI breakdown: `monthlyEmi`, `totalPayable`, `totalInterest` (BigDecimal-exact) |
| GET | `/users` | **ADMIN** | |
| POST | `/users` | **ADMIN** | `{username, password, role}` |
| PATCH | `/users/{id}/password` | **ADMIN** | `{password}` |
| DELETE | `/users/{id}` | **ADMIN** | |
| GET | `/reports/summary` | any | Dashboard figures |

Expected error shape (all endpoints):
```json
{ "timestamp": "...", "status": 401, "error": "Unauthorized", "message": "...", "errors": {} }
```
Validation failures put field errors inside `errors`.

---

## 5. Seeded demo data

In the demo bank `amc-bank` (ENTERPRISE plan): 10 customers (Arjun, Priya, Karthik, Divya, Suresh, Meena, Rahul, Anjali, Vijay, Neha) across Bangalore/Mumbai/Chennai with 13 accounts (AC9001+) and 9 loans (HOME/CAR/PERSONAL). One extra row ("John") may exist from earlier runs — the seeder is additive, never destructive.

H2 console (only when running the `h2` profile): http://localhost:8080/h2-console, JDBC URL `jdbc:h2:mem:bfsi`, user `sa`, empty password.

---

## 6. Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `No sources to compile` / `Unable to find a suitable main class` | Maven can't find sources (non-standard layout) | Fixed in `pom.xml` (`<sourceDirectory>java</sourceDirectory>`). Don't remove it. |
| `Public Key Retrieval is not allowed` | MySQL 8 auth handshake | `allowPublicKeyRetrieval=true` in the JDBC URL (already added) |
| `Access denied for user 'root'@'localhost'` | Wrong or missing MySQL password | Set `DB_PASSWORD`, or fix `spring.datasource.password` in `config/application.properties` |
| `Port 8080 was already in use` | Old backend still running | `netstat -ano \| findstr :8080` → `taskkill //F //PID <pid>` |
| 409 on a deposit/withdraw | Two requests updated the same account concurrently | Reload and retry — optimistic locking working as designed |
| Deposit to a closed account → 422 | Soft-closed accounts reject transactions | Expected behavior (was impossible before the status column) |
| UI shows blank page / `React is not defined` in console | Missing `vite.config.js` | Fixed — the file now exists; restart `npm run dev` |
| `ERR_CONNECTION` from UI | Backend not running | Start backend first, then refresh |
| 401 on every API call after an hour | JWT expired | Log in again (UI does this automatically) |
| Want a throwaway demo DB | — | `mvn spring-boot:run "-Dspring-boot.run.profiles=h2"` (in-memory, resets on restart) |
| 401 "Invalid bank code, username or password" | Wrong bank code, user or password — they all look the same on purpose | Check the bank code (required in `prod`; `amc-bank` for the demo) |
| 402 when creating a customer or user | The bank's plan limit is reached (FREE: 25 customers, 3 users) | Upgrade the plan (`tenant.plan`) |
| 429 on login or signup | Too many attempts from one IP in a minute | Wait a minute (`app.auth.rate-limit-per-minute`) |
| `Found non-empty schema(s) … but no schema history table` | The new build was pointed at an old Hibernate-created database such as `bfsi_db` | Use a fresh database — the default is `bfsi_saas` |
| `Schema-validation: wrong column type …` | An entity and the Flyway migration disagree | Fix the entity, or add a new `V2__….sql` — never edit V1. CI boots the app on MySQL to catch this |
| `Missing required environment variables for the prod profile: …` | `prod` started without its secrets | Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (README → Configuration) |

---

## 7. Tests

```bash
mvn test
```
36 tests in 9 classes, all against the in-memory H2 profile (no MySQL needed — Maven forces the `h2` profile for every test):
- `AccountServiceTest` — deposit/withdraw arithmetic (BigDecimal-exact), insufficient-balance rule, savings minimum, soft-close behavior
- `LoanEmiTest` — EMI math against independently computed reference values
- `ApiSecurityTest` — full HTTP stack: public health, JWT login, 401 without token, 403 for clerk deletes
- `TenantIsolationTest` — bank B cannot list, read, change or delete bank A's customers, accounts, loans or users, even by guessing ids
- `TenantSafetyNetTest` — a raw `em.find()` of another bank's row throws instead of leaking
- `SignupAndPlanTest` — onboarding, bank-code rules, FREE-plan customer and user limits (402)
- `AuthHardeningTest` — `/auth/me` without a token, indistinguishable 401s, suspended banks, 404/400 instead of 500
- `RateLimitTest` — the sixth login attempt in a minute is refused (429)
- `StartupSafetyTest` — `prod` refuses to start without its secrets, too-short JWT key stops the start-up, 500s do not leak internals

The H2 tests cannot see MySQL-only problems (native ENUM columns, for one), so CI also boots the built jar against a real MySQL 8.

Run after any backend change — it is the fastest way to catch regressions.

## 8. Hardening upgrade (2026-09-30, post-training)

- **Money is `BigDecimal`** everywhere (entities, DTOs, services) — no more floating-point rounding on balances/EMIs; JSON still arrives as numbers so the UI needed no format changes
- **Optimistic locking** — `@Version` on Account; a lost update race returns **409** with a retry message instead of silently corrupting a balance
- **Withdraw rule tightened** — no account type may go below zero (CURRENT previously could)
- **Accounts soft-close** — `account_status` column (ACTIVE/FROZEN/CLOSED); close keeps the row for audit; CLOSED accounts reject deposit/withdraw (422)
- **New account numbers** are generated pre-insert (`AC` + 6 digits, uniqueness-checked) — fixes a latent bug where the number was derived from the DB id after insert
- **Loan EMI endpoint** — see table above; also surfaced in the Loans page via the "EMI breakdown" button

## 9. Files changed to make this run (2026-09-30)

- `pom.xml` — added `<sourceDirectory>java</sourceDirectory>` + `<resources>` pointing at `resources/`
- `resources/application.properties` — `allowPublicKeyRetrieval=true` in URL; password read from `DB_PASSWORD` / `config/application.properties`
- `vite.config.js` — **new**: loads `@vitejs/plugin-react` (without it JSX crashes with `React is not defined`)

## 10. SaaS upgrade (2026-09-30)

The single-bank app became a multi-tenant SaaS foundation — architecture and deployment are in the README.

- **Multi-tenancy** — new `tenant` table; `tenant_id` on users, customers, accounts and loans (Hibernate `@TenantId`); the JWT carries the bank code; login takes a bank code; usernames, e-mails and account numbers are unique *per bank*
- **Onboarding and plans** — `POST /auth/signup`, `GET /tenant`, FREE / PRO / ENTERPRISE limits (402 when exceeded); a suspended bank is locked out immediately
- **Schema under Flyway** — `resources/db/migration/V1__init_schema.sql`, Hibernate only validates it. The default database is now **`bfsi_saas`**; your old `bfsi_db` is untouched (a build that expects Flyway cannot adopt a Hibernate-created schema)
- **Profiles** — `dev` (default), `h2`, `prod`; production takes every secret from environment variables and refuses to start without them; the JWT key committed in the repo is dev-only
- **Ops** — health probes and Prometheus metrics (Actuator), OpenAPI at `/swagger-ui.html` (dev/h2), `Dockerfile`, `docker-compose.yml`, nginx config, GitHub Actions CI
- **Hardening** — rate limiting on login/signup, no exception text in 500 responses, `/auth/me` now needs a token (it used to be open to anonymous callers), Spring MVC client errors such as an unknown URL or a non-numeric id are answered 404/400 instead of falling into the catch-all 500, the H2 console exists only in the `h2` profile
- **Tests** — 36 (was 18). `ApiSecurityTest` used to run against the real MySQL; every test is now hermetic

Files: `pom.xml`, `resources/application*.properties`, `resources/db/migration/`, `java/…/{tenant,entity,repository,service,controller,security,exception,config}`, `src/{pages,components,api,config.js}`, `src/test/`, `Dockerfile`, `docker-compose.yml`, `deploy/`, `.env.example`, `.dockerignore`, `.github/workflows/ci.yml`.
