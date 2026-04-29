import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiErrorBody } from '../../../models/auth.models';
import { AuthService } from '../../../services/auth.service';
import { AxiomLogoComponent } from '../../../shared/axiom-logo/axiom-logo.component';

export type VerifyState = 'LOADING' | 'SUCCESS' | 'ERROR_EXPIRED' | 'ERROR_INVALID';

const REDIRECT_DELAY_SECONDS = 5;

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, AxiomLogoComponent],
  templateUrl: './verify-email.component.html',
  styleUrls: ['./verify-email.component.scss']
})
export class VerifyEmailComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  readonly state = signal<VerifyState>('LOADING');
  readonly showResendForm = signal(false);
  readonly resending = signal(false);
  readonly resendNotice = signal<string | null>(null);
  readonly redirectSeconds = signal(REDIRECT_DELAY_SECONDS);

  private countdownIntervalId: ReturnType<typeof setInterval> | null = null;
  private redirectTimeoutId: ReturnType<typeof setTimeout> | null = null;

  readonly resendForm: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]]
  });

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');

    if (!token) {
      this.state.set('ERROR_INVALID');
      return;
    }

    this.state.set('LOADING');
    this.auth.verifyEmail(token).subscribe({
      next: () => {
        this.state.set('SUCCESS');
        this.startRedirectCountdown();
      },
      error: (err) => {
        const body = err?.error as ApiErrorBody | undefined;
        this.state.set(body?.errorType === 'EXPIRED' ? 'ERROR_EXPIRED' : 'ERROR_INVALID');
      }
    });
  }

  ngOnDestroy(): void {
    this.clearRedirectTimers();
  }

  private startRedirectCountdown(): void {
    this.redirectSeconds.set(REDIRECT_DELAY_SECONDS);

    this.countdownIntervalId = setInterval(() => {
      const next = this.redirectSeconds() - 1;
      this.redirectSeconds.set(next > 0 ? next : 0);
    }, 1000);

    this.redirectTimeoutId = setTimeout(() => {
      this.clearRedirectTimers();
      this.router.navigate(['/login']);
    }, REDIRECT_DELAY_SECONDS * 1000);
  }

  private clearRedirectTimers(): void {
    if (this.countdownIntervalId !== null) {
      clearInterval(this.countdownIntervalId);
      this.countdownIntervalId = null;
    }

    if (this.redirectTimeoutId !== null) {
      clearTimeout(this.redirectTimeoutId);
      this.redirectTimeoutId = null;
    }
  }

  openResendForm(): void {
    this.showResendForm.set(true);
    this.resendNotice.set(null);
  }

  resendVerification(): void {
    if (this.resendForm.invalid || this.resending()) {
      this.resendForm.markAllAsTouched();
      return;
    }

    const email = this.resendForm.value.email as string;
    this.resending.set(true);
    this.resendNotice.set(null);

    this.auth.resendVerification(email).subscribe({
      next: (res) => {
        this.resending.set(false);
        this.resendNotice.set(res?.message || 'A new verification link has been sent to your email.');
      },
      error: (err) => {
        this.resending.set(false);
        const body = err?.error as ApiErrorBody | undefined;
        if (err?.status === 400 && body?.message?.toLowerCase().includes('already verified')) {
          this.resendNotice.set('Email already verified — please sign in.');
          return;
        }
        this.resendNotice.set('A new verification link has been sent to your email.');
      }
    });
  }

  goToLogin(): void {
    this.clearRedirectTimers();
    this.router.navigate(['/login']);
  }
}
