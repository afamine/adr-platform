export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
  workspaceSlug?: string;
}

export enum Role {
  AUTHOR = 'AUTHOR',
  REVIEWER = 'REVIEWER',
  APPROVER = 'APPROVER',
  ADMIN = 'ADMIN'
}

export interface AuthUser {
  id: string;
  workspaceId: string;
  email: string;
  fullName: string;
  role: Role;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  refreshToken: string;
  user: AuthUser;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

export interface RegisterResponse {
  message: string;
  email: string;
}

export interface MessageResponse {
  message: string;
}

export type AuthErrorType = 'EXPIRED' | 'INVALID' | 'EMAIL_NOT_VERIFIED' | string;

export interface ApiErrorBody {
  status: number;
  message: string;
  errorType?: AuthErrorType;
  timestamp?: string;
  error?: string;
  path?: string;
}
