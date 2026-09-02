import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterModule, RouterOutlet } from '@angular/router';

import { AuthService } from '../../services/auth.service';
import { NotificationDropdownComponent } from '../../components/notification-dropdown/notification-dropdown.component';
import { WorkspaceSwitcherComponent } from '../../components/workspace-switcher/workspace-switcher.component';
import { ProfileMenuComponent } from '../../components/profile-menu/profile-menu.component';
import { AxiomLogoComponent } from '../../shared/axiom-logo/axiom-logo.component';
import { WorkspaceEventsService } from '../../services/workspace-events.service';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, RouterOutlet, WorkspaceSwitcherComponent, NotificationDropdownComponent, ProfileMenuComponent, AxiomLogoComponent],
  templateUrl: './admin-layout.component.html',
  styleUrls: ['./admin-layout.component.scss']
})
export class AdminLayoutComponent {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly workspaceEvents = inject(WorkspaceEventsService);
  readonly currentUser = this.authService.getCurrentUser();

  constructor() { this.workspaceEvents.connect(); }

  get workspaceName(): string {
    return this.currentUser?.workspaceName || 'Default Workspace';
  }

  get workspaceSlug(): string {
    return this.currentUser?.workspaceSlug || 'default';
  }
  isActive(section: 'users' | 'settings' | 'analytics' | 'audit-log' | 'email-templates-preview'): boolean {
    return this.router.url.includes(`/admin/${section}`);
  }

  navigateTo(path: string): void {
    void this.router.navigate([path]);
  }

  goBackToAdrs(): void {
    void this.router.navigate(['/adrs']);
  }

  logout(): void {
    this.authService.logout();
  }
}
