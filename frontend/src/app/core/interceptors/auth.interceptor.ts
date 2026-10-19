import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../auth/auth.service';

const isAuthEndpoint = (url: string): boolean => {
  const path = url.split('?')[0];
  return ['/api/auth/login', '/api/auth/register', '/api/auth/refresh'].includes(path);
};

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const isApiRequest = request.url.startsWith('/api/');
  const isAuthRequest = isAuthEndpoint(request.url);
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = isApiRequest && !isAuthRequest ? auth.accessToken() : null;

  const authorizedRequest = token
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : request;

  return next(authorizedRequest).pipe(
    catchError((error: unknown) => {
      if (isApiRequest && !isAuthRequest && error instanceof HttpErrorResponse) {
        if (error.status === 401) {
          auth.logout(router.url);
        } else if (error.status === 403) {
          void router.navigate(['/forbidden']);
        }
      }
      return throwError(() => error);
    })
  );
};