import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';

@Component({
  standalone: true,
  imports: [RouterLink],
  template: `
    <main class="content-card">
      <p class="eyebrow">Dashboard</p>
      <h1>Welcome, {{ auth.currentUser()?.username }}</h1>
      <p>You are signed in. Your account roles and permissions are shown for convenience only.</p>
      <nav aria-label="Account navigation">
        <a routerLink="/profile">View profile</a>
        @if (auth.hasRole('ROLE_ADMIN')) {
          <a routerLink="/admin/users">Manage users</a>
        }
        <button type="button" (click)="auth.logout()">Sign out</button>
      </nav>
    </main>
  `,
  styles: [`
    :host { display: block; }
    .content-card { max-width: 52rem; margin: 4rem auto; padding: 2rem; background: white; border-radius: .75rem; box-shadow: 0 8px 30px #17255418; }
    .eyebrow { color: #1d4ed8; font-weight: 700; text-transform: uppercase; letter-spacing: .08em; }
    nav { display: flex; flex-wrap: wrap; align-items: center; gap: 1rem; margin-top: 2rem; }
    button { padding: .65rem 1rem; border: 0; border-radius: .375rem; color: white; background: #b91c1c; cursor: pointer; }
  `]
})
export class DashboardComponent {
  readonly auth = inject(AuthService);
}
