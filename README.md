# java-base-project
java-base-project

## Backend API

- `GET /api/public/hello` is public.
- `GET /api/profile` requires authentication.
- `GET /api/admin/users` requires `ROLE_ADMIN`.
- `DELETE /api/users/{id}` requires the `USER_DELETE` authority.

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
mvn -f backend\pom.xml spring-boot:run "-Dspring-boot.run.profiles=dev"
```
