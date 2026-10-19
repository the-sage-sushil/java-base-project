import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../core/auth/auth.service';

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <main class="auth-card">
      <h1>Sign in</h1>
      @if (registered) {
        <p class="notice" role="status">Your account was created. Sign in to continue.</p>
      }
      <form [formGroup]="form" (ngSubmit)="submit()">
        <label for="email">Email</label>
        <input id="email" type="email" autocomplete="email" formControlName="email" required>
        @if (form.controls.email.touched && form.controls.email.invalid) {
          <p class="field-error">Enter a valid email address.</p>
        }

        <label for="password">Password</label>
        <input id="password" type="password" autocomplete="current-password" formControlName="password" required>
        @if (form.controls.password.touched && form.controls.password.invalid) {
          <p class="field-error">Password is required.</p>
        }

        @if (errorMessage()) {
          <p class="error-message" role="alert">{{ errorMessage() }}</p>
        }
        <button type="submit" [disabled]="form.invalid || submitting()">Sign in</button>
      </form>
      <p>New here? <a routerLink="/register">Create an account</a></p>
    </main>
  `,
  styles: [`
    :host { display: block; }
    .auth-card { max-width: 28rem; margin: 4rem auto; padding: 2rem; background: white; border-radius: .75rem; box-shadow: 0 8px 30px #17255418; }
    form { display: grid; gap: .75rem; }
    label { font-weight: 600; }
    input { width: 100%; padding: .75rem; border: 1px solid #cbd5e1; border-radius: .375rem; }
    button { margin-top: .5rem; padding: .8rem; border: 0; border-radius: .375rem; color: white; background: #1d4ed8; cursor: pointer; }
    button:disabled { opacity: .55; cursor: wait; }
    .field-error, .error-message { color: #b91c1c; margin: 0; }
    .notice { color: #166534; }
  `]
})
export class LoginComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly submitting = signal(false);
  readonly errorMessage = signal('');
  readonly registered = this.route.snapshot.queryParamMap.has('registered');
  readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set('');
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        const destination = returnUrl?.startsWith('/') && !returnUrl.startsWith('//')
          ? returnUrl
          : '/dashboard';
        void this.router.navigateByUrl(destination);
      },
      error: (error: unknown) => {
        this.errorMessage.set(this.getErrorMessage(error));
        this.submitting.set(false);
      }
    });
  }

  private getErrorMessage(error: unknown): string {
    if (
      error instanceof HttpErrorResponse
      && typeof error.error === 'object'
      && error.error !== null
      && 'message' in error.error
      && typeof error.error.message === 'string'
    ) {
      return error.error.message;
    }
    return 'Sign in failed. Check your email and password.';
  }
}
