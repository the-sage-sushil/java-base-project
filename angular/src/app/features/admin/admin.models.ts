export interface AdminUser {
  id: number;
  username: string;
  email: string;
  enabled: boolean;
  roles: string[];
}

export interface AdminPermission {
  id: number;
  name: string;
}

export interface AdminRole {
  id: number;
  name: string;
  permissions: AdminPermission[];
}
