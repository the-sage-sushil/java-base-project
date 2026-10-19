import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  standalone: true,
  imports: [RouterLink],
  template: `
    <main class="content-card">
      <h1>Access denied</h1>
      <p>You do not have permission to view this page.</p>
      <a routerLink="/dashboard">Return to dashboard</a>
    </main>
  `,
  styles: [`
    :host { display: block; }
    .content-card { max-width: 38rem; margin: 4rem auto; padding: 2rem; background: white; border-radius: .75rem; box-shadow: 0 8px 30px #17255418; }
  `]
})
export class ForbiddenComponent {}
