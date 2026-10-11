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

