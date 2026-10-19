import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';

@Component({
  standalone: true,
  imports: [RouterLink],
  template: `
    <main class="content-card">
      <a routerLink="/dashboard">← Dashboard</a>
      <h1>Your profile</h1>
      <dl>
        <dt>Username</dt>
        <dd>{{ auth.currentUser()?.username }}</dd>
        <dt>Roles</dt>
        <dd>{{ auth.roles().length ? auth.roles().join(', ') : 'No roles found in token' }}</dd>
        <dt>Permissions</dt>
        <dd>{{ auth.permissions().length ? auth.permissions().join(', ') : 'No permissions found in token' }}</dd>
      </dl>
      <p class="disclaimer">These values are decoded from the access token for display only and are not trusted for authorization.</p>
    </main>
  `,
  styles: [`
    :host { display: block; }
    .content-card { max-width: 52rem; margin: 4rem auto; padding: 2rem; background: white; border-radius: .75rem; box-shadow: 0 8px 30px #17255418; }
    dl { display: grid; grid-template-columns: minmax(8rem, 1fr) 3fr; gap: .75rem; }
    dt { font-weight: 700; }
    dd { margin: 0; overflow-wrap: anywhere; }
    .disclaimer { color: #475569; font-size: .9rem; }
  `]
})
export class ProfileComponent {
  readonly auth = inject(AuthService);
}
