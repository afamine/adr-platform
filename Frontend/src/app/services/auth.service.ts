import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, finalize, Observable, of, take } from 'rxjs';
import {
  AuthResponse,
  AuthUser,
  MessageResponse,
  LoginRequest,
  RefreshTokenRequest,
  RegisterRequest,
  RegisterResponse,
  Role
} from '../models/auth.models';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly API_URL = environment.apiUrl;
  private readonly TOKEN_KEY = 'adr_token';
  private readonly REFRESH_KEY = 'adr_refresh_token';
  private readonly USER_KEY = 'adr_user';
  private isLoggingOut = false;

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/api/auth/login`, request);
  }

  register(request: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${this.API_URL}/api/auth/register`, request);
  }

  forgotPassword(email: string): Observable<void> {
    const payload = { email };
    return this.http.post<void>(`${this.API_URL}/api/auth/forgot-password`, payload);
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    const payload = { token, newPassword };
    return this.http.post<void>(`${this.API_URL}/api/auth/reset-password`, payload);
  }

  changePassword(payload: { currentPassword: string; newPassword: string }): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.API_URL}/api/auth/change-password`, payload);
  }

  verifyEmail(token: string): Observable<MessageResponse> {
    const params = new URLSearchParams({ token }).toString();
    return this.http.get<MessageResponse>(`${this.API_URL}/api/auth/verify-email?${params}`);
  }

  resendVerification(email: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.API_URL}/api/auth/resend-verification`, { email });
  }

  refreshToken(refreshToken: string): Observable<AuthResponse> {
    const payload: RefreshTokenRequest = { refreshToken };
    return this.http.post<AuthResponse>(`${this.API_URL}/api/auth/refresh`, payload);
  }

  saveTokens(response: AuthResponse): void {
    localStorage.setItem(this.TOKEN_KEY, response.token);
    localStorage.setItem(this.REFRESH_KEY, response.refreshToken);
    localStorage.setItem(this.USER_KEY, JSON.stringify(response.user));
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_KEY);
  }

  getCurrentUser(): AuthUser | null {
    const raw = localStorage.getItem(this.USER_KEY);
    try {
      return raw ? (JSON.parse(raw) as AuthUser) : null;
    } catch {
      return null;
    }
  }

  isAuthenticated(): boolean {
    const token = this.getToken();
    if (!token) {
      return false;
    }

    const payload = this.decodeJwtPayload(token);
    if (!payload || typeof payload['exp'] !== 'number') {
      return false;
    }

    return payload['exp'] * 1000 > Date.now();
  }

  hasRole(role: Role): boolean {
    return this.getCurrentUser()?.role === role;
  }

  isAdmin(): boolean {
    return this.getCurrentUser()?.role === Role.ADMIN;
  }

  canApprove(): boolean {
    const r = this.getCurrentUser()?.role;
    return r === Role.APPROVER || r === Role.ADMIN;
  }

  canReview(): boolean {
    const r = this.getCurrentUser()?.role;
    return r === Role.REVIEWER || r === Role.APPROVER || r === Role.ADMIN;
  }

  getUsersInWorkspace(): Observable<AuthUser[]> {
    return this.http.get<AuthUser[]>(`${this.API_URL}/api/users`);
  }

  updateUserRole(
    userId: string,
    role: Role
  ): Observable<{ message: string; userId: string; newRole: string }> {
    return this.http.put<{ message: string; userId: string; newRole: string }>(
      `${this.API_URL}/api/users/${userId}/role`,
      { role }
    );
  }

  logout(): void {
    if (this.isLoggingOut) {
      return;
    }

    this.isLoggingOut = true;
    const token = this.getToken();

    if (!token) {
      this.finishLogout();
      return;
    }

    this.http
      .post<void>(`${this.API_URL}/api/auth/logout`, {}, { headers: new HttpHeaders({ Authorization: `Bearer ${token}` }) })
      .pipe(
        take(1),
        catchError(() => of(null)),
        finalize(() => this.finishLogout())
      )
      .subscribe();
  }

  private decodeJwtPayload(token: string): Record<string, any> | null {
    const parts = token.split('.');
    if (parts.length !== 3) {
      return null;
    }

    try {
      const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      const normalized = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=');
      return JSON.parse(atob(normalized));
    } catch {
      return null;
    }
  }

  private finishLogout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.router.navigateByUrl('/login');
    this.isLoggingOut = false;
  }
}
