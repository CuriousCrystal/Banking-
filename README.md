# AMC Bank — BFSI Banking Application

A full-stack banking back office built as a Java Full-Stack training project: a **Spring Boot 3 REST API** secured with **JWT and role-based access**, plus a **React (Vite) admin dashboard**. It manages customers, accounts and loans for the fictional *AMC Bank*.

## Features

- **JWT login** — BCrypt-hashed passwords, stateless sessions, three roles: `ADMIN`, `OFFICER`, `USER`
- **Customers** — create, search / filter by city, view, update, delete; email and PAN must be unique and the PAN format is validated
- **Accounts** — open `SAVINGS` / `CURRENT` accounts (auto-numbered `AC…`), deposit, withdraw (overdrafts are rejected)
- **Loans** — sanction and list `HOME` / `CAR` / `PERSONAL` loans (officers and admins only); the customer must hold at least 10% of the loan amount across their accounts
- **User management** — admins create users, reset passwords and delete users
- **Dashboard** — live totals plus charts of loans by type and deposits by city
- **Consistent errors** — 401, 403 and validation failures all return the same JSON shape
- **Demo data** — three logins and ten sample customers (with accounts and loans) are seeded on first run

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.2.5 (Web, Data JPA, Validation, Security), JJWT 0.12 |
| Database | MySQL 8 (default) · H2 in-memory (`h2` profile, nothing to install) |
| Frontend | React 18, Vite 5, React Router 6, Axios, Recharts |
| Build | Maven 3.9+, npm (Node 18+) |

## Quick start

Prerequisites: **JDK 17+**, **Maven 3.9+**, **Node 18+** (MySQL 8 only if you skip the H2 profile).

```bash
git clone https://github.com/CuriousCrystal/Banking-.git
cd Banking-
```

**1. Start the backend** (http://localhost:8080) with one of:

```bash
# A) Zero setup — in-memory H2, data resets on every restart
mvn spring-boot:run "-Dspring-boot.run.profiles=h2"

# B) MySQL on localhost:3306 (user `root`; database `bfsi_db` is created automatically)
export DB_PASSWORD=your-mysql-password     # PowerShell: $env:DB_PASSWORD = "your-mysql-password"
mvn spring-boot:run
```

With option A the H2 console is at http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:bfsi`, user `sa`, empty password).

**2. Start the frontend** in a second terminal (http://localhost:5173):

```bash
npm install
npm run dev
```

Open http://localhost:5173 and sign in with a demo login. Quick API check: `curl http://localhost:8080/api/health`.

## Demo logins

Seeded on startup by `DataSeeder` (`app.seed.enabled=true`). **For local demos only** — change or disable them before deploying anywhere.

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | `ADMIN` |
| `officer` | `officer123` | `OFFICER` |
| `clerk` | `clerk123` | `USER` |

| Action | USER | OFFICER | ADMIN |
|---|:-:|:-:|:-:|
| View / create / update customers and accounts, deposit, withdraw | ✅ | ✅ | ✅ |
| View and sanction loans | ❌ | ✅ | ✅ |
| Delete customers and accounts | ❌ | ❌ | ✅ |
| Manage users | ❌ | ❌ | ✅ |

## API

Base URL `http://localhost:8080/api`. Log in first, then send `Authorization: Bearer <token>` on every other call (tokens last 1 hour).

| Method | Path | Access | Notes |
|---|---|---|---|
| GET | `/health` | public | |
| POST | `/auth/login` | public | `{username, password}` → JWT |
| GET | `/auth/me` | any | current user and roles |
| GET, POST | `/customers` | any | `?city=&search=` filters; body `{name, email, city, panNumber}` |
| GET, PUT | `/customers/{id}` | any | |
| DELETE | `/customers/{id}` | ADMIN | |
| GET, POST | `/accounts` | any | `?customerId=` filter; body `{customerId, accountType, balance}` |
| GET | `/accounts/{id}` | any | |
| PATCH | `/accounts/{id}/deposit`, `/accounts/{id}/withdraw` | any | `{amount}`; withdraw returns 400 when funds are insufficient |
| DELETE | `/accounts/{id}` | ADMIN | |
| GET, POST | `/loans` | OFFICER, ADMIN | body `{customerId, loanType, principalAmount, interestRate, tenureMonths}` |
| GET | `/loans/customer/{id}` | OFFICER, ADMIN | |
| GET, POST | `/users` | ADMIN | body `{username, password, role}` |
| PATCH | `/users/{id}/password` | ADMIN | `{password}` |
| DELETE | `/users/{id}` | ADMIN | |
| GET | `/reports/summary` | any | dashboard totals |

All errors share one shape — `{ "timestamp", "status", "error", "message", "errors": {} }` — with field-level validation problems inside `errors`. A Postman walk-through and troubleshooting table are in [RUNBOOK.md](RUNBOOK.md).

## Configuration

Backend settings live in `resources/application.properties`:

| Property | Default | Notes |
|---|---|---|
| `spring.datasource.password` | `${DB_PASSWORD:}` | Set the `DB_PASSWORD` env var, or put the property in a git-ignored `config/application.properties` |
| `app.jwt.secret` | dev placeholder | Base64 HMAC key — **replace it for anything beyond a local demo** |
| `app.jwt.expiration-ms` | `3600000` | Token lifetime (1 hour) |
| `app.cors.allowed-origins` | `http://localhost:[*]`, `http://127.0.0.1:[*]` | Any local port |
| `app.seed.enabled` | `true` | Seed demo users and customers on startup |

The frontend calls `http://localhost:8080/api`; override it with the `VITE_API_URL` environment variable.

## Project structure

```
.
├── java/com/amc/bfsi/       Spring Boot backend (non-standard source root, set in pom.xml)
│   ├── controller/ service/ repository/ entity/ dto/
│   ├── security/            JWT filter, access rules, user details
│   ├── exception/           global JSON error handling
│   ├── config/              DataSeeder (demo data)
│   └── collections/         core-Java practice demos (collections, generics, exceptions)
├── resources/               application.properties, application-h2.properties
├── src/                     React frontend (pages, components, API client, auth)
├── index.html, vite.config.js, package.json
├── pom.xml
├── RUNBOOK.md               full run guide, troubleshooting, Postman endpoint reference
└── BFSI_Project_Report.pdf  project report (source: report.html)
```

The backend keeps its sources in `java/` and `resources/` instead of `src/main/...`; `pom.xml` points Maven at them, so leave `<sourceDirectory>` and `<resources>` in place.
