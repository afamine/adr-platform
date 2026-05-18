import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ConfirmService } from '../../services/confirm.service';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule],
  template: `
    <ng-container *ngIf="confirmService.config$ | async as cfg">
      <div class="confirm-overlay" (click)="confirmService.resolve(false)">
        <div class="confirm-card" (click)="$event.stopPropagation()">
          <div class="confirm-title">{{ cfg.title }}</div>
          <div class="confirm-message">{{ cfg.message }}</div>
          <div class="confirm-actions">
            <button class="btn-cancel" type="button" (click)="confirmService.resolve(false)">
              {{ cfg.cancelLabel }}
            </button>
            <button
              class="btn-confirm"
              type="button"
              [class.danger]="cfg.danger"
              (click)="confirmService.resolve(true)"
            >
              {{ cfg.confirmLabel }}
            </button>
          </div>
        </div>
      </div>
    </ng-container>
  `,
  styles: [`
    .confirm-overlay {
      position: fixed;
      inset: 0;
      background: rgba(15, 23, 42, 0.4);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 10000;
    }
    .confirm-card {
      background: white;
      border-radius: 12px;
      padding: 28px;
      max-width: 400px;
      width: 90%;
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
    }
    .confirm-title {
      font-size: 16px;
      font-weight: 600;
      color: #111827;
      margin-bottom: 8px;
    }
    .confirm-message {
      font-size: 14px;
      color: #6b7280;
      line-height: 1.6;
      margin-bottom: 24px;
    }
    .confirm-actions {
      display: flex;
      gap: 12px;
      justify-content: flex-end;
    }
    .btn-cancel {
      border: 1px solid #d1d5db;
      background: white;
      color: #374151;
      padding: 8px 16px;
      border-radius: 8px;
      font-size: 13px;
      cursor: pointer;
    }
    .btn-cancel:hover {
      background: #f9fafb;
    }
    .btn-confirm {
      background: #0f172a;
      color: white;
      border: none;
      padding: 8px 16px;
      border-radius: 8px;
      font-size: 13px;
      font-weight: 500;
      cursor: pointer;
    }
    .btn-confirm.danger {
      background: #dc2626;
    }
    .btn-confirm:hover {
      opacity: 0.9;
    }
  `]
})
export class ConfirmDialogComponent {
  readonly confirmService = inject(ConfirmService);
}
