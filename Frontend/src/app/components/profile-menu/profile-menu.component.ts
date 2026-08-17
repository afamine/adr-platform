import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-profile-menu',
  standalone: true,
  templateUrl: './profile-menu.component.html',
  styleUrl: './profile-menu.component.scss'
})
export class ProfileMenuComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly isOpen = signal(false);

  get initials(): string {
    return (this.authService.getCurrentUser()?.fullName || 'AP')
      .split(' ')
      .filter(Boolean)
      .map((part) => part[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  }

  toggle(): void {
    this.isOpen.update((open) => !open);
  }

  openProfile(): void {
    this.isOpen.set(false);
    void this.router.navigate(['/profile']);
  }

  signOut(): void {
    this.authService.logout();
  }
}
