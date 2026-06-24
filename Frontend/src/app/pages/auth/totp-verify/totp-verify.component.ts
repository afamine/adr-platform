import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { TotpService } from '../../../services/totp.service';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-totp-verify',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './totp-verify.component.html',
  styleUrl: './totp-verify.component.scss'
})
export class TotpVerifyComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly totp = inject(TotpService);
  private readonly auth = inject(AuthService);

  form = this.fb.group({
    code: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6), Validators.pattern('[0-9]{6}')]]
  });

  isLoading = signal(false);
  errorMessage = signal<string | null>(null);

  private valueSub: Subscription = Subscription.EMPTY;

  ngOnInit(): void {
    this.valueSub = this.form.controls.code.valueChanges.subscribe((val) => {
      const v = String(val || '');
      if (v.length === 6 && this.form.valid && !this.isLoading()) {
        this.onVerify();
      }
    });
  }

  ngOnDestroy(): void {
    this.valueSub.unsubscribe();
  }

  onVerify(): void {
    if (this.isLoading()) return;

    this.isLoading.set(true);
    this.errorMessage.set(null);

    const code = this.form.controls.code.value as string;
    const pendingToken = this.totp.getPendingToken();

    if (!pendingToken) {
      // No pending token — go back to login
      this.isLoading.set(false);
      this.router.navigate(['/login']);
      return;
    }

    this.totp.validateLogin({ pendingToken, code }).subscribe({
      next: (response) => {
        this.auth.saveTokens(response);
        this.totp.clearPendingToken();
        // Redirect based on role
        if (response.user.role === 'ADMIN') {
          this.router.navigate(['/admin']);
        } else {
          this.router.navigate(['/adrs']);
        }
      },
      error: () => {
        this.errorMessage.set('Invalid code. Please try again.');
        this.isLoading.set(false);
      },
      complete: () => {
        this.isLoading.set(false);
      }
    });
  }

  goBack(): void {
    this.totp.clearPendingToken();
    this.router.navigate(['/login']);
  }
}

