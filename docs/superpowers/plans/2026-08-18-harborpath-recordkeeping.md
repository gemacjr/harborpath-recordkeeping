# Harborpath Recordkeeping Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a runnable Native Federation Angular shell with participant and sponsor remotes, backed by one Spring Boot API on Postgres, demoable in five minutes.

**Architecture:** Dynamic-host shell loads `participant` and `sponsor` remotes. One Java 21 Spring Boot bounded context owns auth, household, census, and compliance. Compose reverse-proxies `/api/` and `/remotes/*` through nginx in the shell image.

**Tech Stack:** Angular 19 standalone + `@angular-architects/native-federation`, Spring Boot 3.5, Java 21, Spring Security JWT, Spring Data JPA, Postgres 16, Testcontainers, Docker Compose, Nginx.

**Spec:** `docs/superpowers/specs/2026-08-18-harborpath-recordkeeping-design.md`

## Global Constraints

- Brand: Harborpath Recordkeeping. Never use Ascensus names, logos, or products.
- Java package: `com.harborpath.recordkeeping`. Angular: `@harborpath/*`.
- API base: `/api/v1`. Ports: Postgres 5432, API 8080, shell 4200, participant 4201, sponsor 4202.
- Demo users: `maya.chen` / `harborpath` (PARTICIPANT), `jordan.ellis` / `harborpath` (SPONSOR).
- Copy: login miss “That sign-in didn’t match our records.”; 403 “This chair isn’t yours.”
- Tokens: Carbon `#141414`, Bond `#F4F6F1`, Folder `#3A5344`, Tab `#D4C4A8`, Ribbon `#9B1D20`, Grid `#C5CDC2`. Type: Sora, Source Sans 3, IBM Plex Mono.
- Tests: Testcontainers Postgres only (no H2). No Cypress. Do not commit unless the user asks.

---

### Task 1: Backend API (TDD against Testcontainers)

**Files:**
- Create: `backend/` Spring Boot Maven project (web, security, data-jpa, validation, postgresql, actuator, springdoc, jjwt, testcontainers)
- Create: entities, repos, JWT filter, controllers, seed runner, `application.yml`, exception handler
- Test: `backend/src/test/java/com/harborpath/recordkeeping/ApiContractIT.java`

**Interfaces:**
- Consumes: Postgres via Testcontainers `@ServiceConnection`
- Produces: REST contract in the spec §7; JWT with claims `sub`, `role`, `uid`

- [ ] **Step 1:** Scaffold Spring Boot 3.5 / Java 21 from start.spring.io into `backend/`. Add jjwt, springdoc, testcontainers-postgresql.
- [ ] **Step 2:** Write failing `ApiContractIT` covering login success/failure, role isolation, deferral cap, deferral persistence into census.
- [ ] **Step 3:** Run tests; confirm they fail because endpoints are missing.
- [ ] **Step 4:** Implement packages `auth`, `plan`, `account`, `household`, `census`, `beneficiary`, `compliance`, `seed` until tests pass. Seed amounts from spec §6.
- [ ] **Step 5:** Re-run `./mvnw test`. Expected: PASS.

Key test cases (must exist):

```java
@Test void loginSuccessReturnsJwtAndRole() { /* POST /api/v1/auth/login maya.chen */ }
@Test void loginFailureReturns401() { /* bad password */ }
@Test void participantCannotReadCensus() { /* 403 */ }
@Test void sponsorCannotPatchDeferral() { /* 403 */ }
@Test void deferralAboveCapReturns422() { /* 51 vs cap 50 */ }
@Test void deferralAtCapSucceeds() { /* 50 */ }
@Test void deferralPersistsIntoCensus() { /* 7 -> 8, sponsor census shows 8 */ }
```

---

### Task 2: Angular workspace + Native Federation remotes

**Files:**
- Create: Angular 19 workspace at repo root, `newProjectRoot: apps`
- Create: apps `shell` (dynamic-host :4200), `participant` (remote :4201), `sponsor` (remote :4202)
- Create: `libs/shared` (tokens CSS, models, AuthSession)
- Create: login, chrome, 403, API-down banner, household/401k/529/beneficiaries, plan/census/alerts

**Interfaces:**
- Consumes: `/api/v1` as in Task 1; `loadRemoteModule({ remoteName, exposedModule: './App' })`
- Produces: Federated `./App` from each remote; `federation.manifest.json` for local and Compose

- [ ] **Step 1:** `ng new` workspace `--create-application=false`; generate three applications; `ng g @angular-architects/native-federation:init` for host + remotes. Expose `./App`.
- [ ] **Step 2:** Shared tokens + AuthSession (localStorage JWT). Shell login personas, guards, chrome.
- [ ] **Step 3:** Participant remote: household, deferral PATCH, tape, beneficiaries PUT.
- [ ] **Step 4:** Sponsor remote: plan header, census, acknowledge alert.
- [ ] **Step 5:** `ng build` all three projects.

---

### Task 3: Docker Compose + README

**Files:**
- Create: `backend/Dockerfile`, `apps/*/Dockerfile`, `apps/shell/nginx.conf`, `docker-compose.yml`, `README.md`, `.gitignore`

**Interfaces:**
- Consumes: built JARs and Angular dist
- Produces: `docker compose up --build` on `:4200` with `/api/` and `/remotes/{participant,sponsor}/`

- [ ] **Step 1:** Compose services `postgres`, `api`, `participant`, `sponsor`, `shell` with healthchecks.
- [ ] **Step 2:** Nginx: `/` → shell, `/api/` → api:8080, `/remotes/participant/` → participant:80, `/remotes/sponsor/` → sponsor:80. Compose federation manifest uses those paths.
- [ ] **Step 3:** README with mermaid diagram, demo users, local vs Compose, 5-minute walkthrough, interview talking points.

---

## Self-review

- Spec §2–13 mapped: Task 1 APIs/tests, Task 2 UI/MFE/visual, Task 3 Compose/README.
- No H2. No Ascensus branding. Seed amounts and copy strings copied from spec.
