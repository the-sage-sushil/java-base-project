import { Injectable } from '@angular/core';

const TOKEN_KEY = 'access_token';

@Injectable({ providedIn: 'root' })
export class TokenStorage {
  getToken(): string | null {
    return typeof window === 'undefined' ? null : window.sessionStorage.getItem(TOKEN_KEY);
  }

  setToken(token: string): void {
    if (typeof window !== 'undefined') {
      window.sessionStorage.setItem(TOKEN_KEY, token);
    }
  }

  clearToken(): void {
    if (typeof window !== 'undefined') {
      window.sessionStorage.removeItem(TOKEN_KEY);
    }
  }
}