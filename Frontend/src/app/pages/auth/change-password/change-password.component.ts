import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../services/auth.service';
import { NotificationService } from '../../../services/notification.service';

const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d).+$/;

const matchPasswordsValidator: ValidatorFn = (group): ValidationErrors | null => {
  const password = group.get('newPassword')?.value;
  const confirmPassword = group.get('confirmPassword')?.value;

  if (!confirmPassword) {
    return null;
  }

  return password === confirmPassword ? null : { passwordMismatch: true };
};

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, MatIconModule],
  templateUrl: './change-password.component.html',
  styleUrls: ['./change-password.component.scss']
})
export class ChangePasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly notif = inject(NotificationService);

  readonly isLoading = signal(false);
  readonly success = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly currentPasswordVisible = signal(false);
  readonly newPasswordVisible = signal(false);
  readonly confirmPasswordVisible = signal(false);

  readonly form = this.fb.group(
    {
      currentPassword: ['', [Validators.required, Validators.minLength(8)]],
      newPassword: ['', [Validators.required, Validators.minLength(8), Validators.pattern(passwordPattern)]],
      confirmPassword: ['', [Validators.required]]
    },
    { validators: [matchPasswordsValidator] }
  );

  constructor() {
    this.form.controls.newPassword.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.form.controls.confirmPassword.updateValueAndValidity({ emitEvent: false });
        this.form.updateValueAndValidity({ emitEvent: false });
      });
  }

  toggleCurrentPasswordVisibility(): void {
    this.currentPasswordVisible.update((v) => !v);
  }

  toggleNewPasswordVisibility(): void {
    this.newPasswordVisible.update((v) => !v);
  }

  toggleConfirmPasswordVisibility(): void {
    this.confirmPasswordVisible.update((v) => !v);
  }

  onSubmit(): void {
    this.errorMessage.set(null);

    if (this.form.invalid || this.isLoading()) {
      this.form.markAllAsTouched();
      if (this.form.hasError('passwordMismatch')) {
        this.notif.error('Erreur de validation', 'Les mots de passe ne correspondent pas.');
      }
      return;
    }

    const value = this.form.getRawValue();

    this.isLoading.set(true);
    this.auth
      .changePassword({
        currentPassword: value.currentPassword ?? '',
        newPassword: value.newPassword ?? ''
      })
      .subscribe({
        next: () => {
          this.isLoading.set(false);
          this.success.set(true);
          this.notif.success('Mot de passe modifié', 'Vous serez déconnecté dans 3 secondes.');
          this.form.disable({ emitEvent: false });

          setTimeout(() => {
            this.auth.logout();
          }, 3000);
        },
        error: (err) => {
          this.isLoading.set(false);
          const msg = err?.error?.message;
          this.errorMessage.set(msg || 'Unable to change password. Please try again.');
          if (err?.status === 400 || err?.status === 401) {
            this.notif.error('Mot de passe incorrect', 'Le mot de passe actuel saisi est incorrect.');
          }
        }
      });
  }

  hasError(
    control: 'currentPassword' | 'newPassword' | 'confirmPassword',
    error: string
  ): boolean {
    const c = this.form.controls[control];
    if (!c) return false;
    const show = c.touched || c.dirty;

    if (error === 'passwordMismatch') {
      return show && this.form.hasError('passwordMismatch');
    }

    return !!(show && c.hasError(error));
  }

  goBack(): void {
    this.router.navigate(['/adrs']);
  }
}
