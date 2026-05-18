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
  workspaceName?: string;
  workspaceSlug?: string;
  email: string;
  fullName: string;
  role: Role;
  createdAt: string;
  isActive?: boolean;
  emailVerified?: boolean;
  avatarColor?: string;
}

export interface AuthResponse {
  token: string;
  refreshToken: string;
  user: AuthUser;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
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

export interface ValidateInviteResponse {
  email: string;
  workspaceName: string;
  role: Role;
}

export interface AcceptInviteRequest {
  token: string;
  fullName: string;
  password: string;
  confirmPassword: string;
}

export interface ApiErrorBody {
  status: number;
  message: string;
  errorType?: AuthErrorType;
  timestamp?: string;
  error?: string;
  path?: string;
}

export interface WorkspaceInfo {
  id: string;
  name: string;
  slug: string;
  voteQuorum: number;
  quorumMode: 'AUTO' | 'MANUAL';
  memberCount: number;
  createdAt: string;
}

export interface UpdateWorkspaceRequest {
  name: string;
  slug: string;
  voteQuorum: number;
  quorumMode: 'AUTO' | 'MANUAL';
}

export interface NotificationPreferences {
  emailOnReview: boolean;
  emailOnVote: boolean;
  emailOnStatus: boolean;
  slackEnabled: boolean;
  slackWebhook?: string | null;
}
