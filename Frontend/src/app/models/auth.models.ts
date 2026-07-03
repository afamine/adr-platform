export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
  workspaceMode?: 'PRIVATE' | 'JOIN_TEAM';
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
  totpSetupRequired?: boolean;
}

export interface AuthResponse {
  token?: string;
  refreshToken?: string;
  user?: AuthUser;
  requiresTwoFactor?: boolean;
  requiresTwoFactorSetup?: boolean;
  pendingToken?: string;
}

export interface InviteUserResponse {
  message: string;
  inviteLink: string;
  invitation: WorkspaceInvitation;
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

export type AuthErrorType = 'EXPIRED' | 'INVALID' | 'EMAIL_NOT_VERIFIED' | 'ACCOUNT_DISABLED' | 'ACCOUNT_DEACTIVATED' | string;

export interface ValidateInviteResponse {
  email: string;
  workspaceName: string;
  role: Role;
  existingAccount: boolean;
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
  joinPolicy: WorkspaceJoinPolicy;
  memberCount: number;
  createdAt: string;
}

export type WorkspaceJoinPolicy = 'INVITE_ONLY' | 'ALLOW_SLUG' | 'CLOSED';

export interface UpdateWorkspaceRequest {
  name: string;
  slug: string;
  voteQuorum: number;
  quorumMode: 'AUTO' | 'MANUAL';
  joinPolicy: WorkspaceJoinPolicy;
}

export interface WorkspaceSlugStatus {
  slug: string;
  exists: boolean;
  workspaceName?: string | null;
  joinPolicy?: WorkspaceJoinPolicy | null;
  canJoinBySlug: boolean;
  message: string;
}

export type InvitationStatus = 'PENDING' | 'ACCEPTED' | 'EXPIRED';

export interface WorkspaceInvitation {
  tokenId: string;
  userId: string;
  email: string;
  role: Role;
  status: InvitationStatus;
  createdAt: string;
  expiresAt: string;
}

export interface WorkspaceMembership {
  workspaceId: string;
  workspaceName: string;
  workspaceSlug: string;
  role: Role;
  current: boolean;
  joinedAt: string;
}

export interface NotificationPreferences {
  emailOnReview: boolean;
  emailOnVote: boolean;
  emailOnStatus: boolean;
  slackEnabled: boolean;
  slackWebhook?: string | null;
}

export interface TotpSetupResponse {
  qrCodeBase64: string;
  secret: string;
}

export interface TotpEnableRequest {
  code: string;
}

export interface TotpVerifyRequest {
  pendingToken: string;
  code: string;
}

export interface TotpDisableRequest {
  code: string;
}

export interface TotpStatusResponse {
  enabled: boolean;
}
