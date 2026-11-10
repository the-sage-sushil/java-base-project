# java-base-project
java-base-project

## Backend API

- `GET /api/public/hello` is public.
- `GET /api/profile` requires authentication.
- `GET /api/admin/users` requires `ROLE_ADMIN` and `USER_READ`.
- `DELETE /api/users/{id}` requires the `USER_DELETE` authority.
- `POST /api/auth/login`, `/register`, `/refresh`, and `/logout` provide authentication.
- Admin user, role, and permission endpoints are under `/api/admin/**`.
- `POST /api/account/password` requires authentication.

Access tokens are held in browser memory only. Login/refresh sets an opaque,
rotating refresh token in an HttpOnly, SameSite=Strict cookie scoped to
`/api/auth`; refresh tokens are stored as hashes and reuse revokes the family.
The Angular initializer restores a session via `/api/auth/refresh`, and the
HTTP interceptor retries an unauthorized API request once after a shared
refresh. Keep the frontend same-origin with the backend; if that changes,
revisit the CSRF and cookie policy.

The backend exposes health information at `/actuator/health` and OpenAPI docs
at `/swagger-ui.html`. Login failures are rate-limited and counted toward
account locking; security-relevant actions are persisted in `audit_logs`.

Run backend tests with `mvn test`. PostgreSQL migration tests use Testcontainers
when Docker is available; without Docker they are skipped.

For local development, set `JWT_SECRET` to a private random value of at least
32 bytes, activate the `dev` profile, and provide `DEV_ADMIN_PASSWORD`.
`DEV_ADMIN_USERNAME` and `DEV_ADMIN_EMAIL` are optional and default to `admin`
and `admin@example.com`. The dev-profile seeder creates an admin account only
when that email is not already registered. For example, in PowerShell:

```powershell
$bytes = [byte[]]::new(32)
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
$env:DEV_ADMIN_PASSWORD = "use-a-local-development-password"
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```
