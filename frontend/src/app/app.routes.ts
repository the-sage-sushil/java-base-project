import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';
import { permissionGuard } from './core/guards/permission.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then((module) => module.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register.component').then((module) => module.RegisterComponent)
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./features/dashboard/dashboard.component').then((module) => module.DashboardComponent)
  },
  {
    path: 'profile',
    canActivate: [authGuard, permissionGuard],
    data: { permissions: ['PROFILE_READ'] },
    loadComponent: () => import('./features/profile/profile').then((module) => module.ProfileComponent)
  },
  {
    path: 'admin/users',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ROLE_ADMIN'] },
    loadComponent: () => import('./features/admin/users/admin-users.component').then((module) => module.AdminUsersComponent)
  },
  {
    path: 'forbidden',
    loadComponent: () => import('./features/forbidden/forbidden.component').then((module) => module.ForbiddenComponent)
  },
  { path: '**', redirectTo: 'dashboard' }
];
