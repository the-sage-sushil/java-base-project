Registration with hashing is Milestone 2. Next is **Milestone 3: wire login through Spring Security properly**, then **Milestone 4: JWT**.

First, a quick check on your login. If it looks up the user and calls `passwordEncoder.matches(...)` manually, that works, but it bypasses Spring Security. The goal here is to understand the real flow, so replace it with the framework path.

## Step 1: Milestone 3, Spring Security login

1. **`CustomUserDetails`** implements `UserDetails`. It wraps your `User` and exposes:
   - `getAuthorities()`: roles as `ROLE_X`, plus permissions as plain `USER_READ`, etc.
   - `isEnabled()` and `isAccountNonLocked()`, mapped from your entity fields.
2. **`CustomUserDetailsService`** implements `UserDetailsService`. `loadUserByUsername(email)` fetches the user with roles and permissions. Use a `JOIN FETCH` or `@EntityGraph` to avoid lazy-loading errors and N+1 queries.
3. **`SecurityConfig`** defines:.
   - a `PasswordEncoder` bean (BCrypt)
   - an `AuthenticationManager` bean (from `AuthenticationConfiguration`)
   - a `SecurityFilterChain` that:
     - disables CSRF (you're stateless with bearer tokens)
     - sets session management to `STATELESS`
     - permits `/api/auth/**`, Swagger and `/api/public/**`
     - requires authentication for everything else
4. **`AuthService.login()`**:
   ```java
   Authentication auth = authenticationManager.authenticate(
       new UsernamePasswordAuthenticationToken(email, password));
   CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
   ```
   You never compare passwords yourself. `DaoAuthenticationProvider` calls your `UserDetailsService` and `PasswordEncoder` for you.
5. **Exception handling**: map `BadCredentialsException` to 401 with a generic message like "Invalid credentials". Never reveal whether the email or the password was wrong.

**Checkpoint:** a correct login returns user info, a wrong password gives 401, and a disabled user is rejected. A debugger on `DaoAuthenticationProvider.authenticate()` will show you the whole flow from first principles.

## Step 2: Milestone 4, JWT

Once Step 1 works:

1. Add the `jjwt` library (or Spring's built-in `NimbusJwtEncoder`).
2. Build a **`JwtService`** with `generateAccessToken`, `validateToken` and `extractUserId`. Load the signing key from config or an env var, never hardcode it, and make it at least 256 bits for HS256.
3. Return the access token from login (15-minute expiry).
4. Write **`JwtAuthenticationFilter`** (`OncePerRequestFilter`):
   - read the `Authorization: Bearer ...` header
   - validate the token
   - build a `UsernamePasswordAuthenticationToken` with authorities
   - set it in `SecurityContextHolder`
   - register it with `addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)`
5. Add `GET /api/profile`, which needs a valid token. Test it with no token (401), a garbage token (401) and a valid token (200).

Do refresh tokens later, in Milestone 7. Get the access token flow solid first.

## Step 3: Milestone 6, Embed Angular in Spring Boot (DONE)

**Goal:** one deployable jar. Spring Boot serves the Angular build and falls back to `index.html` for client-side routes, so refreshing `/login` or `/dashboard` never hits a Spring 404.

**Architecture decision:** Angular is embedded in Spring, not deployed as a separate container.

| Mode | How it runs | Why |
|---|---|---|
| Dev | `ng serve` on `:4200`, proxy `/api` to Spring on `:8080` | Hot reload, no CORS config |
| Prod | Maven builds Angular, copies it into the jar, Spring serves it on `:8080` | Single artifact, same origin, no CORS |

Consequence: the Docker plan drops from three containers to two (app and Postgres), and CORS is only needed if the frontend is ever split out.

### What was built

1. **SPA fallback (`core/config/SpaWebConfig.java`)**: a `WebMvcConfigurer` with a `PathResourceResolver` on `/**` that:
   - returns the real file if it exists in `classpath:/static/`
   - returns `null` (a real 404) for paths starting with `api/`, `v3/`, `swagger-ui`, `actuator`, or containing a `.` (missing `.js`, `.css`, images)
   - returns `index.html` for everything else (Angular routes)
2. **Security rules**: only the API is protected, because the SPA files contain no secrets:
   ```java
   .requestMatchers("/api/auth/**", "/api/public/**").permitAll()
   .requestMatchers("/api/admin/**").hasRole("ADMIN")
   .requestMatchers("/api/**").authenticated()
   .anyRequest().permitAll()
   ```
3. **Dev proxy (`frontend/proxy.conf.json`)** and `"proxyConfig": "proxy.conf.json"` under `serve.options` in `angular.json`.
4. **Maven integration (`backend/pom.xml`, `<profiles>` after `</build>`)**: profile `frontend`, active unless `-DskipFrontend` is passed.
   - `frontend-maven-plugin` installs Node v22.12.0, runs `npm ci` and `npm run build` in `../frontend` (`generate-resources` phase)
   - `maven-resources-plugin` copies `../frontend/dist/angular-base/browser` to `target/classes/static` (`prepare-package` phase)
5. **`.gitignore`**: `frontend/node/`, `frontend/node_modules/`, `frontend/dist/`. Build output is never committed.

### Commands

| Command | Use |
|---|---|
| `mvn clean package` (in `backend/`) | Full build: Angular and Spring, one jar |
| `mvn package -DskipFrontend` | Backend only, fast |
| `mvn help:active-profiles` | Confirm the `frontend` profile is active |
| `ng serve` (in `frontend/`) | Dev mode on `:4200` with proxy |

### Checkpoint (verified)

- `http://localhost:8080/` loads Angular.
- Refreshing a nested client route loads Angular, not a Spring 404.
- `/api/nope` returns 401/404, never HTML.
- `/missing.js` returns 404, never `index.html`.
- `jar tf target/*.jar | grep static/index.html` shows `BOOT-INF/classes/static/index.html`.

### Rules to remember

- Angular code always calls **relative** URLs (`/api/...`). They work in dev (proxy) and prod (same origin).
- If the jar has no `index.html`, `ng build` did not finish first. The copy step silently copies nothing from a missing directory.

---

## Step 4: Milestone 7, Angular authentication

**Goal:** a working login, register, dashboard and profile flow in Angular 21, with signal-based auth state, an HTTP interceptor and route guards. Refresh tokens come in Milestone 8, so this milestone handles a 401 by logging the user out.

**Key concept:** Angular guards are for user experience, not security. The backend must reject every unauthorized call on its own. You will prove this at the checkpoint.

### Folder structure

```
frontend/src/app/
├── core/
│   ├── auth/
│   │   ├── auth.models.ts
│   │   ├── auth.service.ts
│   │   └── token.storage.ts
│   ├── guards/
│   │   ├── auth.guard.ts
│   │   ├── role.guard.ts
│   │   └── permission.guard.ts
│   └── interceptors/
│       └── auth.interceptor.ts
├── features/
│   ├── auth/
│   │   ├── login/
│   │   └── register/
│   ├── dashboard/
│   ├── profile/
│   ├── admin/
│   │   └── users/            (placeholder, built in Milestone 9)
│   └── forbidden/
├── app.config.ts
└── app.routes.ts
```

All components are standalone and lazy-loaded with `loadComponent`.

### 7.1 Models (`auth.models.ts`)

Mirror your backend DTOs exactly. Check the field names in `LoginResponseDTO` (for example `jwt` or `accessToken`) and use the same ones.

```ts
export interface LoginRequest { email: string; password: string; }
export interface RegisterRequest { username: string; email: string; password: string; }
export interface LoginResponse {
  jwt: string;            // match your DTO field name
  username: string;
  expiresIn: number;
  roles: string[];
  permissions: string[];
}
export interface AuthUser { username: string; roles: string[]; permissions: string[]; }
```

### 7.2 Auth service (`auth.service.ts`)

State as signals, derived state as `computed`:

```ts
private readonly _user = signal<AuthUser | null>(null);
private readonly _token = signal<string | null>(null);

readonly currentUser = this._user.asReadonly();
readonly isAuthenticated = computed(() => this._token() !== null);
readonly roles = computed(() => this._user()?.roles ?? []);
readonly permissions = computed(() => this._user()?.permissions ?? []);

hasRole(role: string): boolean { return this.roles().includes(role); }
hasPermission(p: string): boolean { return this.permissions().includes(p); }
```

Methods:
- `login(req)`: `POST /api/auth/login`, then `tap` to store the token and set the user signals
- `register(req)`: `POST /api/auth/register` (returns 201, no body)
- `logout()`: clear signals and storage, navigate to `/login`
- `restoreSession()`: on app start, reload the token from storage and rebuild state

**Token storage decision for this milestone:** keep the access token in the signal and mirror it to `sessionStorage` so a page reload survives. This is a temporary choice. In Milestone 8 the refresh token moves to an HttpOnly cookie, and the access token stays in memory only, restored by a silent refresh on startup. Do not use `localStorage`.

**Decoding the JWT:** you may decode the payload (base64) to read `exp`, `roles` and `permissions` for display and for the UI. The client cannot verify the signature, so never treat decoded data as trusted. The server is the only authority.

Run `restoreSession()` through `provideAppInitializer(...)` in `app.config.ts` so guards see the right state on first navigation.

### 7.3 Interceptor (`auth.interceptor.ts`)

A functional `HttpInterceptorFn`:

1. If the URL starts with `/api/` and is not `/api/auth/login`, `/api/auth/register` (and later `/api/auth/refresh`), clone the request with `Authorization: Bearer <token>`.
2. On a 401 response from a non-auth endpoint, call `auth.logout()` and redirect to `/login` with a `returnUrl` query param.
3. On a 403, do not log out. Navigate to `/forbidden` or show a message, since the user is authenticated but not allowed.

Register it in `app.config.ts`:

```ts
provideHttpClient(withInterceptors([authInterceptor]))
```

### 7.4 Guards

Functional `CanActivateFn` guards. Each redirects, never throws.

| Guard | Rule | Redirect on fail |
|---|---|---|
| `authGuard` | `auth.isAuthenticated()` | `/login?returnUrl=<url>` |
| `roleGuard` | route `data.roles` contains one of the user's roles | `/forbidden` |
| `permissionGuard` | route `data.permissions` all present | `/forbidden` |

Usage in routes:

```ts
{ path: 'dashboard', canActivate: [authGuard], loadComponent: ... },
{ path: 'admin', canActivate: [authGuard, roleGuard], data: { roles: ['ROLE_ADMIN'] },
  children: [
    { path: 'users', canActivate: [permissionGuard], data: { permissions: ['USER_READ'] }, loadComponent: ... }
  ] },
```

### 7.5 Pages

| Route | Page | Notes |
|---|---|---|
| `/login` | Login | Reactive form (email, password), shows backend error, redirects to `returnUrl` or `/dashboard` |
| `/register` | Register | Reactive form (username, email, password, confirm), validators mirror backend rules, on 201 go to `/login` |
| `/dashboard` | Dashboard | Shows username, roles, permissions from signals |
| `/profile` | Profile | Calls `GET /api/profile` to prove the interceptor works |
| `/admin/users` | Placeholder | Only reachable with `ROLE_ADMIN` and `USER_READ` |
| `/forbidden` | Forbidden | Shown on 403 or guard failure |
| `**` | Not found | Wildcard route |

Rules for the forms:
- Disable the submit button while the request is running (a `loading` signal).
- Never log passwords. Never put them in the URL.
- Show the generic backend message for login failures. Do not add "email not found" logic in the client.

### 7.6 Role-based UI

Show and hide elements using the signals, for example a delete button:

```html
@if (auth.hasPermission('USER_DELETE')) {
  <button (click)="delete(user)">Delete</button>
}
```

This improves the experience only. It is not security.

### 7.7 Tests (Vitest, already in the project)

- `AuthService`: login sets signals, logout clears them, `hasRole` and `hasPermission` behave.
- `authInterceptor`: adds the header to `/api/` calls, skips auth endpoints, handles 401.
- Guards: allow and deny cases, correct redirect URLs.

### Checkpoint (definition of done)

1. Register a new user, log in, and land on `/dashboard`. Roles and permissions show.
2. `/profile` loads data, and the network tab shows `Authorization: Bearer ...`.
3. Refresh on `/dashboard`: you are still logged in.
4. Visiting `/dashboard` logged out redirects to `/login?returnUrl=/dashboard`, and after login you return there.
5. A `ROLE_USER` visiting `/admin/users` lands on `/forbidden`. A `ROLE_ADMIN` gets in.
6. Edit the stored token (change one character) and reload: the next API call returns 401 and you are logged out.
7. **The key test:** log in as a normal user, copy the token, and run this in a terminal. The backend must answer 403 even though Angular never let you open that page:
   ```bash
   curl -i -X DELETE http://localhost:8080/api/users/5 -H "Authorization: Bearer <USER_TOKEN>"
   ```
8. Works in both modes: `ng serve` on `:4200` and the embedded jar on `:8080`.

### Common pitfalls

- Hardcoding `http://localhost:8080` in services. Use `/api/...` only.
- Guards reading state before `restoreSession()` finishes. Use the app initializer.
- Treating a decoded JWT as proof of identity. Decode for display only.
- Redirect loops: the login route must not use `authGuard`.

### Note on the earlier plan text

The line "Do refresh tokens later, in Milestone 7" in Step 2 is outdated. Refresh tokens and logout are now **Milestone 8**.

---

## Updated roadmap after Milestone 7

| Milestone | Topic | Status |
|---|---|---|
| 1 | Foundation (Spring Boot, Angular, Postgres, Flyway) | Done |
| 2 | Registration and password hashing | Done |
| 3 | Spring Security login | Done |
| 4 / 4.5 | JWT and security config fixes | Done |
| 5 | Authorization (roles, permissions, tests) | Done |
| 6 | Embed Angular in Spring | Done |
| 7 | Angular authentication (service, interceptor, guards, pages) | Next |
| 8 | Refresh tokens and logout (HttpOnly cookie, rotation, reuse detection) | Planned |
| 9 | Admin module (users, roles, permissions) | Planned |
| 10 | Hardening, audit logs, security tests | Planned |
| 11 | Docker (multi-stage image) and README | Planned |




------------------------------------------------------------------------------------------------------------------------


# secure-starter: Plan (20/10/2026)

Reusable full-stack security starter: **Spring Boot (Java 25) + Angular 21 + PostgreSQL**. JWT auth and RBAC are built by hand so the Spring Security internals are understood from first principles. Clone it for future projects.

Last updated: 2026-10-20 (after reviewing the repo at commit `596a555`)

---

## 1. Roadmap

| # | Milestone | Status |
|---|---|---|
| 1 | Foundation (Spring Boot, Angular, Postgres, Flyway) | Done |
| 2 | Registration and password hashing (BCrypt) | Done |
| 3 | Spring Security login | Done |
| 4 / 4.5 | JWT and security config fixes | Done |
| 5 | Authorization (roles, permissions, `@PreAuthorize`) | Done |
| 6 | Embed Angular in Spring | Done |
| 7 | Angular authentication | Done (`596a555`) |
| **7.5** | **Cleanup and fixes found in repo review (section 3)** | **Next, about half a day** |
| 8 | Refresh tokens and logout | After 7.5 |
| 9 | Admin module | Planned |
| 10 | Hardening, audit logs, security tests, Testcontainers | Planned |
| 11 | Docker and README | Planned |
| 12 | OAuth2/OIDC social login | Planned |
| 13 | Keycloak as own IdP, SSO across two apps | Planned |
| 14 | (Optional) Backend as OAuth2 resource server | Planned |

---

## 2. Architecture decisions (keep these)

- **Modular monolith** using Spring Modulith (`auth`, `user`, `core` modules, verified by `ModulithArchitectureTests`). No microservices.
- **Angular embedded in Spring.** One jar, same origin, no CORS. Docker = 2 containers (app + Postgres).
- **Dev:** `ng serve` on `:4200`, proxy `/api` to `:8080`. **Prod:** Maven builds Angular into `target/classes/static`.
- **Only `/api/**` is protected;** `/api/auth/**` and `/api/public/**` are open, `/api/admin/**` needs `ROLE_ADMIN`.
- **Stateless sessions, CSRF off** (bearer tokens). Revisit for the refresh cookie (M8).
- **Guards are UX, not security.** The backend rejects every unauthorized call itself.
- **Decoded JWT on the client is display-only.** The server loads the user from the DB on every request, so the backend never trusts token claims for authorities.
- Authorities: roles as `ROLE_X`, permissions as `USER_READ` etc.
- No `localStorage` for tokens. Angular calls relative URLs only.

| Command | Use |
|---|---|
| `mvn clean package` (in `backend/`) | Full build, one jar |
| `mvn package -DskipFrontend` | Backend only |
| `ng serve` (in `frontend/`) | Dev mode with proxy |

---

## 3. Milestone 7.5: Cleanup before refresh tokens

Findings from the repo review, in priority order. Do 1 to 4 before M8.

### Security
- [ ] **1. JWT secret is committed.** It is in `application.yaml` and in `backend/.env` (not git-ignored). Fix: `jwt.secret: ${JWT_SECRET}` with no default, add `.env` to `.gitignore`, and **generate a new secret**, because the old one stays in git history. Optionally fail startup if it is under 32 bytes.
- [ ] **2. Dev admin password is committed** in `application-dev.yaml`, while the README says to use `DEV_ADMIN_PASSWORD`. Fix: `password: ${DEV_ADMIN_PASSWORD}`, and delete the committed value from the file.

### Bugs
- [ ] **3. Duplicate email/username most likely returns 500.** `UserService` throws `IllegalArgumentException`, and `GlobalExceptionHandler` has no handler for it. Validation failures (`@Valid`) also return no useful message, so the Angular form only shows the generic fallback. Fix:
  - throw a `DuplicateResourceException`, return **409** with `{message}`
  - handle `MethodArgumentNotValidException`, return **400** with per-field messages
  - handle `DataIntegrityViolationException` as 409 (the check-then-insert has a race; the DB unique constraint is the real guard)
  - **normalise email to lowercase** on register and login (the DB unique index is case-sensitive)
- [ ] **4. `/profile` page never calls `GET /api/profile`.** Plan checkpoint 2 ("network tab shows `Authorization: Bearer`") can't be verified. Add a `ProfileApi` call and show `username` and `email` from the server, next to the decoded token values.

### Tests
- [ ] **5. Missing tests from the M7 plan:** `authInterceptor` (adds header, skips auth endpoints, 401 and 403 handling) and the three guards. The backend has none for the `JwtAuthenticationFilter` (garbage, expired, tampered token; disabled user), login, or register.
- [ ] **6. Backend tests need a live Postgres on `:5486` and a built `static/index.html`** (`anonymousCanLoadAngularApp`). Move to Testcontainers in M10 and mark the SPA test to skip with `-DskipFrontend`.

### Tidy
- [ ] `LoginResponseDTO.expiresIn` is **minutes** (`getTokenExpire`), but the original design said seconds. Switch to seconds now, because M8 will use it to schedule refresh.
- [ ] Move the `PasswordEncoder` bean out of `UserDetailsServiceImpl` into a config class.
- [ ] `JwtService`: parse claims once per call; `validateToken` compares the subject to the `email` claim, which is always equal, so check expiry plus the username instead.
- [ ] User timestamps use `java.util.Date`; prefer `Instant` for `TIMESTAMPTZ`.
- [ ] Delete empty `core/guards/app.guard.ts` and the unused imports in `LoginResponseDTO`.
- [ ] `.vscode/settings.json` has a bogus `java.format.settings.url`. `launch.json` has staging and prod profiles with no matching config files.
- [ ] The swagger filter chain matches `/swagger/open-api/**`, but no springdoc or Actuator dependency exists yet. Add both (M10) or remove the chain.
- [ ] Register form has no confirm-password field (was in the M7 plan).

---

## 4. Milestone 8: Refresh tokens and logout

**Why:** a short-lived access token is safe but annoying. A refresh token gets new ones without re-login, and since it lives in the DB it can be revoked, which a stateless JWT cannot.

### 4.1 Design decisions

| Decision | Choice | Reason |
|---|---|---|
| Format | Opaque random string (32+ bytes, `SecureRandom`) | Must be revocable, so it is looked up anyway |
| DB storage | SHA-256 hash only | A DB leak gives no usable tokens |
| Browser storage | HttpOnly, SameSite=Strict cookie, `Path=/api/auth`, `Secure` configurable | JavaScript and XSS can't read it |
| Access token | Memory only (signal); drop the `sessionStorage` mirror | Restored by silent refresh on load |
| Rotation | New refresh token on every refresh; old one marked used | Limits stolen-token window |
| Reuse detection | A used token shows up again: revoke the whole family | Someone has a copy |
| Lifetimes | Access 15 min, refresh 7 days absolute | |
| Disabled/locked users | Check `isEnabled()` and `isAccountNonLocked()` on every refresh | Disabling a user must stop refresh too |

### 4.2 Fit with Spring Modulith (important)

The `auth` module may only depend on `user` public API and `core::security`. `User` and `CustomUserDetails` are in internal sub-packages, so the refresh-token code **must not reference them**.

- Put the code in `auth/tokens/` (`RefreshToken`, `RefreshTokenRepository`, `RefreshTokenService`, `RefreshCookieFactory`).
- Store the **subject (email)** as a plain column, the same identifier the JWT `sub` already uses. No JPA relation to `User`.
- On refresh: token row gives email, then `UserDetailsService.loadUserByUsername(email)` (a Spring interface, so no module violation), then check enabled and unlocked, then `jwtService.generateAccessToken`.
- Deleting or disabling a user needs no extra work: the lookup fails and the refresh returns 401.
- Later alternative: expose a small lookup interface in the `user` module root and switch to `user_id`.

Run `ModulithArchitectureTests` after each step.

### 4.3 Backend steps (do first, test with curl)

1. **Migration** `V4__create_refresh_tokens.sql`: `id, subject, token_hash (unique), family_id (uuid), expires_at, used_at, revoked_at, created_at, user_agent, ip`; index on `family_id` and `subject`.
2. **Entity and repository:** `findByTokenHash`, `revokeAllByFamilyId`, `revokeAllBySubject`.
3. **`RefreshTokenService`:**
   - `create(subject, familyId)`: generate, store hash, return raw value once.
   - `rotate(raw)`, `@Transactional`, with a row lock (`@Lock(PESSIMISTIC_WRITE)`) so two parallel refreshes can't both win:
     ```java
     var rt = repo.findByTokenHash(sha256(raw)).orElseThrow(Invalid::new);
     if (rt.getRevokedAt() != null || rt.getExpiresAt().isBefore(now)) throw new Invalid();
     if (rt.getUsedAt() != null) {                 // REUSE DETECTED
         repo.revokeAllByFamilyId(rt.getFamilyId());
         throw new Invalid();
     }
     rt.setUsedAt(now);
     return create(rt.getSubject(), rt.getFamilyId());
     ```
   - Caveat: a revoke inside a transaction that then throws gets rolled back. Use `REQUIRES_NEW` for the family revoke, or return a result object instead of throwing.
4. **Endpoints** (already covered by `/api/auth/**` permitAll):
   - `POST /login`: sets the cookie, new family. The body stays `LoginResponseDTO`.
   - `POST /refresh`: rotate, set the new cookie, return `LoginResponseDTO` (same shape as login, so the frontend reuses it). Failure: 401 and clear the cookie.
   - `POST /logout`: revoke the family, clear the cookie.
5. **Cookie** via `ResponseCookie`, `secure` from a property (`app.security.cookie-secure`, `false` in the dev profile).
6. **`@Scheduled` cleanup** of expired rows.

### 4.4 Frontend steps (after the backend works)

1. `AuthService`: remove `sessionStorage`/`TokenStorage`. Add `refresh()` (the browser sends the cookie). `logout()` calls the backend, then clears signals.
2. `restoreSession()` becomes async (returns an Observable that never errors: failure means logged out), still wired through `provideAppInitializer`. Optional: skip the call when no session hint exists, to avoid a console 401 for every anonymous visitor.
3. Interceptor: on 401 from a non-auth URL, refresh **once**, then retry the request. **Single-flight:** parallel 401s share one refresh. If refresh fails, log out. Never refresh on `/api/auth/*`.
4. Update `auth.service.spec.ts`: it currently asserts `sessionStorage`.

### 4.5 Checkpoint

1. Cookie is HttpOnly with `Path=/api/auth`; `document.cookie` doesn't show it.
2. With a 30s access expiry, a later call succeeds transparently (`/refresh` then retry).
3. Reload keeps you logged in, with nothing in storage.
4. 5 parallel calls with an expired token produce exactly **one** `/refresh`.
5. Replaying a used refresh token via curl gives 401, **and the legitimate newest token is revoked too**.
6. Logout, then replaying the cookie gives 401.
7. Tampered or missing cookie gives 401 and a redirect to `/login`.
8. Disable the user in the DB: the next refresh gives 401.

### 4.6 Tests

Valid, expired, revoked, reused tokens; reuse revokes the family; concurrent rotation succeeds once; `/refresh` with no cookie gives 401; logout invalidates; disabled user can't refresh.

### 4.7 CSRF note

`SameSite=Strict` + `Path=/api/auth` + POST-only protects the refresh endpoint. This works because Angular is embedded (same origin). If the frontend is ever split to another domain, revisit.

---

## 5. Milestone 9: Admin module

- Users: list, create, update, disable, delete. Roles: list, create, assign permissions. Permissions: view.
- Backend: `@PreAuthorize("hasAuthority('USER_...')")`. Angular `/admin/users` (placeholder today) needs `ROLE_ADMIN` + `USER_READ`.
- Delete or disable user also revokes that user's refresh tokens (`revokeAllBySubject`).
- Role-based UI with signals is UX only.

## 6. Milestone 10: Hardening, audit, tests

- Account locking, failed-login counter, password change, rate limiting on `/api/auth/*`, security headers.
- `audit_logs`: LOGIN_SUCCESS/FAILED, LOGOUT, PASSWORD_CHANGED, USER_CREATED/UPDATED, ROLE_ASSIGNED, PERMISSION_CHANGED, ACCESS_DENIED.
- Add Testcontainers, Actuator, springdoc.
- Security tests: anonymous 401, USER 403, ADMIN 200, missing `USER_DELETE` 403; invalid, expired, tampered JWT; revoked refresh token; disabled user; wrong password; duplicate email.
- **Key proof:** a normal user's token on `curl -X DELETE /api/users/5` returns 403.

## 7. Milestone 11: Docker and README

- Multi-stage Dockerfile, compose with app + Postgres. Keep the `public.ecr.aws/docker/library/postgres` image (Docker Hub times out on your network).
- README: architecture, auth flow, authorization flow, JWT and refresh flow, DB model, filter chain, Angular auth and guards, running locally, Docker, test users, diagrams.

---

## 8. OAuth 2.0, OIDC and SSO (M12 to M14)

| Term | What it solves |
|---|---|
| OAuth 2.0 | Delegated authorization: app A accesses data in service B without the password. Issues access tokens. |
| OIDC | Authentication on top of OAuth 2.0. Adds an **ID token** (a JWT saying who the user is). "Login with Google". |
| SSO | One login at a central Identity Provider (IdP), trusted by many apps. Built with OIDC (or SAML). |

**Authorization Code flow with PKCE:**
```
1. User clicks "Login with Google"
2. App redirects browser to IdP /authorize
     ?client_id & redirect_uri & scope=openid email & state=<random> & code_challenge
3. User logs in AT THE IdP (app never sees the password)
4. IdP redirects to redirect_uri?code=<one-time code>&state=...
5. App backend POSTs to IdP /token (code + code_verifier [+ client_secret])
6. IdP returns access_token + id_token (+ refresh_token)
7. App validates the id_token, finds or creates the local user, issues its own session/JWT
```
- **Code in the redirect, tokens on the back channel:** URLs leak.
- **`state`:** blocks CSRF on the callback. **PKCE:** a stolen code is useless without the verifier.
- **SSO:** the IdP keeps its own session cookie, so a second app's redirect finds the user already logged in.

**Milestones:**
- **M12 Social login:** `spring-boot-starter-oauth2-client` with Google/GitHub. Map to a local `User` with local roles. Decide account linking by email (auto-linking unverified emails is a takeover risk), and fit it into the `auth` module boundaries.
- **M13 Keycloak:** in docker-compose. Make the starter a client, add a second small app, watch SSO. Learn realms, clients, claim mapping, logout.
- **M14 (optional):** resource server validating IdP JWTs via JWK set.

---

## 9. Rules to remember

- Never hardcode secrets; HS256 key is at least 256 bits and comes from env.
- Generic login error; never reveal which of email or password was wrong.
- Never log passwords or put them in URLs.
- Guards read state only after the app initializer finishes. The login route must not use `authGuard`.
- Refresh tokens are in **Milestone 8** (older notes saying M7 are obsolete).

