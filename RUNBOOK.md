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

**Config that matters** — `resources/application.properties`:
- `spring.datasource.username=root`
- `spring.datasource.password=${DB_PASSWORD:}`   ← your MySQL root password is **not** stored in the repo (see below)
- URL includes `allowPublicKeyRetrieval=true` (required for MySQL 8 + `useSSL=false`)
- Database `bfsi_db` is auto-created on first boot

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
{ "username": "admin", "password": "admin123" }
```
Response: `{ "token": "eyJ...", "username": "admin", "roles": ["ROLE_ADMIN"], "expiresInMs": 3600000 }`

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
| POST | `/auth/login` | none | Returns JWT |
| GET | `/auth/me` | any | Echoes username + roles |
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

10 customers (Arjun, Priya, Karthik, Divya, Suresh, Meena, Rahul, Anjali, Vijay, Neha) across Bangalore/Mumbai/Chennai with 13 accounts (AC9001+) and 9 loans (HOME/CAR/PERSONAL). One extra row ("John") may exist from earlier runs — the seeder is additive, never destructive.

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
| Want a throwaway demo DB | — | `mvn spring-boot:run -Dspring-boot.run.profiles=h2` (in-memory, resets on restart) |

---

## 7. Tests

```bash
mvn test
```
18 tests in 3 classes, all against the in-memory H2 profile (no MySQL needed):
- `AccountServiceTest` — deposit/withdraw arithmetic (BigDecimal-exact), insufficient-balance rule, savings minimum, soft-close behavior
- `LoanEmiTest` — EMI math against independently computed reference values
- `ApiSecurityTest` — full HTTP stack: public health, JWT login, 401 without token, 403 for clerk deletes

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
