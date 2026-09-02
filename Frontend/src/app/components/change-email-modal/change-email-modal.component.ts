import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-change-email-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './change-email-modal.component.html',
  styleUrls: ['../change-password-modal/change-password-modal.component.scss']
})
export class ChangeEmailModalComponent {
  @Input() totpEnabled = false;
  @Output() closed = new EventEmitter<void>();
  @Output() requested = new EventEmitter<void>();

  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);

  readonly form = this.fb.group({
    newEmail: ['', [Validators.required, Validators.email]],
    currentPassword: ['', [Validators.required]],
    totpCode: ['']
  });

  showPassword = false;
  isLoading = false;
  errorMessage = '';
  successMessage = '';

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const totpCode = (this.form.value.totpCode ?? '').trim();
    if (this.totpEnabled && !/^\d{6}$/.test(totpCode)) {
      this.errorMessage = 'Enter the six-digit code from your authenticator app.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.authService.requestEmailChange({
      newEmail: (this.form.value.newEmail ?? '').trim(),
      currentPassword: this.form.value.currentPassword ?? '',
      totpCode: totpCode || undefined
    }).subscribe({
      next: (response) => {
        this.isLoading = false;
        this.successMessage = response.message || 'Check your new inbox for a confirmation link.';
        this.requested.emit();
      },
      error: (error) => {
        this.isLoading = false;
        this.errorMessage = error?.error?.message || 'Unable to start the email change. Please try again.';
      }
    });
  }

  onClose(): void {
    if (!this.isLoading) {
      this.closed.emit();
    }
  }
}
