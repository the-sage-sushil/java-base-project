import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../core/auth/auth.service';

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <main class="auth-card">
      <h1>Create account</h1>
      <form [formGroup]="form" (ngSubmit)="submit()">
        <label for="username">Username</label>
        <input id="username" autocomplete="username" formControlName="username" required>
        @if (form.controls.username.touched && form.controls.username.invalid) {
          <p class="field-error">Use at least 5 letters, numbers, or underscores.</p>
        }

        <label for="email">Email</label>
        <input id="email" type="email" autocomplete="email" formControlName="email" required>
        @if (form.controls.email.touched && form.controls.email.invalid) {
          <p class="field-error">Enter a valid email address.</p>
        }

        <label for="password">Password</label>
        <input id="password" type="password" autocomplete="new-password" formControlName="password" required>
        @if (form.controls.password.touched && form.controls.password.invalid) {
          <p class="field-error">Use 8+ characters with uppercase, lowercase, number, and special character.</p>
        }

        @if (errorMessage()) {
          <p class="error-message" role="alert">{{ errorMessage() }}</p>
        }
        <button type="submit" [disabled]="form.invalid || submitting()">Create account</button>
      </form>
      <p>Already registered? <a routerLink="/login">Sign in</a></p>
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
  `]
})
export class RegisterComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly submitting = signal(false);
  readonly errorMessage = signal('');
  readonly form = this.formBuilder.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(5), Validators.pattern(/^[a-zA-Z0-9_]+$/)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [
      Validators.required,
      Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/)
    ]]
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set('');
    this.auth.register(this.form.getRawValue()).subscribe({
      next: () => void this.router.navigate(['/login'], { queryParams: { registered: '1' } }),
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
    return 'Registration failed. Please check your details and try again.';
  }
}
