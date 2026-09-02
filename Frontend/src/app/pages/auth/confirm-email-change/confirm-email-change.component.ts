import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../services/auth.service';
import { AxiomLogoComponent } from '../../../shared/axiom-logo/axiom-logo.component';

type ConfirmationState = 'LOADING' | 'SUCCESS' | 'ERROR';

@Component({
  selector: 'app-confirm-email-change',
  standalone: true,
  imports: [CommonModule, RouterLink, AxiomLogoComponent],
  templateUrl: './confirm-email-change.component.html',
  styleUrls: ['../verify-email/verify-email.component.scss']
})
export class ConfirmEmailChangeComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private redirectTimeout: ReturnType<typeof setTimeout> | null = null;

  readonly state = signal<ConfirmationState>('LOADING');
  readonly errorMessage = signal('This confirmation link is invalid, expired, or has already been used.');

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.state.set('ERROR');
      return;
    }

    this.authService.confirmEmailChange(token).subscribe({
      next: () => {
        this.state.set('SUCCESS');
        this.redirectTimeout = setTimeout(() => this.authService.logout(), 2500);
      },
      error: (error) => {
        this.errorMessage.set(error?.error?.message || this.errorMessage());
        this.state.set('ERROR');
      }
    });
  }

  ngOnDestroy(): void {
    if (this.redirectTimeout) {
      clearTimeout(this.redirectTimeout);
    }
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}
