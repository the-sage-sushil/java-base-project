export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface LoginResponse {
  jwt: string;
  username: string;
  expiresIn: number;
}
export interface ProfileResponse {
  username: string;
  email: string;
}

export interface AuthUser {
  username: string;
  roles: string[];
  permissions: string[];
}