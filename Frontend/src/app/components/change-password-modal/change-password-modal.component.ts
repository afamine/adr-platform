import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, EventEmitter, Output, inject } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { environment } from '../../../environments/environment';

function passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
  const newPwd = group.get('newPassword')?.value;
  const confirm = group.get('confirmPassword')?.value;

  if (newPwd && confirm && newPwd !== confirm) {
    group.get('confirmPassword')?.setErrors({ mismatch: true });
    return { mismatch: true };
  }

  if (group.get('confirmPassword')?.errors?.['mismatch']) {
    group.get('confirmPassword')?.setErrors(null);
  }

  return null;
}

@Component({
  selector: 'app-change-password-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './change-password-modal.component.html',
  styleUrls: ['./change-password-modal.component.scss']
})
export class ChangePasswordModalComponent {
  @Output() closed = new EventEmitter<void>();

  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  form = this.fb.group(
    {
      currentPassword: ['', [Validators.required]],
      newPassword: [
        '',
        [
          Validators.required,
          Validators.minLength(8),
          Validators.pattern(/^(?=.*[a-zA-Z])(?=.*[0-9]).{8,}$/)
        ]
      ],
      confirmPassword: ['', [Validators.required]]
    },
    { validators: passwordMatchValidator }
  );

  showCurrentPwd = false;
  showNewPwd = false;
  showConfirmPwd = false;
  isLoading = false;
  errorMessage = '';
  successMessage = '';

  get newPwdValue(): string {
    return this.form.get('newPassword')?.value || '';
  }

  get strengthLevel(): 'none' | 'weak' | 'medium' | 'strong' {
    const pwd = this.newPwdValue;
    if (!pwd) return 'none';

    const hasLetter = /[a-zA-Z]/.test(pwd);
    const hasNumber = /[0-9]/.test(pwd);
    const hasSpecial = /[^a-zA-Z0-9]/.test(pwd);

    if (pwd.length >= 12 && hasLetter && hasNumber && hasSpecial) return 'strong';
    if (pwd.length >= 8 && hasLetter && hasNumber) return 'medium';
    return 'weak';
  }

  get filledBars(): number {
    return { none: 0, weak: 1, medium: 2, strong: 3 }[this.strengthLevel];
  }

  get strengthColor(): string {
    return { none: '', weak: '#ef4444', medium: '#f59e0b', strong: '#1d9e75' }[this.strengthLevel];
  }

  get strengthLabel(): string {
    return { none: '', weak: 'Weak', medium: 'Medium', strong: 'Strong' }[this.strengthLevel];
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    const { currentPassword, newPassword, confirmPassword } = this.form.value;
    const url = `${environment.apiUrl}/api/auth/change-password`;

    this.http
      .put<{ message: string }>(url, { currentPassword, newPassword, confirmPassword })
      .subscribe({
        next: (res) => {
          this.isLoading = false;
          this.successMessage = res.message || 'Password updated successfully!';
          setTimeout(() => {
            this.authService.logout();
            this.closed.emit();
          }, 2000);
        },
        error: (err) => {
          this.isLoading = false;
          this.errorMessage = err.error?.message || 'Failed to update password. Please try again.';
        }
      });
  }

  onClose(): void {
    if (!this.isLoading) {
      this.closed.emit();
    }
  }

  onForgotPassword(): void {
    this.closed.emit();
    this.router.navigate(['/forgot-password']);
  }
}
