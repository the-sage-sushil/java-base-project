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

The standalone login and registration pages call `POST /api/auth/login` and
`POST /api/auth/register`. The dashboard and profile require a signed-in
session; the admin users placeholder requires `ROLE_ADMIN`. The functional
HTTP interceptor adds the access token to API requests, sends 401 responses
back to sign-in, and routes 403 responses to the forbidden page.

For this milestone, the access token is kept in an Angular signal and mirrored
to `sessionStorage` so it survives page reloads. Roles and permissions decoded
from the token are display/UI hints only; the backend must enforce authorization
on every protected endpoint. Refresh-token support and the move to an
HttpOnly-cookie-based refresh flow are deferred to Milestone 8.

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
