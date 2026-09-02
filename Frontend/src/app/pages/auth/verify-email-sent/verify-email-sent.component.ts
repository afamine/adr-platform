import { CommonModule } from '@angular/common';
import { Component, OnDestroy, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../services/auth.service';
import { AxiomLogoComponent } from '../../../shared/axiom-logo/axiom-logo.component';

const STATUS_TOKEN_KEY = 'adr_verification_status_token';
const STATUS_POLL_INTERVAL_MS = 5000;

@Component({
  selector: 'app-verify-email-sent',
  standalone: true,
  imports: [CommonModule, RouterLink, AxiomLogoComponent],
  templateUrl: './verify-email-sent.component.html',
  styleUrls: ['./verify-email-sent.component.scss']
})
export class VerifyEmailSentComponent implements OnDestroy {
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private statusPollTimer: ReturnType<typeof setInterval> | null = null;
  private redirectTimer: ReturnType<typeof setTimeout> | null = null;
  private readonly verificationStatusToken: string | null;

  readonly email: string;
  readonly resending = signal(false);
  readonly resendSuccess = signal(false);
  readonly splashDone = signal(false);
  readonly verifiedElsewhere = signal(false);

  constructor() {
    const state = this.router.getCurrentNavigation()?.extras.state ?? history.state ?? {};
    this.email = (state['email'] as string | undefined) ?? this.auth.getCurrentUser()?.email ?? '';
    this.verificationStatusToken = (state['verificationStatusToken'] as string | undefined)
      ?? sessionStorage.getItem(STATUS_TOKEN_KEY);
    if (this.verificationStatusToken) {
      sessionStorage.setItem(STATUS_TOKEN_KEY, this.verificationStatusToken);
      this.checkVerificationStatus();
      this.statusPollTimer = setInterval(() => this.checkVerificationStatus(), STATUS_POLL_INTERVAL_MS);
    }
  }

  ngOnDestroy(): void {
    if (this.statusPollTimer) clearInterval(this.statusPollTimer);
    if (this.redirectTimer) clearTimeout(this.redirectTimer);
  }

  onSplashComplete(): void { this.splashDone.set(true); }

  resendEmail(): void {
    if (!this.email || this.resending() || this.resendSuccess()) return;
    this.resending.set(true);
    this.auth.resendVerification(this.email).subscribe({
      next: () => { this.resending.set(false); this.resendSuccess.set(true); },
      error: () => { this.resending.set(false); this.resendSuccess.set(true); }
    });
  }

  private checkVerificationStatus(): void {
    if (!this.verificationStatusToken || this.verifiedElsewhere()) return;
    this.auth.getEmailVerificationStatus(this.verificationStatusToken).subscribe({
      next: ({ verified }) => {
        if (!verified) return;
        this.verifiedElsewhere.set(true);
        if (this.statusPollTimer) clearInterval(this.statusPollTimer);
        sessionStorage.removeItem(STATUS_TOKEN_KEY);
        this.redirectTimer = setTimeout(() => void this.router.navigate(['/login']), 1200);
      },
      error: () => { /* Invalid/expired token leaves the manual sign-in path available. */ }
    });
  }
}