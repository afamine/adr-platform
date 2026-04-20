import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../services/auth.service';

const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d).+$/;

const matchPasswordsValidator: ValidatorFn = (group): ValidationErrors | null => {
  const password = group.get('password')?.value;
  const confirmPassword = group.get('confirmPassword')?.value;

  if (!confirmPassword) {
    return null;
  }

  return password === confirmPassword ? null : { passwordMismatch: true };
};

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, MatIconModule],
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.scss']
})
export class ResetPasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  resetForm: FormGroup;
  isLoading = false;
  token: string | null = null;
  tokenValid = true;
  successMessage = '';
  errorMessage = '';

  constructor() {
    this.token = this.route.snapshot.queryParamMap.get('token');
    if (!this.token) {
      this.tokenValid = false;
    }

    this.resetForm = this.fb.group(
      {
        password: ['', [Validators.required, Validators.minLength(8), Validators.pattern(passwordPattern)]],
        confirmPassword: ['', [Validators.required]]
      },
      { validators: [matchPasswordsValidator] }
    );
  }

  onSubmit(): void {
    if (!this.token) {
      this.tokenValid = false;
      this.errorMessage = 'This reset link is invalid or has expired.';
      return;
    }

    if (this.resetForm.invalid || this.isLoading) {
      this.resetForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    const newPassword = this.resetForm.get('password')?.value as string;

    this.auth.resetPassword(this.token, newPassword).subscribe({
      next: () => {
        this.isLoading = false;
        this.successMessage = 'Your password has been reset successfully. You can now sign in.';
        this.resetForm.disable();
        setTimeout(() => this.router.navigate(['/login']), 2000);
      },
      error: (err) => {
        this.isLoading = false;
        this.tokenValid = false;
        this.errorMessage = err?.error?.message || 'This reset link is invalid or has expired.';
      }
    });
  }

  hasError(control: 'password' | 'confirmPassword', error: string): boolean {
    const c = this.resetForm.controls[control];
    if (!c) return false;
    const show = c.touched || c.dirty;

    if (error === 'passwordMismatch') {
      return show && this.resetForm.hasError('passwordMismatch');
    }

    return !!(show && c.hasError(error));
  }
}
