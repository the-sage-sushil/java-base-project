import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../auth/auth.service';

export const permissionGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const requiredPermissions: string[] = route.data['permissions'] ?? [];

  if (requiredPermissions.every((permission) => auth.hasPermission(permission))) {
    return true;
  }

  return inject(Router).createUrlTree(['/forbidden']);
};