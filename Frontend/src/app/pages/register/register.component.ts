import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AuthService } from '../../services/auth.service';
import { RegisterRequest, RegisterResponse } from '../../models/auth.models';
import { NotificationService } from '../../services/notification.service';

const slugPattern = /^[a-z0-9-]+$/;
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
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatIconModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly notif = inject(NotificationService);

  protected readonly hidePassword = signal(true);
  protected readonly hideConfirmPassword = signal(true);
  protected readonly submitted = signal(false);
  protected readonly isLoading = signal(false);
  protected readonly successMessage = signal<string | null>(null);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly registerForm = this.fb.group(
    {
      fullName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8), Validators.pattern(passwordPattern)]],
      confirmPassword: ['', [Validators.required]],
      workspaceSlug: ['', [Validators.pattern(slugPattern)]]
    },
    { validators: [matchPasswordsValidator] }
  );

  constructor() {
    this.registerForm.controls.password.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.registerForm.controls.confirmPassword.updateValueAndValidity({ emitEvent: false });
        this.registerForm.updateValueAndValidity({ emitEvent: false });
      });
  }

  protected onSubmit(): void {
    this.submitted.set(true);
    this.successMessage.set(null);
    this.errorMessage.set(null);

    if (this.registerForm.invalid || this.isLoading()) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);

    const payload = this.registerForm.getRawValue();
    const request: RegisterRequest = {
      fullName: payload.fullName ?? '',
      email: payload.email ?? '',
      password: payload.password ?? '',
      workspaceSlug: payload.workspaceSlug?.trim() || undefined
    };

    this.authService
      .register(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (_response: RegisterResponse) => {
          this.isLoading.set(false);
          // Navigate to "check your inbox" page, passing the email
          this.router.navigate(['/verify-email-sent'], {
            state: { email: request.email }
          });
        },
        error: (err) => {
          this.isLoading.set(false);
          if (err.status === 409) {
            this.errorMessage.set('An account with this email already exists.');
            this.notif.warning('Inscription impossible', 'Un compte avec cet email existe déjà.');
            return;
          }

          if (err.status === 400) {
            const errors = err.error?.errors;
            if (errors && Array.isArray(errors) && errors.length > 0) {
              this.errorMessage.set(errors.map((e: any) => e?.message || e).join('. '));
              this.notif.error('Erreur de validation', 'Veuillez vérifier vos informations.');
              return;
            }

            const message = err.error?.message;
            this.errorMessage.set(
              message === 'Email already registered' ? 'An account with this email already exists.' : message || 'Please check your information.'
            );
            this.notif.error('Erreur de validation', 'Veuillez vérifier vos informations.');
            return;
          }

          this.errorMessage.set('Unable to connect to server. Please try again.');
          this.notif.error('Erreur réseau', 'Impossible de contacter le serveur.');
        },
        complete: () => {
          this.isLoading.set(false);
        }
      });
  }

  protected togglePassword(): void {
    this.hidePassword.update((value) => !value);
  }

  protected toggleConfirmPassword(): void {
    this.hideConfirmPassword.update((value) => !value);
  }

  protected controlHasError(
    controlName: 'fullName' | 'email' | 'password' | 'confirmPassword' | 'workspaceSlug',
    errorKey: string
  ): boolean {
    const control = this.registerForm.controls[controlName];
    return !!control && (control.touched || control.dirty || this.submitted()) && control.hasError(errorKey);
  }

  protected get showPasswordMismatch(): boolean {
    const confirmControl = this.registerForm.controls.confirmPassword;
    return (
      (confirmControl.dirty || confirmControl.touched || this.submitted()) &&
      this.registerForm.hasError('passwordMismatch')
    );
  }
}
