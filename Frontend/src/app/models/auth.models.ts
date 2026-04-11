export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthUser {
  id: string;
  email: string;
  fullName: string;
  role: 'AUTHOR' | 'REVIEWER' | 'APPROVER' | 'ADMIN';
  workspaceId: string;
}

export interface AuthResponse {
  token: string;
  refreshToken: string;
  user: AuthUser;
}
