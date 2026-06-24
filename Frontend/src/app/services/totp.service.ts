import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  TotpSetupResponse,
  TotpEnableRequest,
  TotpVerifyRequest,
  TotpDisableRequest,
  TotpStatusResponse,
  AuthResponse
} from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class TotpService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = environment.apiUrl;

  /** Get current 2FA status for the authenticated user. */
  getStatus(): Observable<TotpStatusResponse> {
    return this.http.get<TotpStatusResponse>(`${this.API_URL}/api/auth/2fa/status`);
  }

  /** Generate a new secret + QR code. Does NOT enable 2FA yet. */
  setup(): Observable<TotpSetupResponse> {
    return this.http.get<TotpSetupResponse>(`${this.API_URL}/api/auth/2fa/setup`);
  }

  /** Enable 2FA: save secret and verify first code. */
  enable(payload: TotpEnableRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.API_URL}/api/auth/2fa/enable`, payload);
  }

  /** Disable 2FA: requires current valid TOTP code. */
  disable(payload: TotpDisableRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.API_URL}/api/auth/2fa/disable`, payload);
  }

  /** Complete login when 2FA is required: exchange pendingToken + code for full JWT. */
  validateLogin(payload: TotpVerifyRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/api/auth/2fa/validate`, payload);
  }

  /** Store and retrieve the pending 2FA token during login flow (session only). */
  savePendingToken(token: string): void {
    sessionStorage.setItem('adr_pending_2fa', token);
  }

  getPendingToken(): string | null {
    return sessionStorage.getItem('adr_pending_2fa');
  }

  clearPendingToken(): void {
    sessionStorage.removeItem('adr_pending_2fa');
  }
}

