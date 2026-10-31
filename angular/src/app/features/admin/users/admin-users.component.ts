import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  standalone: true,
  imports: [RouterLink],
  template: `
    <main class="content-card">
      <a routerLink="/dashboard">← Dashboard</a>
      <h1>User administration</h1>
      <p>This page is reserved for the user management feature planned for Milestone 9.</p>
    </main>
  `,
  styles: [`
    :host { display: block; }
    .content-card { max-width: 52rem; margin: 4rem auto; padding: 2rem; background: white; border-radius: .75rem; box-shadow: 0 8px 30px #17255418; }
  `]
})
export class AdminUsersComponent {}
