import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../auth/auth.service';

export const roleGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const requiredRoles: string[] = route.data['roles'] ?? [];

  if (requiredRoles.some((role) => auth.hasRole(role))) {
    return true;
  }

  return inject(Router).createUrlTree(['/forbidden']);
};