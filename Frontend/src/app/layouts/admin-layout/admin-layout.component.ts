import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterModule } from '@angular/router';

import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './admin-layout.component.html',
  styleUrls: ['./admin-layout.component.scss']
})
export class AdminLayoutComponent {
  private readonly router = inject(Router);
  readonly currentUser = inject(AuthService).getCurrentUser();

  get userInitials(): string {
    return (this.currentUser?.fullName || 'AP')
      .split(' ')
      .filter(Boolean)
      .map((n: string) => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  }

  isActive(section: 'users' | 'settings' | 'analytics'): boolean {
    return this.router.url.includes(`/admin/${section}`);
  }

  navigateTo(path: string): void {
    void this.router.navigate([path]);
  }

  goBackToAdrs(): void {
    void this.router.navigate(['/adrs']);
  }
}
