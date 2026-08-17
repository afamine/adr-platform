import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../services/auth.service';
import { ApiErrorBody, LoginRequest, AuthResponse } from '../../../models/auth.models';
import { TotpService } from '../../../services/totp.service';
import { AxiomLogoComponent } from '../../../shared/axiom-logo/axiom-logo.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, MatIconModule, AxiomLogoComponent],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly totp = inject(TotpService);

  passwordVisible = signal(false);
  isLoading = signal(false);
  submitted = signal(false);
  errorMessage = signal<string | null>(null);
  emailNotVerified = signal(false);
  resending = signal(false);
  resendNotice = signal<string | null>(null);

  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]]
  });

  togglePasswordVisibility(): void {
    this.passwordVisible.update(v => !v);
  }

  onSubmit(): void {
    this.submitted.set(true);
    this.errorMessage.set(null);
    this.emailNotVerified.set(false);
    this.resendNotice.set(null);

    if (this.form.invalid || this.isLoading()) {
      this.form.markAllAsTouched();
      return;
    }

    const request: LoginRequest = this.form.getRawValue() as LoginRequest;

    this.isLoading.set(true);
    this.auth.login(request).subscribe({
      next: (response: AuthResponse) => {
        this.isLoading.set(false);

        // NEW: Handle 2FA requirement
        if (response.requiresTwoFactor && response.pendingToken) {
          this.totp.savePendingToken(response.pendingToken);
          this.router.navigate(['/2fa-verify']);
          return;
        }

        if (!response.user) {
          this.errorMessage.set('Invalid authentication response. Please try again.');
          return;
        }

        this.auth.saveTokens(response);

        if (response.requiresTwoFactorSetup || response.user.totpSetupRequired) {
          this.router.navigate(['/setup-2fa']);
          return;
        }

        // Redirect based on role
        if (response.user.role === 'ADMIN') {
          this.router.navigate(['/admin/users']);
        } else {
          this.router.navigate(['/adrs']);
        }
      },
      error: (err) => {
        this.isLoading.set(false);
        const body = err?.error as ApiErrorBody | undefined;
        const errorCode = body?.errorType || body?.error;

        if (err.status === 403 && (errorCode === 'ACCOUNT_DISABLED' || errorCode === 'ACCOUNT_DEACTIVATED')) {
          this.emailNotVerified.set(false);
          this.errorMessage.set(body?.message || 'Your account is disabled. Please contact your administrator.');
          return;
        }

        if (err.status === 403 && errorCode === 'EMAIL_NOT_VERIFIED') {
          this.emailNotVerified.set(true);
          this.errorMessage.set('Please verify your email address before signing in.');
          return;
        }

        if (err.status === 401) {
          this.errorMessage.set('Invalid email or password. Please try again.');
          return;
        }

        if (err.status === 400) {
          this.errorMessage.set(body?.message || 'Please check your information.');
          return;
        }

        if (err.status === 0) {
          this.errorMessage.set('Unable to connect to server. Please try again.');
          return;
        }

        this.errorMessage.set(body?.message || 'Sign-in failed. Please try again.');
      },
      complete: () => {
        this.isLoading.set(false);
      }
    });
  }

  resendVerification(): void {
    const email = this.form.controls.email.value;
    if (!email || this.resending()) {
      return;
    }

    this.resending.set(true);
    this.resendNotice.set(null);

    this.auth.resendVerification(email).subscribe({
      next: () => {
        this.resending.set(false);
        this.resendNotice.set('A new verification link has been sent to your email.');
      },
      error: (err) => {
        this.resending.set(false);
        const body = err?.error as ApiErrorBody | undefined;
        if (err.status === 400 && body?.message?.toLowerCase().includes('already verified')) {
          this.emailNotVerified.set(false);
          this.resendNotice.set('Email already verified — please sign in.');
          return;
        }
        this.resendNotice.set('A new verification link has been sent to your email.');
      }
    });
  }

  // Helpers for template
  hasError(control: 'email' | 'password', error: string): boolean {
    const c = this.form.controls[control];
    if (!c) return false;
    const show = c.touched || this.submitted();
    return !!(show && c.hasError(error));
  }
}
