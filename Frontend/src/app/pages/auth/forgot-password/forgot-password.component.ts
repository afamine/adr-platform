import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../services/auth.service';
import { NotificationService } from '../../../services/notification.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, MatIconModule],
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.scss']
})
export class ForgotPasswordComponent {
  forgotForm: FormGroup;
  isLoading = false;
  successMessage = '';
  errorMessage = '';

  private readonly auth = inject(AuthService);
  private readonly notif = inject(NotificationService);

  constructor(private fb: FormBuilder) {
    this.forgotForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  onSubmit(): void {
    if (this.forgotForm.invalid) {
      this.forgotForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.successMessage = '';
    this.errorMessage = '';

    const email = this.forgotForm.get('email')?.value as string;

    this.auth.forgotPassword(email).subscribe({
      next: () => {
        this.isLoading = false;
        this.successMessage =
          'If this email is registered, you will receive a reset link shortly. Check your inbox (and spam folder).';
        this.notif.success('Email envoyé', 'Si cet email est enregistré, vous recevrez un lien de réinitialisation.');
        this.forgotForm.reset();
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err?.error?.message || 'Unable to send reset link. Please try again later.';
        this.notif.error('Envoi échoué', 'Impossible d\'envoyer le lien de réinitialisation.');
      }
    });
  }

  resetToForm(): void {
    this.successMessage = '';
    this.errorMessage = '';
    this.forgotForm.reset();
  }
}
