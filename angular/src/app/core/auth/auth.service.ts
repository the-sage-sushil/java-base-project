import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, finalize, map, Observable, of, shareReplay, tap, throwError } from 'rxjs';

import { AuthUser, LoginRequest, LoginResponse, ProfileResponse, RegisterRequest } from './auth.models';

interface JwtClaims {
  exp?: unknown;
  username?: unknown;
  email: unknown;
  roles?: unknown;
  role?: unknown;
  permissions?: unknown;
  authorities?: unknown;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _user = signal<AuthUser | null>(null);
  private readonly _token = signal<string | null>(null);
  private refreshRequest: Observable<LoginResponse> | null = null;
  private logoutRequest: Observable<void> | null = null;

  readonly currentUser = this._user.asReadonly();
  readonly accessToken = this._token.asReadonly();
  readonly isAuthenticated = computed(() => this._token() !== null);
  readonly roles = computed(() => this._user()?.roles ?? []);
  readonly permissions = computed(() => this._user()?.permissions ?? []);

  hasRole(role: string): boolean {
    return this.roles().includes(role);
  }

  hasPermission(permission: string): boolean {
    return this.permissions().includes(permission);
  }

  login(request: LoginRequest) {
    return this.http.post<LoginResponse>('/api/auth/login', request).pipe(
      tap((response) => {
        const claims = this.decodeToken(response.jwt);
        this.setSession(response, claims);
      })
    );
  }

  register(request: RegisterRequest) {
    return this.http.post<void>('/api/auth/register', request);
  }

  refresh(): Observable<LoginResponse> {
    if (!this.refreshRequest) {
      let request!: Observable<LoginResponse>;
      request = this.http.post<LoginResponse>('/api/auth/refresh', {}, { withCredentials: true }).pipe(
        tap((response) => this.setSession(response, this.decodeToken(response.jwt))),
        finalize(() => {
          if (this.refreshRequest === request) {
            this.refreshRequest = null;
          }
        }),
        shareReplay({ bufferSize: 1, refCount: false })
      );
      this.refreshRequest = request;
    }

    return this.refreshRequest;
  }

  restoreSession(): Observable<void> {
    return this.refresh().pipe(
      map(() => undefined),
      catchError(() => {
        this.clearSession();
        return of(undefined);
      })
    );
  }

  logout(returnUrl?: string): Observable<void> {
    if (!this.logoutRequest) {
      let request!: Observable<void>;
      request = this.http.post<void>('/api/auth/logout', {}, { withCredentials: true }).pipe(
        map(() => undefined),
        finalize(() => {
          if (this.logoutRequest === request) {
            this.logoutRequest = null;
          }
          this.clearSession();
          void this.router.navigate(['/login'], {
            queryParams: returnUrl ? { returnUrl } : undefined
          });
        }),
        shareReplay({ bufferSize: 1, refCount: false })
      );
      this.logoutRequest = request;
    }

    return this.logoutRequest;
  }

  profile() {
    return this.http.get<ProfileResponse>('/api/profile');
  }

  private clearSession(): void {
    this._token.set(null);
    this._user.set(null);
  }

  private setSession(response: LoginResponse, claims: JwtClaims | null): void {
    this._token.set(response.jwt);
    this._user.set(this.createUser(response.username, claims));
  }

  private createUser(username: string, claims: JwtClaims | null): AuthUser {
    const authorities = this.readAuthorities(claims?.role ?? claims?.authorities);
    const roles = this.readStringArray(claims?.roles);
    const email = claims?.email;
    const permissions = this.readStringArray(claims?.permissions);

    return {
      username,
      email: String(claims?.email),
      roles: roles.length ? roles : authorities.filter((authority) => authority.startsWith('ROLE_')),
      permissions: permissions.length
        ? permissions
        : authorities.filter((authority) => !authority.startsWith('ROLE_'))
    };
  }

  private decodeToken(token: string): JwtClaims | null {
    try {
      const payload = token.split('.')[1];
      if (!payload) {
        return null;
      }
      const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
      const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=');
      const binary = atob(padded);
      const bytes = Uint8Array.from(binary, (character) => character.charCodeAt(0));
      const decoded: unknown = JSON.parse(new TextDecoder().decode(bytes));
      return typeof decoded === 'object' && decoded !== null ? decoded as JwtClaims : null;
    } catch {
      return null;
    }
  }

  private readStringArray(value: unknown): string[] {
    return Array.isArray(value) ? value.filter((item): item is string => typeof item === 'string') : [];
  }

  private readAuthorities(value: unknown): string[] {
    if (!Array.isArray(value)) {
      return [];
    }
    return value.flatMap((item: unknown) => {
      if (typeof item === 'string') {
        return [item];
      }
      if (typeof item === 'object' && item !== null && 'authority' in item) {
        const authority = item.authority;
        return typeof authority === 'string' ? [authority] : [];
      }
      return [];
    });
  }
}
