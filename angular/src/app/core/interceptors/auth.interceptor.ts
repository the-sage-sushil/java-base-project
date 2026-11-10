import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';

import { AuthService } from '../auth/auth.service';

const isAuthEndpoint = (url: string): boolean => {
  const path = url.split('?')[0];
  return path.startsWith('/api/auth/');
};

const isUnauthorized = (error: unknown): error is HttpErrorResponse =>
  error instanceof HttpErrorResponse && error.status === 401;

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const isApiRequest = request.url.startsWith('/api/');
  const isAuthRequest = isAuthEndpoint(request.url);
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = isApiRequest && !isAuthRequest ? auth.accessToken() : null;

  const authorizedRequest = token
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : request;

  const navigateForForbidden = (error: unknown): void => {
    if (error instanceof HttpErrorResponse && error.status === 403) {
      void router.navigate(['/forbidden']);
    }
  };

  const logoutAfterRefreshFailure = (): void => {
    auth.logout(router.url).subscribe({
      error: (error: unknown) => console.error('Unable to complete logout', error)
    });
  };

  return next(authorizedRequest).pipe(
    catchError((error: unknown) => {
      if (!isApiRequest || isAuthRequest || !isUnauthorized(error)) {
        navigateForForbidden(error);
        return throwError(() => error);
      }

      return auth.refresh().pipe(
        catchError((refreshError: unknown) => {
          logoutAfterRefreshFailure();
          return throwError(() => refreshError);
        }),
        switchMap((response) =>
          next(request.clone({ setHeaders: { Authorization: `Bearer ${response.jwt}` } })).pipe(
            catchError((retryError: unknown) => {
              if (isUnauthorized(retryError)) {
                logoutAfterRefreshFailure();
              }
              navigateForForbidden(retryError);
              return throwError(() => retryError);
            })
          )
        )
      );
    })
  );
};
