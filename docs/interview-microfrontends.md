# Harborpath micro-frontends — how they work, and what interviewers will ask

This document is a walkthrough of **this repo**, not a generic Module Federation essay. Use it to explain the architecture in an interview, then to answer follow-ups with concrete file names and trade-offs.

Harborpath is an Angular **19** monorepo using **Native Federation** (`@angular-architects/native-federation` 19) plus a single Spring Boot **3.5** API. The product slice is retirement/education **recordkeeping**: a participant chair (Maya Chen) and a plan-sponsor chair (Jordan Ellis) on the same 401(k) plan, with a shared ledger remote.

---

## 1. What “micro-frontend” means here

A micro-frontend (MFE) is an independently **built and deployed** UI that a **host** composes at **runtime**. It is not “folders in one Angular app.” If you cannot ship the participant UI without rebuilding the sponsor UI, you do not have MFEs — you have a modular monolith with extra ceremony.

Harborpath has four Angular applications:

| App | Role | Local port | Federated name |
|---|---|---|---|
| `apps/shell` | Host. Login, chrome, route guards, session. | 4200 | (host; no `exposes`) |
| `apps/participant` | Remote. Household, deferral, beneficiaries, loan request. | 4201 | `participant` |
| `apps/sponsor` | Remote. Plan book, census, compliance queue, loan desk. | 4202 | `sponsor` |
| `apps/contributions` | Remote. Shared ledger: payroll, 529 cash, contribution tape, audit tape. | 4203 | `contributions` |

`libs/shared` is **not** a fifth remote. It is a TypeScript path alias (`@harborpath/shared` → `libs/shared/index.ts`) compiled **into** each app. Interviewers often mix these up. Correct answer:

- **Federated remotes** = runtime JS loaded from `remoteEntry.json`.
- **Shared library** = compile-time contract (tokens, models, `AuthSession`, HTTP interceptor). Same source, copied into each bundle unless the packages it depends on (`@angular/core`, etc.) are deduplicated by federation sharing.

The backend is **one bounded context**, one JAR, one Postgres. Micro-frontends do **not** imply microservices. That is a talking point this codebase is designed to make.

```
Browser
  └── Shell (host)  :4200
        ├── loadRemoteModule('participant', './App')     → participant routes
        ├── loadRemoteModule('sponsor', './App')         → sponsor routes
        ├── loadRemoteModule('contributions', './App')   → ledger routes
        └── HTTP /api/v1  → Spring Boot :8080 → Postgres
```

---

## 2. Why Native Federation, not Webpack Module Federation

**Webpack Module Federation** (the original 2020 pattern) plugs a `ModuleFederationPlugin` into webpack. Angular 17+ defaulted to **esbuild / application builder**. Native Federation is the Angular Architects layer that gives you the *same composition model* on esbuild: a host, remotes, `exposes`, shared singletons, a `remoteEntry.json`.

What the browser actually uses is the **web platform**: ES modules, import maps, and `es-module-shims` (polyfill listed in `angular.json` polyfills). There is no webpack runtime in the production graph.

### What you should be able to say out loud

> “We used Native Federation because Angular 19’s application builder is esbuild. Webpack Module Federation would have forced a webpack custom builder. Native Federation still gives us independently built remotes and shared Angular singletons, but the runtime is import maps plus `remoteEntry.json`, not a webpack container.”

### Trade-offs (say these unprompted)

| | Native Federation | Webpack MF | One SPA with lazy routes |
|---|---|---|---|
| Independent deploy | Yes (separate `ng build`) | Yes | No — one pipeline |
| Shared Angular | `shareAll` singletons | `shared` config | Automatic (one copy) |
| Tooling | Extra builder + import maps | Mature webpack plugin | Simplest |
| Version skew | `strictVersion: true` fails loud | Same class of problem | Does not exist |
| Cost | Four builds, four containers, CORS/path issues | Same | Lowest |

Harborpath **pays** the MFE cost because the interview is about composition, team boundaries, and dual chairs — not because a 6-screen demo *needs* four deploys.

---

## 3. Build graph (what `ng build` actually does)

Each project in `angular.json` has **two** architect targets that matter:

1. **`esbuild`** — `@angular-devkit/build-angular:application`. Normal Angular application build.
2. **`build`** — `@angular-architects/native-federation:build`, with `"target": "<project>:esbuild:production"`.

The federation builder wraps the esbuild build: it reads `federation.config.js`, emits shared chunks, and writes **`remoteEntry.json`** (the catalog the host will fetch).

**Gotcha this repo hit:** `build.options` must include `target`. Do **not** put `"dev": true` on the **build** configuration. That makes the plugin treat a production build like `ng serve` and crash on `buildTarget.split`. Serve uses `"dev": true` with `target` pointing at `*:serve-original:development`.

Local scripts (`package.json`):

```bash
npm start                     # shell :4200
npm run start:participant     # :4201
npm run start:sponsor         # :4202
npm run start:contributions   # :4203
```

Four processes. The host does not compile the remotes into its bundle.

---

## 4. Remote contracts: `exposes` and `./App`

Each remote’s `federation.config.js` looks like this (`apps/participant/federation.config.js`):

```js
name: 'participant',
exposes: {
  './App': './apps/participant/src/app/app.routes.ts',
},
shared: {
  ...shareAll({ singleton: true, strictVersion: true, requiredVersion: 'auto' }),
},
```

**`name`** must match the key in the host’s federation manifest (`participant`, `sponsor`, `contributions`).

**`exposes`** is the public API of the remote. We expose **one** surface: `./App`, which is the **route table**, not a component and not the whole `bootstrapApplication` call.

Participant routes (`apps/participant/src/app/app.routes.ts`):

```ts
export const routes: Routes = [
  { path: '', component: HouseholdComponent },
  { path: 'accounts/:id', component: AccountComponent },
  { path: 'accounts/:id/beneficiaries', component: BeneficiariesComponent }
];
```

Those paths are **child** paths. The host mounts them under `/participant`, so the URL the user sees is `/participant/accounts/:id`. Same idea: `/sponsor`, `/ledger`.

The host loads that contract in `apps/shell/src/app/app.routes.ts`:

```ts
loadChildren: () =>
  loadRemoteModule('participant', './App').then((m) => m.routes)
```

`loadRemoteModule(remoteName, exposedKey)`:

1. Looks up `participant` in the manifest → URL of `remoteEntry.json`.
2. Fetches the entry; the entry lists the exposed modules and shared deps.
3. Downloads the chunk for `./App`.
4. Returns the ES module; we take `m.routes`.

If you exposed a component instead (`exposes: { './Household': '...' }`), the host would use `loadComponent`. Routes are the better contract for a **whole chair** (many screens, nested URLs).

### Standalone vs federated

Every remote still has `main.ts` → `initFederation()` → `bootstrap.ts`. That path is for **running the remote alone** on 4201/4202/4203 (dev / debugging). When the shell loads `./App`, it does **not** run the remote’s `bootstrapApplication`. It only imports the route module into the **shell’s** router and injector.

That is why chrome, guards, and the JWT interceptor that “just work” in the demo are owned by the **shell**. Remotes also register `tokenInterceptor` in their own `app.config.ts` so a standalone `ng serve participant` can call the API — but in Compose, users never hit :4201 in the browser.

---

## 5. Host boot sequence (the most important runtime story)

`apps/shell/src/main.ts`:

```ts
import { initFederation } from '@angular-architects/native-federation';

initFederation('federation.manifest.json')
  .catch(err => console.error(err))
  .then(_ => import('./bootstrap'))
```

Order is not optional:

1. **`initFederation(manifestUrl)`** fetches `federation.manifest.json` (a static file in `apps/shell/public/`).
2. That JSON maps remote **names** → **entry URLs**.
3. Native Federation installs an **import map** so shared packages (`@angular/core`, `rxjs`, …) resolve to one copy.
4. **Then** dynamic `import('./bootstrap')` runs `bootstrapApplication`. Angular must not boot before the import map exists, or you get two Angular runtimes (broken DI, “NG0203”, duplicate `AuthSession`).

Local manifest (`federation.manifest.json`):

```json
{
  "participant": "http://localhost:4201/remoteEntry.json",
  "sponsor": "http://localhost:4202/remoteEntry.json",
  "contributions": "http://localhost:4203/remoteEntry.json"
}
```

Docker manifest (`federation.manifest.docker.json`), copied **over** the local file in the shell image:

```json
{
  "participant": "/remotes/participant/remoteEntry.json",
  "sponsor": "/remotes/sponsor/remoteEntry.json",
  "contributions": "/remotes/contributions/remoteEntry.json"
}
```

Same-origin paths in Docker. Absolute `localhost:4201` URLs would be wrong inside Compose: the **browser** would try the user’s laptop ports, and the remote containers do not publish 4201–4203.

---

## 6. Shared dependencies vs `libs/shared`

### Federation `shared` (runtime)

```js
shared: {
  ...shareAll({ singleton: true, strictVersion: true, requiredVersion: 'auto' }),
}
```

- **`shareAll`** — treat almost every npm dependency as a candidate to share.
- **`singleton: true`** — host and remotes must use **one** instance of Angular, RxJS, etc. Two copies of `@angular/core` means two injectors; `providedIn: 'root'` `AuthSession` would not be the same object.
- **`strictVersion: true`** — version mismatch is a **hard fail** at load, not a subtle bug two days later.
- **`requiredVersion: 'auto'`** — read the version from this app’s `package.json`.

`skip` excludes RxJS entry points we never import (`rxjs/ajax`, `rxjs/webSocket`, …) so they are not forced into the shared graph.

### `libs/shared` (compile time)

Exported from `libs/shared/index.ts`:

- CSS design tokens (`tokens.css`) imported by each app’s `styles.css`
- Domain TypeScript types (`AccountSummary`, `LoanRow`, …)
- `AuthSession` (`providedIn: 'root'`, `localStorage` key `harborpath.session`)
- `tokenInterceptor` (attaches `Authorization: Bearer …`, treats HTTP status `0` as API-down, 401 clears session)
- Formatters (`money`, `pct`)

**Interview trap:** “Is `@harborpath/shared` a federated module?”  
**Answer:** No. It is a path mapping. What *is* federated is Angular itself, so the `AuthSession` singleton constructed in the shell is the same class/token remotes inject — **if** `@angular/core` is a singleton. If sharing broke, you would see logged-in chrome and unauthenticated API calls from a remote (two sessions).

---

## 7. Routing, guards, and who owns security

The shell owns **perimeter** UI security. The API owns **real** security.

Shell routes:

| Path | Guard | Remote |
|---|---|---|
| `/login`, `/forbidden` | none | shell components |
| `/participant/**` | `authGuard` + `roleGuard('PARTICIPANT')` | participant |
| `/sponsor/**` | `authGuard` + `roleGuard('SPONSOR')` | sponsor |
| `/ledger` | `authGuard` only (both chairs) | contributions |

`authGuard` sends anonymous users to `/login`. `roleGuard` sends the wrong chair to `/forbidden` (“This chair isn’t yours.”). Empty `''` redirects to `AuthSession.homePath()` (`/sponsor` or `/participant`).

**This is not authorization.** A determined client can still `GET /api/v1/plans/{id}/census` with a participant JWT. The API returns **403** (`participantCannotReadCensus` in `ApiContractTest`). Same for sponsor patching deferral, Maya posting payroll, Maya approving her own loan.

Say this sentence in the interview:

> “The shell guard is UX. JWT role checks on the API are the control. We proved both with contract tests.”

Session storage is **access token only**, 8-hour expiry, no refresh token. Logout is `AuthSession.clear()` (drop `localStorage`). Fine for a demo; say what you would add for production (httpOnly cookie or BFF, refresh rotation, CSP).

---

## 8. How HTTP works from a remote

Remotes call `/api/v1/...` with `HttpClient`. They do not know the host.

**Local `ng serve`:** `proxy.conf.json` on the shell (and remotes if served) forwards `/api` → `http://localhost:8080`. The browser origin is `http://localhost:4200`. Spring CORS allows 4200–4203.

**Docker:** the browser only talks to **:4200**. `apps/shell/nginx.conf`:

- `/api/` → `http://api:8080/api/` (Compose DNS name `api`)
- `/remotes/participant/` → `http://participant:80/`
- `/remotes/sponsor/` → `http://sponsor:80/`
- `/remotes/contributions/` → `http://contributions:80/`
- everything else → SPA `index.html` (`try_files`)

Remote nginx (`apps/remote.nginx.conf`) serves static files and sends `Access-Control-Allow-Origin *` so a local host on :4200 can fetch :4201 entries during `ng serve`. In Docker, remotes are **same-origin** via the `/remotes/...` proxy, so CORS is less critical — but the header is harmless.

**Restart pitfall:** shell nginx **resolves upstream hostnames at start**. If `api` is not in DNS yet, nginx exits (`host not found in upstream "api"`). Compose `depends_on: api: condition: service_healthy` is the fix. `docker compose restart` can still race; starting shell after API is healthy recovers.

The shell Dockerfile **overwrites** the baked local manifest:

```
COPY apps/shell/public/federation.manifest.docker.json \
     /usr/share/nginx/html/federation.manifest.json
```

Without that copy, a Compose shell would request `http://localhost:4201` from the user’s browser and federation would fail.

---

## 9. Team / bounded-context mapping (why three remotes)

MFEs should follow **team and domain** seams, not “one remote per page.”

| Remote | Domain language | Writes | Reads others’ writes |
|---|---|---|---|
| Participant | Household, deferral, beneficiaries, loan **request** | Deferral %, designations, 529 is on ledger, loan request | Census/alerts only via API as herself (mostly her household) |
| Sponsor | Plan book, census, compliance, loan **decision** | Acknowledge alerts, approve/deny loans, payroll is on ledger | Sees Maya’s deferral and beneficiary completeness |
| Contributions (Ledger) | Cash movement | Payroll, 529 contribute/withdraw, (loan **disbursement** is API-side on approve) | Shows the tape both chairs care about |

Cross-remote **stories** (this is the demo, and the interview narrative):

1. Maya sets deferral 8% (participant) → Jordan posts payroll (ledger) → Maya’s YTD and census rate match.
2. Maya unchecks primary beneficiary (participant) → Jordan’s queue opens `MISSING_PRIMARY_BENEFICIARY` (computed, not a fixture) → restoring primary **CLEARED**.
3. Maya requests a 401(k) loan (participant) → Jordan approves (sponsor) → Ledger shows `LOAN_DISBURSEMENT` −$5,000; balance drops **only then**.

If an interviewer asks “why not one remote?”, answer: participant vs sponsor are different **users, release trains, and compliance surfaces**. Ledger is the shared **cash** remote so we do not duplicate payroll UI in both chairs. If it grew, a servicing/ops chair would be a fourth remote, still one API.

---

## 10. What is *not* federated (and why that is correct)

- **Spring Boot** is not split into participant/sponsor services. Recordkeeping is one database, one transaction for “approve loan + debit + contribution row + audit.” Distributed transactions across MFEs would be a mistake.
- **No event bus between remotes.** They do not `window.postMessage` to each other. They **reload from the API**. That is the integration pattern: **shared system of record**, not shared UI state.
- **No Module Federation of `libs/shared`.** Sharing a company library as a remote is possible but adds versioning hell. A compile-time package (or a real npm package later) is enough at this size.

---

## 11. Failure modes you should mention before they ask

| Symptom | Cause | What we did / would do |
|---|---|---|
| Blank `/participant`, console 404 on `remoteEntry.json` | Manifest URL wrong; remote not up; Docker still using localhost:4201 | Two manifests; nginx `/remotes/...` |
| `NG0203` / duplicate Angular | `initFederation` after bootstrap, or sharing off | Boot order in `main.ts`; `singleton: true` |
| Logged in but API 401 from remote | Two `AuthSession` instances | Share `@angular/core` as singleton |
| Shell container `Exited (1)` | nginx resolved `api` too early | `depends_on` + healthcheck |
| Build: `buildTarget.split` | `"dev": true` on federation **build** | `target` only; `dev` only on serve |
| CORS errors local | Remote nginx missing ACAO | `apps/remote.nginx.conf` |
| Participant sees sponsor routes | Guard bug | `roleGuard`; API still 403 |

---

## 12. How to draw it on a whiteboard (60 seconds)

```
[Browser]
    |  document from :4200
    v
[Shell]  initFederation(manifest)
    |  import map (one Angular)
    |  loadRemoteModule(name, './App')
    v
[remoteEntry.json] --> [exposed routes chunk]
    |
    |  HttpClient + Bearer (shell interceptor)
    v
[Spring Boot /api/v1] --> [Postgres]
```

Then draw **one** vertical line: “UI composition = federation. Data composition = REST + JWT. Never confuse them.”

---

# Interview questions and how to answer them

Answers below are calibrated to **Harborpath**. Prefer a 45-second version, then offer depth. If you do not know, say what you would look up — do not invent webpack internals.

---

## A. Architecture and “why MFEs”

### 1. “What is a micro-frontend? Why not one Angular app?”

A deployable UI owned by a team, composed at runtime by a host. We split **participant / sponsor / ledger** because those are different chairs and release cadences in a recordkeeper, not because the screen count required it. A single SPA would have been simpler for a 10-minute demo; we used MFEs to show the pattern interviewers hire for.

### 2. “When are micro-frontends a bad idea?”

Small team, one product, one release train, no org boundary. Also: sharing a lot of fine-grained components (a federated button library) — that is a design system package, not an MFE. We federate **applications** (`./App` routes), not widgets.

### 3. “Did you use microservices because you used micro-frontends?”

No. Orthogonal. The API is one bounded context so “approve loan” is one transaction. Splitting UI teams does not force splitting the database. If the company later split “contributions” into its own service, the ledger remote would be the first consumer — we did not pretend that was required on day one.

### 4. “How do you choose remote boundaries?”

Around **users and capabilities**, not pages. Participant capabilities (elections, beneficiaries, loan request). Sponsor capabilities (census, compliance, loan decision). Ledger is the exception: **shared cash** used by both, so it is its own remote instead of duplicated or shoved into the shell.

### 5. “What does the shell own vs remotes?”

Shell: authentication UX, session, layout chrome, route guards, federation init, API-down banner, 403 page. Remotes: domain screens only. Shared lib: types and session helper — not feature UI.

---

## B. Native Federation mechanics

### 6. “How does the host find a remote?”

`initFederation('federation.manifest.json')` loads a name → `remoteEntry.json` URL map. `loadRemoteModule('participant', './App')` uses that name and the exposed key. The entry file is the catalog, not the whole remote.

### 7. “What is `remoteEntry.json`?”

The remote’s runtime manifest: exposed modules, shared dependency versions, and URLs of the JS chunks. The host never hardcodes chunk hashes; it asks the entry.

### 8. “What is an import map? Why `es-module-shims`?”

Import maps tell the browser “`@angular/core` means this URL.” Native Federation generates that so host and remotes share one Angular. `es-module-shims` polyfills import maps in browsers that need it; it is in the shell `polyfills`.

### 9. “Why `singleton` and `strictVersion`?”

Angular, the router, and `providedIn: 'root'` services must be one instance. `strictVersion` turns a silent double-bundle into a load-time error. In a real org you might `strictVersion: false` with a compatibility range — we chose fail-fast for a demo.

### 10. “Webpack Module Federation vs Native Federation?”

Same *idea* (host, remotes, shared). Different *bundler*. Webpack MF is a plugin in the webpack runtime. Native Federation targets esbuild/Vite-style builds and the native module system. Angular 19 → Native Federation is the default recommendation from Angular Architects.

### 11. “Could remotes be different Angular versions?”

In theory, with no singleton sharing — you get two frameworks, larger download, no shared DI. In practice for this shop: **same major, shared singleton**. We would not mix Angular 19 and 17 in one shell.

### 12. “What do you expose — components or routes?”

We expose **routes** (`./App` → `app.routes.ts`). A chair is a tree of screens. Exposing one component per page would make the host a god router. Alternative: expose a single `RemoteRootComponent` with its own child `RouterOutlet` — we flattened into host `loadChildren` instead, so URLs stay `/participant/...` in one router.

### 13. “Does each remote bootstrap Angular?”

When **served alone**, yes (`main.ts` → `bootstrap.ts`). When **loaded by the shell**, no: only the exposed module is imported. One `bootstrapApplication` (the shell).

### 14. “How do you version remotes independently?”

Build/deploy remote container independently; host fetches *current* `remoteEntry.json` at runtime. Breaking the `./App` export (rename `routes`, change path shape) is a **contract break** — version the exposed API or keep a compatibility export. We have one contract: `export const routes`.

### 15. “How do you test federation?”

Unit-test remote components in isolation. Contract-test the API. For composition: run all four `ng serve`s or Compose and click the walkthrough. We did not add Playwright; I would add one e2e that logs in as Maya, loads `/participant`, and asserts `remoteEntry` 200.

---

## C. State, auth, and communication

### 16. “How do remotes share state?”

They don’t share a global store. **Postgres is the shared state.** After Maya saves deferral, Jordan’s census **GETs** the plan. No `Subject` across remotes. Audit and contribution tables are the cross-cutting logs.

### 17. “Redux / NgRx across MFEs?”

Usually a trap. A federated store becomes a distributed monolith. We use component-local signals + HTTP. If we needed ephemeral UI state (wizard step), keep it inside the remote.

### 18. “How does auth work across remotes?”

Login in the shell → JWT in `localStorage` via `AuthSession`. Interceptor on `HttpClient` (shell; remotes too if standalone) attaches `Bearer`. Guards hide the wrong chair. API `@PreAuthorize`/role checks are authoritative. Remotes never get their own login page in the federated app.

### 19. “XSS and `localStorage` tokens?”

Any XSS in **any** remote can steal the token because they share origin in Docker (all on :4200). That is a reason to move to httpOnly cookies or a BFF in production, and to treat remotes as **trusted** as the shell. Independent *deploy* is not independent *trust*.

### 20. “Can remotes talk to each other without the API?”

Custom events, `postMessage`, a tiny event bus on `window`. We avoided it. Implicit coupling (“ledger must listen for `deferralChanged`”) is worse than a GET. The loan story is three remotes + one API, not three remotes chatting.

### 21. “How do you share a logged-in user object?”

`AuthSession` signals (`token`, `role`, `displayName`). Ledger also `GET /api/v1/me` to refresh role. The JWT claims include `sub`, `role`, `uid`.

---

## D. DevOps, Docker, environments

### 22. “How does this run in Docker vs local?”

Local: four Node dev servers + API + optional Compose Postgres; manifest uses `localhost:4201–4203`. Docker: each app is a Node build → nginx image; shell nginx reverse-proxies API and remotes; Docker manifest uses `/remotes/{name}/remoteEntry.json`.

### 23. “Why reverse-proxy remotes instead of exposing 4201–4203?”

One public origin (cookies, CSP, CORS, TLS). The browser should not know Compose service names. Import maps with relative URLs stay same-origin.

### 24. “CDN / caching `remoteEntry.json`?”

If you cache `remoteEntry.json` forever, hosts never see a new remote. Cache the **hashed chunks** long-term (`outputHashing: all`); cache the **entry** short (or `no-store`). We did not set this in nginx yet — a valid “what I’d add” answer.

### 25. “Independent deploy pipeline?”

Build `apps/sponsor` → push `angular-sb-app-sponsor` image → deploy that service. Host image only changes when the **manifest contract** or chrome changes. You can ship a census bugfix without rebuilding participant.

### 26. “What if a remote is down?”

`loadRemoteModule` rejects; that child’s `loadChildren` fails; Angular shows a navigation error unless you wrap it. Production: `loadChildren` catch → a shell “this chair is unavailable” route. We have an API-down banner for HTTP status 0, not a remote-down banner — easy follow-up improvement.

---

## E. Angular-specific

### 27. “Standalone APIs vs NgModules?”

All apps are standalone `bootstrapApplication` + `Routes`. Federation exposes the `routes` array. No `AppModule`.

### 28. “Signals vs RxJS?”

UI state is Angular signals (`household = signal(...)`). HTTP is still Observables (`HttpClient`). Interceptor is RxJS. We did not federate a signal store.

### 29. “Zone.js?”

Still in polyfills (`zone.js` ~0.15). Not zoneless. Not interesting unless they ask about zoneless Angular; then: we’d enable it on **all** remotes at once because Zone is a shared polyfill.

### 30. “Styling across remotes?”

CSS custom properties in `libs/shared/tokens.css` (Carbon, Bond, Folder, Ribbon, …). Each app’s `styles.css` imports the same file. No federated CSS runtime. Risk: a remote’s component styles can clash; we used component-scoped `styles: [\`...\`]`.

---

## F. Recordkeeping domain (they will pivot here)

Know these in **their** language.

### 31. “What is a recordkeeper vs a custodian vs a plan sponsor?”

- **Sponsor** (Jordan): the employer who offers the plan; census, payroll file, compliance.
- **Participant** (Maya): the employee/saver; deferral, beneficiaries, 529 for a dependent.
- **Recordkeeper** (Harborpath): keeps the books — who is in the plan, rates, YTD, loans — not necessarily the bank that holds assets.

### 32. “Why can’t Maya approve her own loan?”

Segregation of duties. The participant **requests**; the plan’s fiduciary process **approves**; cash **disburses** as a ledger event. UI hides the button; API returns 403.

### 33. “Why doesn’t the balance drop on loan request?”

A request is a case, not money movement. Disbursement is a 401(k) debit + `LOAN_DISBURSEMENT` contribution row. Same pattern as payroll: UI in one place, cash on the ledger.

### 34. “Are compliance alerts calculated or seeded?”

Priya’s missing primary is seeded (census-only person, no login). Maya’s is **computed** on `PUT .../beneficiaries`: no primary → `OPEN`; restore → `CLEARED`. Census `beneficiaryComplete` is always derived. Acknowledge means “ops saw it,” not “the household fixed it.”

### 35. “Deferral vs payroll vs YTD?”

Deferral is an **election** (% of pay). Payroll **applies** it: employee = periodPay × %, match = 50% of employee in this demo. YTD lives on `Account` and on `Contribution` rows. Changing % does not rewrite history.

---

## G. Behavioral / system-design wrappers

### 36. “Walk me through a production incident: sponsor sees old deferral.”

1. Confirm Maya’s `GET /accounts/{id}` vs Jordan’s `GET /census` — same API, different DTOs.
2. Cache? We don’t cache GETs in the UI beyond the component lifetime; refresh should hit the API.
3. Stale remote bundle? Check `remoteEntry` vs CDN.
4. Wrong plan id / JWT `uid`.
5. DB replica lag — we have one Postgres, so not that here.

### 37. “How would you scale to 40 remotes?”

Don’t. Group by domain (we already have three). Add a **manifest service** instead of a static JSON file. Platform team owns the shell; domain teams own remotes. Shared design system as npm, not a remote. Contract tests on `exposes`.

### 38. “SSR / SEO?”

Recordkeeping is an authenticated app. No SSR. If marketing pages needed SEO, they would not be this shell.

### 39. “Accessibility across remotes?”

Shell owns skip-to-content and header landmarks; remotes must not nest extra `<header>` carelessly. We used `role="alert"` on errors. A real audit would be axe-core in CI per remote.

### 40. “What’s the biggest mistake you made in this project?”

Examples you can tell honestly:

- Federation **build** `dev: true` / missing `target` — composition plumbing is easy to get wrong.
- Shell nginx resolving `api` at start — host of hosts has **startup order**.
- Treating seeded compliance as “the product” until we computed Maya’s alert — fixtures are not domain logic.

---

## H. Questions you should ask *them*

Interviews are two-way. Strong ones:

- Do your MFEs share a browser origin or are they multi-origin with a reverse proxy?
- Who owns the shell — a platform team?
- Is the recordkeeping core one service or already split (census vs trading vs money movement)?
- How do you version `remoteEntry` on the CDN?
- What’s the story for a down remote during enrollment season?

---

## 13. File map (memorize 8 paths)

| Path | Why it matters |
|---|---|
| `apps/shell/src/main.ts` | `initFederation` **before** bootstrap |
| `apps/shell/src/app/app.routes.ts` | `loadRemoteModule` + guards |
| `apps/shell/public/federation.manifest.json` | Local remotes |
| `apps/shell/public/federation.manifest.docker.json` | Compose remotes |
| `apps/*/federation.config.js` | `name`, `exposes`, `shareAll` |
| `apps/shell/nginx.conf` | `/api/` and `/remotes/` proxy |
| `libs/shared/auth-session.ts` | JWT session singleton |
| `backend/.../RecordkeepingService.java` | Real authorization and domain |

---

## 14. 90-second spoken summary (practice this)

> Harborpath is a recordkeeping demo with three independently built Angular remotes and a shell, composed with Native Federation because Angular 19 is esbuild, not webpack. The shell boots federation first so Angular is a singleton, then lazy-loads each remote’s route table from `remoteEntry.json`. Locally the manifest points at ports 4201–4203; in Docker the shell nginx reverse-proxies `/remotes/*` so the browser stays on one origin. We share session through a small library and JWT interceptor, but participant and sponsor data is isolated on the Spring API — guards are UX, 403s are security. The interesting part isn’t the import map; it’s that Maya’s deferral, loan request, and beneficiaries are participant UI, Jordan’s census and loan desk are sponsor UI, and cash only moves on the ledger remote, all against one Postgres system of record.

If they only remember one distinction from you, make it this: **federation composes JavaScript; the API composes the business.**
