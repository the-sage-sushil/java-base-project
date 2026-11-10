# AngularBase

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 21.2.24.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This compiles the Angular app into the backend's `src/main/resources/static/` directory.

## Authentication

The login and registration pages call `POST /api/auth/login` and
`POST /api/auth/register`. The access token is kept only in an Angular signal.
The server stores a rotating refresh token in an HttpOnly, SameSite=Strict
cookie scoped to `/api/auth`; the initializer silently refreshes on reload.
When a protected API request receives a 401, the interceptor shares one refresh
request across concurrent failures and retries each request once.

The admin users page requires both `ROLE_ADMIN` and `USER_READ`. Roles and
permissions decoded from the access token only control UI visibility; the
backend separately enforces each admin permission. A refresh failure clears
the in-memory session and returns the user to sign-in.

The cookie and Angular app are same-origin. If the frontend is moved to another
domain, revisit the SameSite and CSRF policy before deployment.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
