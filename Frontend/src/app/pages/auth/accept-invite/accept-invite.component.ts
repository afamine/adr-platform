import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../services/auth.service';
import { ValidateInviteResponse, Role } from '../../../models/auth.models';

const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d).+$/;

const matchPasswordsValidator: ValidatorFn = (group): ValidationErrors | null => {
  const password = group.get('password')?.value;
  const confirmPassword = group.get('confirmPassword')?.value;

  if (!confirmPassword) {
    return null;
  }

  return password === confirmPassword ? null : { passwordMismatch: true };
};

type InviteState = 'LOADING' | 'FORM' | 'SUCCESS' | 'ERROR_INVALID' | 'ERROR_EXPIRED';

@Component({
  selector: 'app-accept-invite',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, MatIconModule],
  templateUrl: './accept-invite.component.html',
  styleUrls: ['./accept-invite.component.scss']
})
export class AcceptInviteComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  readonly state = signal<InviteState>('LOADING');
  readonly inviteDetails = signal<ValidateInviteResponse | null>(null);
  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string>('');

  token: string | null = null;

  inviteForm: FormGroup = this.fb.group(
    {
      fullName: ['', [Validators.required, Validators.minLength(2)]],
      password: ['', [Validators.required, Validators.minLength(8), Validators.pattern(passwordPattern)]],
      confirmPassword: ['', [Validators.required]]
    },
    { validators: [matchPasswordsValidator] }
  );

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token');

    if (!this.token) {
      this.state.set('ERROR_INVALID');
      return;
    }

    this.auth.validateInvite(this.token).subscribe({
      next: (details) => {
        this.inviteDetails.set(details);
        this.state.set('FORM');
      },
      error: (err) => {
        const body = err?.error;
        if (body?.errorType === 'EXPIRED') {
          this.state.set('ERROR_EXPIRED');
        } else {
          this.state.set('ERROR_INVALID');
        }
      }
    });
  }

  onSubmit(): void {
    if (!this.token) {
      this.state.set('ERROR_INVALID');
      return;
    }

    if (this.inviteForm.invalid || this.isSubmitting()) {
      this.inviteForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');

    const request = {
      token: this.token,
      fullName: this.inviteForm.get('fullName')?.value as string,
      password: this.inviteForm.get('password')?.value as string,
      confirmPassword: this.inviteForm.get('confirmPassword')?.value as string
    };

    this.auth.acceptInvite(request).subscribe({
      next: (response) => {
        this.isSubmitting.set(false);
        this.auth.saveTokens(response);
        this.state.set('SUCCESS');
        setTimeout(() => this.router.navigate(['/adrs']), 2000);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        const body = err?.error;
        if (body?.errorType === 'EXPIRED') {
          this.state.set('ERROR_EXPIRED');
        } else {
          this.errorMessage.set(body?.message || 'Something went wrong. Please try again.');
        }
      }
    });
  }

  hasError(control: 'fullName' | 'password' | 'confirmPassword', error: string): boolean {
    const c = this.inviteForm.controls[control];
    if (!c) return false;
    const show = c.touched || c.dirty;

    if (error === 'passwordMismatch') {
      return show && this.inviteForm.hasError('passwordMismatch');
    }

    return !!(show && c.hasError(error));
  }

  roleLabel(role: Role): string {
    switch (role) {
      case Role.ADMIN:
        return 'Administrator';
      case Role.APPROVER:
        return 'Approver';
      case Role.REVIEWER:
        return 'Reviewer';
      case Role.AUTHOR:
        return 'Author';
      default:
        return role;
    }
  }
}
