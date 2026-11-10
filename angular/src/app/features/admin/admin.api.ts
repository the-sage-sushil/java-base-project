import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { AdminPermission, AdminRole, AdminUser } from './admin.models';

@Injectable({ providedIn: 'root' })
export class AdminApi {
  private readonly http = inject(HttpClient);

  users() {
    return this.http.get<AdminUser[]>('/api/admin/users');
  }

  createUser(request: { username: string; email: string; password: string }) {
    return this.http.post<AdminUser>('/api/admin/users', request);
  }

  updateUser(id: number, request: { username: string; email: string }) {
    return this.http.put<AdminUser>(`/api/admin/users/${id}`, request);
  }

  setUserEnabled(id: number, enabled: boolean) {
    return this.http.patch<AdminUser>(`/api/admin/users/${id}/enabled`, { enabled });
  }

  assignRoles(id: number, roleIds: number[]) {
    return this.http.put<AdminUser>(`/api/admin/users/${id}/roles`, { roleIds });
  }

  deleteUser(id: number) {
    return this.http.delete<void>(`/api/admin/users/${id}`);
  }

  roles() {
    return this.http.get<AdminRole[]>('/api/admin/roles');
  }

  createRole(name: string) {
    return this.http.post<AdminRole>('/api/admin/roles', { name });
  }

  assignPermissions(roleId: number, permissionIds: number[]) {
    return this.http.put<AdminRole>(`/api/admin/roles/${roleId}/permissions`, { permissionIds });
  }

  permissions() {
    return this.http.get<AdminPermission[]>('/api/admin/permissions');
  }
}
