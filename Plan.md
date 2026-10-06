Registration with hashing is Milestone 2. Next is **Milestone 3: wire login through Spring Security properly**, then **Milestone 4: JWT**.

First, a quick check on your login. If it looks up the user and calls `passwordEncoder.matches(...)` manually, that works, but it bypasses Spring Security. The goal here is to understand the real flow, so replace it with the framework path.

## Step 1: Milestone 3, Spring Security login

1. **`CustomUserDetails`** implements `UserDetails`. It wraps your `User` and exposes:
   - `getAuthorities()`: roles as `ROLE_X`, plus permissions as plain `USER_READ`, etc.
   - `isEnabled()` and `isAccountNonLocked()`, mapped from your entity fields.
2. **`CustomUserDetailsService`** implements `UserDetailsService`. `loadUserByUsername(email)` fetches the user with roles and permissions. Use a `JOIN FETCH` or `@EntityGraph` to avoid lazy-loading errors and N+1 queries.
3. **`SecurityConfig`** defines:
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

## Before you start

Check that your seed data creates `ROLE_USER` and the permissions, and that registration assigns `ROLE_USER`. Step 1 depends on the authorities being populated.

Want to go with Step 1? Paste your current `User` entity and login service, and I'll tell you what to change and what the `UserDetails` / `SecurityConfig` code should look like.