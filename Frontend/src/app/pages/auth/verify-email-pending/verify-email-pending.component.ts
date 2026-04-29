import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ApiErrorBody } from '../../../models/auth.models';
import { AuthService } from '../../../services/auth.service';

interface VerifyEmailPendingState {
  maskedEmail?: string;
  email?: string;
}

@Component({
  selector: 'app-verify-email-pending',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './verify-email-pending.component.html',
  styleUrls: ['./verify-email-pending.component.scss']
})
export class VerifyEmailPendingComponent {
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  readonly maskedEmail = signal<string>('');
  readonly resending = signal(false);
  readonly notice = signal<string | null>(null);

  private readonly rawEmail: string;

  constructor() {
    const navState = (this.router.getCurrentNavigation()?.extras?.state ??
      (history.state as VerifyEmailPendingState)) as VerifyEmailPendingState | undefined;

    this.maskedEmail.set(navState?.maskedEmail || 'your email');
    this.rawEmail = navState?.email || '';

    if (!navState?.email) {
      // Direct visit (refresh, deep link) — no context: send the user back.
      this.router.navigate(['/login']);
    }
  }

  resend(): void {
    if (!this.rawEmail || this.resending()) {
      return;
    }
    this.resending.set(true);
    this.notice.set(null);

    this.auth.resendVerification(this.rawEmail).subscribe({
      next: () => {
        this.resending.set(false);
        this.notice.set('A new verification link has been sent to your email.');
      },
      error: (err) => {
        this.resending.set(false);
        const body = err?.error as ApiErrorBody | undefined;
        if (err?.status === 400 && body?.message?.toLowerCase().includes('already verified')) {
          this.notice.set('Email already verified — please sign in.');
          return;
        }
        this.notice.set('A new verification link has been sent to your email.');
      }
    });
  }
}
