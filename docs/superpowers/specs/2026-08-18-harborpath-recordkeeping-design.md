# Harborpath Recordkeeping — Design Spec

Date: 2026-08-18  
Status: Approved in conversation; pending user review of this file  
Audience: Interview demo for a full-stack role at a retirement/education savings recordkeeper (Ascensus-shaped domain, original brand)

## 1. Problem

Build a runnable Angular micro-frontend + Spring Boot system that a candidate can walk through in 5–10 minutes. It must show:

- Independently built Angular remotes loaded by a host shell (Native Federation)
- One Spring Boot API with JWT auth, Postgres, and role isolation
- Dual chairs on the same plan: **participant** (saver) and **sponsor** (plan administrator)
- Domain language of recordkeeping (401(k), 529, deferral, census, beneficiaries, plan-year compliance), not a generic brokerage or private bank

This is not an Ascensus clone. No Ascensus logos, product names (READYSAVE, Unite, Aimee), or trademarks.

## 2. Product slice (in scope)

Fictional firm: **Harborpath Recordkeeping**.

Seed world:

- Plan: Harborpath Staff 401(k), plan year 2026
- Household: Maya Chen (participant) — 401(k) + one 529 for a dependent
- Sponsor: Jordan Ellis — sees the same plan’s census and a short compliance queue
- One additional census participant so the table is not a single row

Screens:

1. Login — two demo personas as cards (no self-registration)
2. Participant remote: household dashboard, 401(k) account (deferral editor), 529 account (read-mostly), beneficiaries (view + replace primary)
3. Sponsor remote: plan book (one plan), census table, compliance queue (acknowledge/dismiss)

Signature interaction: Maya changes deferral %; API persists it; the contribution tape updates; Jordan’s census shows the new rate.

## 3. Explicitly out of scope

- Real money movement, ACH, payroll files, market data
- Full ADP/ACP, 5500, or IRS testing engines (alerts are seeded records, not computed tax law)
- OAuth/SAML/OIDC, MFA, password reset
- Spring Cloud Gateway, Eureka, Kafka, multiple backend services
- Cypress/Playwright suite
- Mobile app
- Impersonation of Ascensus branding

## 4. Architecture

### 4.1 Repository layout

Monorepo at the workspace root:

```
apps/shell                 # Angular host (login, chrome, federation runtime)
apps/participant           # Angular remote
apps/sponsor               # Angular remote
libs/shared                # Design tokens (CSS), TS models, auth helpers used by frontends
backend/                   # Spring Boot 3, Java 21
docker-compose.yml         # postgres, api, shell, participant, sponsor
docs/                      # this spec, later the implementation plan
README.md
```

Package names:

- Java: `com.harborpath.recordkeeping`
- Angular: `@harborpath/shell`, `@harborpath/participant`, `@harborpath/sponsor`, `@harborpath/shared`

### 4.2 Runtime (local and Compose)

| Process        | Port | Notes                                      |
|----------------|------|--------------------------------------------|
| Postgres       | 5432 | Compose only                               |
| Spring Boot    | 8080 | `/api/v1`, OpenAPI at `/swagger-ui.html`   |
| Shell          | 4200 | Host; public entry                         |
| Participant    | 4201 | Remote entry                               |
| Sponsor        | 4202 | Remote entry                               |

Local dev: Angular CLI + `./mvnw spring-boot:run` with `SPRING_PROFILES_ACTIVE=local` (Postgres from Compose or a documented local instance).

Compose: one `docker compose up --build` brings up all five processes. **Nginx** in the shell image serves the host, reverse-proxies `/api/` to the API, and reverse-proxies `/remotes/participant/` and `/remotes/sponsor/` to the remote containers. The shell’s federation manifest uses those same paths in Compose and `http://localhost:4201` / `4202` in local `ng serve`.

### 4.3 Frontend composition

- **Native Federation** (esbuild-friendly). Webpack Module Federation is not used.
- Shell owns: login, session storage, top chrome, role-aware nav, remote outlets, 403 page, API-down banner.
- Participant remote exposes `./App` (or equivalent federated route module).
- Sponsor remote exposes `./App`.
- Shared lib is a **contract**: CSS custom properties, `User`, `Account`, `Plan`, `Alert` types, and a tiny `AuthSession` helper. It is not a dumping ground for feature UI.
- Shell route guards: `/participant/**` requires `PARTICIPANT`; `/sponsor/**` requires `SPONSOR`. Login lives at `/login`. Unauthenticated users redirect to `/login`.

### 4.4 Backend composition

Single bounded context, single deployable. Not a microservice farm.

Modules (packages, not separate JARs):

- `auth` — login, JWT issue/parse, current user
- `plan` — plan header
- `account` — 401(k) and 529 accounts, deferral update
- `household` — participant-facing aggregation
- `census` — sponsor-facing participant list
- `beneficiary`
- `compliance` — seeded alerts + acknowledge
- `seed` — idempotent CommandLineRunner

Persistence: **Postgres** in app and Compose. Integration tests use **Testcontainers Postgres**. No H2.

## 5. Auth and security

Demo users (seeded, bcrypt):

| Username      | Password     | Role          |
|---------------|--------------|---------------|
| maya.chen     | harborpath   | PARTICIPANT   |
| jordan.ellis  | harborpath   | SPONSOR       |

Flow:

1. `POST /api/v1/auth/login` `{ username, password }` → `{ accessToken, tokenType, expiresIn, role, displayName }`
2. Subsequent requests: `Authorization: Bearer <jwt>`
3. JWT claims: `sub` (username), `role`, `uid` (user id), 8-hour expiry (demo)
4. CORS: allow `http://localhost:4200`, `http://localhost:4201`, `http://localhost:4202` in local; Compose origins as configured
5. Participant may only read/update **their** household/accounts. Sponsor may read the seeded plan’s census and alerts. Cross-role calls return 403.

No refresh tokens in v1. Logout is client-side token discard.

## 6. Data model

```
User            id, username, passwordHash, displayName, role
Plan            id, name, planYear, deferralCapPercent (50), sponsorUserId
Participant     id, userId (nullable for census-only rows), planId, fullName, hireDate
Account         id, participantId, type (RETIREMENT_401K | EDUCATION_529), name, balance, ytdEmployee, ytdMatch, deferralPercent (401k only)
Beneficiary     id, accountId, fullName, relationship, percent, primaryFlag
ComplianceAlert id, planId, participantId, code, message, severity, status (OPEN | ACKNOWLEDGED)
Contribution is not a separate mutable table in v1; YTD figures live on Account. Changing deferral does not invent payroll history; it updates Account.deferralPercent and a derived “next paycheck” line on the tape.
```

Seed amounts (fixed, so the walkthrough is stable):

- Maya 401(k): balance `184220.16`, deferral `7.00`, YTD employee `8400.00`, YTD match `4200.00`
- Maya 529: balance `22150.00`, name tied to dependent “Avery Chen”
- Second census person: Priya Shah, deferral `4.00`, missing primary beneficiary (OPEN alert)
- Maya: optional OPEN alert if we want one participant-visible notice; not required

## 7. API

Base path: `/api/v1`. JSON. Validation errors: 422 with `{ field, message }[]`.

| Method | Path | Role | Purpose |
|--------|------|------|---------|
| POST   | `/auth/login` | public | Issue JWT |
| GET    | `/me` | any authenticated | Current user |
| GET    | `/households/me` | PARTICIPANT | Maya’s accounts + tape totals |
| GET    | `/accounts/{id}` | PARTICIPANT (owner) | Account detail |
| PATCH  | `/accounts/{id}/deferral` | PARTICIPANT (owner, 401k only) | `{ percent }` 0–plan cap |
| GET    | `/accounts/{id}/beneficiaries` | PARTICIPANT (owner) | List |
| PUT    | `/accounts/{id}/beneficiaries` | PARTICIPANT (owner) | Replace set; percents must sum to 100 |
| GET    | `/plans` | SPONSOR | Plan book |
| GET    | `/plans/{id}/census` | SPONSOR | Rows: name, deferral, balance, beneficiary complete? |
| GET    | `/plans/{id}/alerts` | SPONSOR | Compliance queue |
| PATCH  | `/alerts/{id}` | SPONSOR | `{ status: ACKNOWLEDGED }` |

Deferral rules: 401(k) only; `percent` in `[0, plan.deferralCapPercent]`; 529 PATCH returns 422.

## 8. UI structure

### Shell

- Wordmark **Harborpath** with small “Recordkeeping” in Source Sans 3
- Right: session chip `Maya Chen · Participant` or `Jordan Ellis · Sponsor`, Sign out
- API-down banner when `/me` or the first remote call fails at network level
- 403 page copy: “This chair isn’t yours.”
- Login miss copy: “That sign-in didn’t match our records.”

### Participant

- Household: two account cards (401(k), 529), combined tape of YTD savings
- 401(k): balance, deferral stepper/input, save; tape shows YTD employee/match and “next paycheck at X%”
- 529: balance, owner/beneficiary label, no deferral control
- Beneficiaries: table; save replaces the set

### Sponsor

- Plan header: name, plan year, participant count, open alert count
- Census table: name, deferral %, balance, beneficiary status
- Queue: seeded alerts; Acknowledge moves status

Empty census copy: “No participants loaded for this plan year.”

## 9. Visual system

Ground: contribution files, hanging-folder green, carbon-copy perforation — a TPA shop, not a hedge fund.

| Token    | Hex       | Use |
|----------|-----------|-----|
| Carbon   | `#141414` | Text, chrome |
| Bond     | `#F4F6F1` | Page background |
| Folder   | `#3A5344` | Primary actions, sponsor accent |
| Tab      | `#D4C4A8` | Chips, folder-tab headers |
| Ribbon   | `#9B1D20` | Exceptions only |
| Grid     | `#C5CDC2` | Rules, table lines |

Type:

- Display/wordmark: **Sora**
- UI: **Source Sans 3**
- Data (balances, percents, tape): **IBM Plex Mono**

Signature:

- **Contribution tape** on account/household screens: fixed-width YTD employee/match ledger. Deferral save updates the “next paycheck” line after the PATCH succeeds.
- Light **vertical perforation** on dashboards (repeating dots), restrained, `prefers-reduced-motion` respected.

Not: navy/gold wealth, acid-green dark mode, terracotta cream serif landing page.

## 10. Error handling

| Case | Behavior |
|------|----------|
| Bad credentials | 401 + login field error |
| Missing/invalid JWT | 401; shell clears session, routes to login |
| Wrong role or not owner | 403 + “This chair isn’t yours.” |
| Deferral out of range / 529 | 422 field error |
| Beneficiaries not 100% | 422 |
| Unknown id | 404 |
| API unreachable | Shell banner; no fabricated balances |
| Empty census | Empty state copy above |

## 11. Testing

Backend (must exist):

1. Login success and failure
2. Participant cannot `GET /plans/{id}/census` (403)
3. Sponsor cannot `PATCH /accounts/{mayaAccount}/deferral` (403)
4. Deferral above cap → 422; at cap → 200
5. Deferral persists and census reflects new percent

Frontend: no E2E suite in v1. Manual script in README (5 minutes):

1. `docker compose up --build`
2. Sign in as Maya; note 7% deferral
3. Change to 8%; tape updates
4. Sign out; sign in as Jordan; census shows Maya at 8%
5. Acknowledge Priya’s missing-beneficiary alert

Compose: healthchecks on Postgres and API.

## 12. Docker and README

`docker-compose.yml` services: `postgres`, `api`, `shell`, `participant`, `sponsor`.

README must include:

- What this is (interview demo, domain mapping, not an official product)
- Architecture diagram in mermaid (shell → remotes → API → Postgres)
- Prerequisites (Docker, or Node 22 + JDK 21 + Postgres)
- Local vs Compose commands
- Demo users
- 5-minute walkthrough
- Talking points: Native Federation vs one SPA, role isolation at API not just UI, why one backend, what you’d add next (SAML, payroll import, real compliance engine)

## 13. Success criteria

Done when:

- `docker compose up --build` yields a working dual-persona demo
- Deferral change is visible across chairs without refreshing seed
- JWT role isolation is proven by tests
- UI follows the visual tokens in this spec
- README is enough for a stranger to run and narrate the demo
