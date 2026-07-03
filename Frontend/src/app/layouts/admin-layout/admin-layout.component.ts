import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterModule } from '@angular/router';

import { AuthService } from '../../services/auth.service';
import { NotificationDropdownComponent } from '../../components/notification-dropdown/notification-dropdown.component';
import { WorkspaceSwitcherComponent } from '../../components/workspace-switcher/workspace-switcher.component';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, WorkspaceSwitcherComponent, NotificationDropdownComponent],
  templateUrl: './admin-layout.component.html',
  styleUrls: ['./admin-layout.component.scss']
})
export class AdminLayoutComponent {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  readonly currentUser = this.authService.getCurrentUser();

  get userInitials(): string {
    return (this.currentUser?.fullName || 'AP')
      .split(' ')
      .filter(Boolean)
      .map((n: string) => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  }

  isActive(section: 'users' | 'settings' | 'analytics' | 'audit-log'): boolean {
    return this.router.url.includes(`/admin/${section}`);
  }

  navigateTo(path: string): void {
    void this.router.navigate([path]);
  }

  goBackToAdrs(): void {
    void this.router.navigate(['/adrs']);
  }

  openProfile(): void {
    void this.router.navigate(['/profile']);
  }

  logout(): void {
    this.authService.logout();
  }
}
