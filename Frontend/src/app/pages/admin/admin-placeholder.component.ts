import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-admin-placeholder',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="admin-page">
      <div class="admin-card">
        <div class="card-header">
          <span class="brand-name">ADR Manager</span>
          <div class="divider"></div>
          <h1 class="title">Admin Panel</h1>
        </div>

        <div class="content">
          <h2 class="section-title">User Management</h2>
          <p class="state-message">
            Coming soon — full implementation in the next sprint.
          </p>

          <p class="user-info" *ngIf="user as u">
            Logged in as: <strong>{{ u.fullName }}</strong> ({{ u.role }})
          </p>

          <button type="button" class="btn btn--primary" (click)="logout()">
            Sign out
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    :host { display: block; min-height: 100vh; }
    .admin-page {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 32px 16px;
      background-color: #f9fafb;
      font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      color: #111827;
    }
    .admin-card {
      width: 100%;
      max-width: 520px;
      background: #ffffff;
      border: 1px solid #e5e7eb;
      border-radius: 16px;
      padding: 48px 40px;
      box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
    }
    .card-header {
      margin-bottom: 32px;
      display: flex;
      flex-direction: column;
      align-items: center;
    }
    .brand-name {
      margin-bottom: 16px;
      font-size: 16px;
      font-weight: 700;
      color: #111827;
    }
    .divider {
      width: 100%;
      height: 1px;
      background-color: #e5e7eb;
      margin-bottom: 24px;
    }
    .title {
      margin: 0;
      font-size: 22px;
      font-weight: 600;
      color: #111827;
    }
    .content {
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      gap: 12px;
    }
    .section-title {
      margin: 0;
      font-size: 18px;
      font-weight: 600;
      color: #111827;
    }
    .state-message {
      margin: 0;
      font-size: 14px;
      color: #6b7280;
    }
    .user-info {
      margin: 8px 0 16px;
      font-size: 13px;
      color: #374151;
      strong { color: #111827; font-weight: 600; }
    }
    .btn {
      width: 100%;
      height: 44px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      padding: 0 16px;
      border-radius: 10px;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      border: 1px solid transparent;
      transition: background-color 0.15s ease;
    }
    .btn--primary {
      background-color: #0f172a;
      color: #ffffff;
      &:hover { background-color: #1e293b; }
    }
  `]
})
export class AdminPlaceholderComponent {
  private readonly authService = inject(AuthService);
  readonly user = this.authService.getCurrentUser();

  logout(): void {
    this.authService.logout();
  }
}
