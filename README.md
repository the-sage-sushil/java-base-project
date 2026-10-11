# java-base-project
java-base-project

## Backend API

- `GET /api/public/hello` is public.
- `GET /api/profile` requires authentication.
- `GET /api/admin/users` requires `ROLE_ADMIN`.
- `DELETE /api/users/{id}` requires the `USER_DELETE` authority.

For local development, activate the `dev` profile and provide `DEV_ADMIN_PASSWORD`.
`DEV_ADMIN_USERNAME` and `DEV_ADMIN_EMAIL` are optional and default to `admin`
and `admin@example.com`. The dev-profile seeder creates an admin account only
when that email is not already registered. For example, in PowerShell:

```powershell
$env:DEV_ADMIN_PASSWORD = "use-a-local-development-password"
mvn -f backend\pom.xml spring-boot:run "-Dspring-boot.run.profiles=dev"
```
