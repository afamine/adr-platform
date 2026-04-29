import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../services/auth.service';
import { AxiomLogoComponent } from '../../../shared/axiom-logo/axiom-logo.component';

@Component({
  selector: 'app-verify-email-sent',
  standalone: true,
  imports: [CommonModule, RouterLink, AxiomLogoComponent],
  templateUrl: './verify-email-sent.component.html',
  styleUrls: ['./verify-email-sent.component.scss']
})
export class VerifyEmailSentComponent {
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  readonly email: string;
  readonly resending = signal(false);
  readonly resendSuccess = signal(false);
  readonly splashDone = signal(false);

  onSplashComplete(): void {
    this.splashDone.set(true);
  }

  constructor() {
    const stateEmail = this.router.getCurrentNavigation()?.extras.state?.['email'] as string | undefined;
    const historyEmail = (typeof history !== 'undefined' && history.state)
      ? (history.state['email'] as string | undefined)
      : undefined;
    this.email = stateEmail ?? historyEmail ?? this.auth.getCurrentUser()?.email ?? '';
  }

  resendEmail(): void {
    if (!this.email || this.resending() || this.resendSuccess()) {
      return;
    }

    this.resending.set(true);
    this.auth.resendVerification(this.email).subscribe({
      next: () => {
        this.resending.set(false);
        this.resendSuccess.set(true);
      },
      error: () => {
        this.resending.set(false);
        this.resendSuccess.set(true);
      }
    });
  }
}
