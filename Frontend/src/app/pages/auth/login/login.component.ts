import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../services/auth.service';
import { LoginRequest, AuthResponse } from '../../../models/auth.models';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, MatIconModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  passwordVisible = signal(false);
  isLoading = signal(false);
  submitted = signal(false);
  errorMessage = signal<string | null>(null);

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
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload: LoginRequest = this.form.getRawValue() as LoginRequest;

    this.isLoading.set(true);
    this.auth.login(payload).subscribe({
      next: (res: AuthResponse) => {
        this.auth.saveTokens(res);
        this.isLoading.set(false);
        this.router.navigateByUrl('/adrs');
      },
      error: (err) => {
        this.isLoading.set(false);
        const msg = err?.error?.message || 'Invalid email or password. Please try again.';
        this.errorMessage.set(msg);
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
