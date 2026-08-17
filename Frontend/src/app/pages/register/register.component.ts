import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { RegisterRequest, RegisterResponse, WorkspaceSlugStatus } from '../../models/auth.models';
import { NotificationService } from '../../services/notification.service';
import { WorkspaceService } from '../../services/workspace.service';
import { AxiomLogoComponent } from '../../shared/axiom-logo/axiom-logo.component';

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
    AxiomLogoComponent
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly workspaceService = inject(WorkspaceService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly notif = inject(NotificationService);

  protected readonly workspaceMode = signal<'PRIVATE' | 'JOIN_TEAM'>('PRIVATE');
  protected readonly hidePassword = signal(true);
  protected readonly hideConfirmPassword = signal(true);
  protected readonly submitted = signal(false);
  protected readonly isLoading = signal(false);
  protected readonly isCheckingSlug = signal(false);
  protected readonly slugStatus = signal<WorkspaceSlugStatus | null>(null);
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

    this.registerForm.controls.workspaceSlug.valueChanges
      .pipe(debounceTime(350), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.checkWorkspaceSlug());
  }

  protected setWorkspaceMode(mode: 'PRIVATE' | 'JOIN_TEAM'): void {
    this.workspaceMode.set(mode);
    const validators = mode === 'JOIN_TEAM'
      ? [Validators.required, Validators.pattern(slugPattern)]
      : [Validators.pattern(slugPattern)];
    this.registerForm.controls.workspaceSlug.setValidators(validators);
    this.registerForm.controls.workspaceSlug.updateValueAndValidity();
    this.checkWorkspaceSlug();
  }

  protected onSubmit(): void {
    this.submitted.set(true);
    this.successMessage.set(null);
    this.errorMessage.set(null);

    if (this.registerForm.invalid || this.isLoading() || this.isWorkspaceSlugBlocking) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);

    const payload = this.registerForm.getRawValue();
    const request: RegisterRequest = {
      fullName: payload.fullName ?? '',
      email: payload.email ?? '',
      password: payload.password ?? '',
      workspaceMode: this.workspaceMode(),
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
            const message = err.error?.message || 'An account with this email already exists.';
            this.errorMessage.set(message);
            this.notif.warning('Registration unavailable', message);
            return;
          }

          if (err.status === 400) {
            const errors = err.error?.errors;
            if (errors && Array.isArray(errors) && errors.length > 0) {
              this.errorMessage.set(errors.map((e: any) => e?.message || e).join('. '));
              this.notif.error('Validation error', 'Please check your information.');
              return;
            }

            const message = err.error?.message;
            this.errorMessage.set(
              message === 'Email already registered' ? 'An account with this email already exists.' : message || 'Please check your information.'
            );
            this.notif.error('Validation error', 'Please check your information.');
            return;
          }

          this.errorMessage.set('Unable to connect to server. Please try again.');
          this.notif.error('Network error', 'Unable to reach the server.');
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

  protected get workspaceSlugLabel(): string {
    return this.workspaceMode() === 'JOIN_TEAM' ? 'Team workspace slug' : 'Private workspace slug';
  }

  protected get workspaceSlugHint(): string {
    return this.workspaceMode() === 'JOIN_TEAM'
      ? 'Enter the slug shared by your workspace admin.'
      : 'Optional. Leave empty and we will create a slug from your name.';
  }

  protected get submitLabel(): string {
    if (this.isLoading()) {
      return this.workspaceMode() === 'JOIN_TEAM' ? 'Joining Workspace...' : 'Creating Account...';
    }
    return this.workspaceMode() === 'JOIN_TEAM' ? 'Join Workspace' : 'Create Account';
  }

  protected get passwordHasUppercase(): boolean {
    return /[A-Z]/.test(this.registerForm.controls.password.value ?? '');
  }

  protected get passwordHasNumberOrSymbol(): boolean {
    return /[0-9!@#$%^&*()\-_=+]/.test(this.registerForm.controls.password.value ?? '');
  }

  protected get isWorkspaceSlugBlocking(): boolean {
    const slug = this.registerForm.controls.workspaceSlug.value?.trim();
    const status = this.slugStatus();

    if (!slug || this.registerForm.controls.workspaceSlug.invalid || this.isCheckingSlug()) {
      return this.workspaceMode() === 'JOIN_TEAM';
    }

    if (!status || status.slug !== slug.toLowerCase()) {
      return true;
    }

    if (this.workspaceMode() === 'JOIN_TEAM') {
      return !status.canJoinBySlug;
    }

    return status.exists;
  }

  private checkWorkspaceSlug(): void {
    const slug = this.registerForm.controls.workspaceSlug.value?.trim().toLowerCase() ?? '';
    this.slugStatus.set(null);

    if (!slug || !slugPattern.test(slug)) {
      this.isCheckingSlug.set(false);
      return;
    }

    this.isCheckingSlug.set(true);
    this.workspaceService
      .checkSlug(slug)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (status) => {
          const currentSlug = this.registerForm.controls.workspaceSlug.value?.trim().toLowerCase() ?? '';
          if (status.slug === currentSlug) {
            this.slugStatus.set(status);
          }
          this.isCheckingSlug.set(false);
        },
        error: () => {
          this.slugStatus.set({
            slug,
            exists: false,
            canJoinBySlug: false,
            message: 'Unable to validate this workspace slug.'
          });
          this.isCheckingSlug.set(false);
        }
      });
  }
}
