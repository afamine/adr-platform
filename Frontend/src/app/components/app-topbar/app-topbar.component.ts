import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-app-topbar',
  templateUrl: './app-topbar.component.html',
  styleUrl: './app-topbar.component.scss'
})
export class AppTopbarComponent {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  openSettings(): void {
    const target = this.authService.isAdmin() ? '/admin/settings' : '/profile';
    void this.router.navigate([target]);
  }

  openProfile(): void {
    void this.router.navigate(['/profile']);
  }
}
