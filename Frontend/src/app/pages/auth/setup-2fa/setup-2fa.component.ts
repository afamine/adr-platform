import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';

import { Enable2faModalComponent } from '../../../components/enable-2fa-modal/enable-2fa-modal.component';
import { AuthService } from '../../../services/auth.service';
import { NotificationService } from '../../../services/notification.service';
import { TotpService } from '../../../services/totp.service';

@Component({
  selector: 'app-setup-2fa',
  standalone: true,
  imports: [CommonModule, Enable2faModalComponent],
  templateUrl: './setup-2fa.component.html',
  styleUrl: './setup-2fa.component.scss'
})
export class Setup2faComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly totp = inject(TotpService);
  private readonly notifications = inject(NotificationService);

  showSetupModal = true;
  isCheckingStatus = true;

  ngOnInit(): void {
    this.totp.getStatus().subscribe({
      next: (status) => {
        this.isCheckingStatus = false;
        if (status.enabled) {
          this.markSetupComplete();
          this.goToDashboard();
        }
      },
      error: () => {
        this.isCheckingStatus = false;
      }
    });
  }

  onEnabled(): void {
    this.markSetupComplete();
    this.notifications.success('2FA enabled', 'Your account is now protected.');
    this.goToDashboard();
  }

  keepModalOpen(): void {
    this.showSetupModal = true;
  }

  private markSetupComplete(): void {
    const user = this.auth.getCurrentUser();
    if (user) {
      this.auth.saveUser({ ...user, totpSetupRequired: false });
    }
  }

  private goToDashboard(): void {
    const user = this.auth.getCurrentUser();
    if (user?.role === 'ADMIN') {
      this.router.navigate(['/admin/users']);
      return;
    }

    this.router.navigate(['/adrs']);
  }
}
