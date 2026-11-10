import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { AuthService } from '../../../core/auth/auth.service';
import { AdminApi } from '../admin.api';
import { AdminPermission, AdminRole, AdminUser } from '../admin.models';

@Component({
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <main class="content-card">
      <a routerLink="/dashboard">Dashboard</a>
      <h1>User administration</h1>
      @if (error()) {
        <p class="error" role="alert">{{ error() }}</p>
      }

      <section aria-labelledby="users-title">
        <h2 id="users-title">Users</h2>
        @if (auth.hasPermission(editingUserId() === null ? 'USER_CREATE' : 'USER_UPDATE')) {
        <form (ngSubmit)="saveUser()">
          <label>
            Username
            <input name="username" [(ngModel)]="userForm.username" required minlength="5">
          </label>
          <label>
            Email
            <input name="email" type="email" [(ngModel)]="userForm.email" required>
          </label>
          @if (editingUserId() === null) {
            <label>
              Temporary password
              <input name="password" type="password" [(ngModel)]="userForm.password" required minlength="8">
            </label>
          }
          <div class="actions">
            <button type="submit">{{ editingUserId() === null ? 'Create user' : 'Save user' }}</button>
            @if (editingUserId() !== null) {
              <button type="button" class="secondary" (click)="cancelEdit()">Cancel</button>
            }
          </div>
        </form>
        }

        <div class="table-wrap">
          <table>
            <thead>
              <tr><th>Username</th><th>Email</th><th>Roles</th><th>Status</th><th>Actions</th></tr>
            </thead>
            <tbody>
              @for (user of users(); track user.id) {
                <tr>
                  <td>{{ user.username }}</td>
                  <td>{{ user.email }}</td>
                  <td>{{ user.roles.join(', ') }}</td>
                  <td>{{ user.enabled ? 'Enabled' : 'Disabled' }}</td>
                  <td class="actions">
                    @if (auth.hasPermission('USER_UPDATE')) {
                      <button type="button" class="secondary" (click)="editUser(user)">Edit</button>
                      <button type="button" class="secondary" (click)="editRoles(user)">Roles</button>
                      <button type="button" class="secondary" (click)="toggleEnabled(user)">
                        {{ user.enabled ? 'Disable' : 'Enable' }}
                      </button>
                    }
                    @if (auth.hasPermission('USER_DELETE')) {
                      <button type="button" class="danger" (click)="deleteUser(user)">Delete</button>
                    }
                  </td>
                </tr>
                @if (rolesEditingUserId() === user.id) {
                  <tr>
                    <td colspan="5">
                      @for (role of roles(); track role.id) {
                        <label class="checkbox">
                          <input
                            type="checkbox"
                            [checked]="selectedUserRoleIds().includes(role.id)"
                            (change)="toggleUserRole(role.id, $event)">
                          {{ role.name }}
                        </label>
                      }
                      <button type="button" (click)="saveUserRoles(user.id)">Save roles</button>
                      <button type="button" class="secondary" (click)="rolesEditingUserId.set(null)">Cancel</button>
                    </td>
                  </tr>
                }
              } @empty {
                <tr><td colspan="5">No users found.</td></tr>
              }
            </tbody>
          </table>
        </div>
      </section>

      <section aria-labelledby="roles-title">
        <h2 id="roles-title">Roles and permissions</h2>
        @if (auth.hasPermission('USER_UPDATE')) {
        <form class="inline-form" (ngSubmit)="createRole()">
          <label>
            New role (for example ROLE_SUPPORT)
            <input name="roleName" [(ngModel)]="roleName" required pattern="ROLE_[A-Z][A-Z0-9_]*">
          </label>
          <button type="submit">Create role</button>
        </form>
        }

        <label>
          Role
          <select
            name="selectedRole"
            [ngModel]="selectedRoleId()"
            (ngModelChange)="selectRole($event)">
            <option [ngValue]="null">Select a role</option>
            @for (role of roles(); track role.id) {
              <option [ngValue]="role.id">{{ role.name }}</option>
            }
          </select>
        </label>
        @if (selectedRoleId() !== null) {
          <fieldset>
            <legend>Permissions to assign</legend>
            @for (permission of permissions(); track permission.id) {
              <label class="checkbox">
                <input
                  type="checkbox"
                  [checked]="selectedPermissionIds().includes(permission.id)"
                  (change)="togglePermission(permission.id, $event)">
                {{ permission.name }}
              </label>
            } @empty {
              <p>No permissions are configured.</p>
            }
          </fieldset>
          @if (auth.hasPermission('USER_UPDATE')) {
            <button type="button" (click)="savePermissions()">Save permissions</button>
          }
        }
      </section>

      <section aria-labelledby="permissions-title">
        <h2 id="permissions-title">Available permissions</h2>
        <ul>
          @for (permission of permissions(); track permission.id) {
            <li>{{ permission.name }}</li>
          } @empty {
            <li>No permissions are configured.</li>
          }
        </ul>
      </section>
    </main>
  `,
  styles: [`
    :host { display: block; }
    .content-card { max-width: 64rem; margin: 2rem auto; padding: 2rem; background: white; border-radius: .75rem; box-shadow: 0 8px 30px #17255418; }
    section { margin-top: 2.5rem; }
    form { display: grid; grid-template-columns: repeat(auto-fit, minmax(13rem, 1fr)); gap: 1rem; align-items: end; }
    label { display: grid; gap: .35rem; font-weight: 600; }
    input, select { box-sizing: border-box; width: 100%; padding: .6rem; border: 1px solid #cbd5e1; border-radius: .375rem; }
    button { padding: .6rem .8rem; border: 0; border-radius: .375rem; color: white; background: #1d4ed8; cursor: pointer; }
    .secondary { color: #1e293b; background: #e2e8f0; }
    .danger { background: #b91c1c; }
    .actions { display: flex; flex-wrap: wrap; gap: .5rem; }
    .table-wrap { overflow-x: auto; margin-top: 1.25rem; }
    table { width: 100%; border-collapse: collapse; }
    th, td { padding: .7rem; border-bottom: 1px solid #e2e8f0; text-align: left; }
    .inline-form { margin-bottom: 1rem; }
    fieldset { display: flex; flex-wrap: wrap; gap: 1rem; margin: 1rem 0; border: 1px solid #cbd5e1; border-radius: .375rem; }
    .checkbox { display: flex; align-items: center; gap: .4rem; }
    .checkbox input { width: auto; }
    .error { padding: .75rem; color: #991b1b; background: #fee2e2; border-radius: .375rem; }
  `]
})
export class AdminUsersComponent implements OnInit {
  private readonly adminApi = inject(AdminApi);

  readonly auth = inject(AuthService);
  readonly users = signal<AdminUser[]>([]);
  readonly roles = signal<AdminRole[]>([]);
  readonly permissions = signal<AdminPermission[]>([]);
  readonly editingUserId = signal<number | null>(null);
  readonly selectedRoleId = signal<number | null>(null);
  readonly selectedPermissionIds = signal<number[]>([]);
  readonly rolesEditingUserId = signal<number | null>(null);
  readonly selectedUserRoleIds = signal<number[]>([]);
  readonly error = signal<string | null>(null);

  userForm = { username: '', email: '', password: '' };
  roleName = '';

  ngOnInit(): void {
    this.loadData();
  }

  saveUser(): void {
    this.error.set(null);
    const userId = this.editingUserId();
    const request = userId === null
      ? this.adminApi.createUser(this.userForm)
      : this.adminApi.updateUser(userId, {
          username: this.userForm.username,
          email: this.userForm.email
        });

    request.subscribe({
      next: () => {
        this.cancelEdit();
        this.loadUsers();
      },
      error: (error: unknown) => this.error.set(this.errorMessage(error))
    });
  }

  editUser(user: AdminUser): void {
    this.editingUserId.set(user.id);
    this.userForm = { username: user.username, email: user.email, password: '' };
  }

  cancelEdit(): void {
    this.editingUserId.set(null);
    this.userForm = { username: '', email: '', password: '' };
  }

  toggleEnabled(user: AdminUser): void {
    this.error.set(null);
    this.adminApi.setUserEnabled(user.id, !user.enabled).subscribe({
      next: () => this.loadUsers(),
      error: (error: unknown) => this.error.set(this.errorMessage(error))
    });
  }

  editRoles(user: AdminUser): void {
    this.rolesEditingUserId.set(user.id);
    this.selectedUserRoleIds.set(
      this.roles().filter((role) => user.roles.includes(role.name)).map((role) => role.id)
    );
  }

  toggleUserRole(roleId: number, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    this.selectedUserRoleIds.update((ids) =>
      checked ? [...ids, roleId] : ids.filter((id) => id !== roleId)
    );
  }

  saveUserRoles(userId: number): void {
    this.error.set(null);
    this.adminApi.assignRoles(userId, this.selectedUserRoleIds()).subscribe({
      next: () => {
        this.rolesEditingUserId.set(null);
        this.loadUsers();
      },
      error: (error: unknown) => this.error.set(this.errorMessage(error))
    });
  }

  deleteUser(user: AdminUser): void {
    if (!window.confirm(`Delete ${user.username}?`)) {
      return;
    }
    this.error.set(null);
    this.adminApi.deleteUser(user.id).subscribe({
      next: () => this.loadUsers(),
      error: (error: unknown) => this.error.set(this.errorMessage(error))
    });
  }

  createRole(): void {
    this.error.set(null);
    this.adminApi.createRole(this.roleName).subscribe({
      next: (role) => {
        this.roleName = '';
        this.roles.update((roles) => [...roles, role]);
        this.selectRole(role.id);
      },
      error: (error: unknown) => this.error.set(this.errorMessage(error))
    });
  }

  selectRole(roleId: number | null): void {
    this.selectedRoleId.set(roleId);
    const role = this.roles().find((item) => item.id === roleId);
    this.selectedPermissionIds.set(role?.permissions.map((permission) => permission.id) ?? []);
  }

  togglePermission(permissionId: number, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    this.selectedPermissionIds.update((ids) =>
      checked ? [...ids, permissionId] : ids.filter((id) => id !== permissionId)
    );
  }

  savePermissions(): void {
    const roleId = this.selectedRoleId();
    if (roleId === null) {
      return;
    }
    this.error.set(null);
    this.adminApi.assignPermissions(roleId, this.selectedPermissionIds()).subscribe({
      next: (updatedRole) => {
        this.roles.update((roles) =>
          roles.map((role) => role.id === updatedRole.id ? updatedRole : role)
        );
        this.selectRole(updatedRole.id);
      },
      error: (error: unknown) => this.error.set(this.errorMessage(error))
    });
  }

  private loadData(): void {
    this.error.set(null);
    forkJoin({
      users: this.adminApi.users(),
      roles: this.adminApi.roles(),
      permissions: this.adminApi.permissions()
    }).subscribe({
      next: ({ users, roles, permissions }) => {
        this.users.set(users);
        this.roles.set(roles);
        this.permissions.set(permissions);
        if (roles.length > 0) {
          this.selectRole(roles[0].id);
        }
      },
      error: (error: unknown) => this.error.set(this.errorMessage(error))
    });
  }

  private loadUsers(): void {
    this.adminApi.users().subscribe({
      next: (users) => this.users.set(users),
      error: (error: unknown) => this.error.set(this.errorMessage(error))
    });
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string') {
      return error.error.message;
    }
    return 'The requested admin action could not be completed.';
  }
}
