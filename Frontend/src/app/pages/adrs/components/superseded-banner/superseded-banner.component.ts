import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-superseded-banner',
  standalone: true,
  template: `
    <div class="superseded-banner">
      <span class="superseded-banner__icon">⚠</span>
      <p class="superseded-banner__text">
        This decision was superseded. It has been replaced by
        <button class="superseded-banner__link" type="button" (click)="navigate.emit()">
          ADR-{{ replacementAdrNumber }}: {{ replacementTitle }}
        </button>
      </p>
    </div>
  `,
  styles: [
    `
      .superseded-banner {
        background: #fffbeb;
        border-left: 4px solid #f59e0b;
        padding: 12px 16px;
        display: flex;
        align-items: flex-start;
        gap: 10px;
        width: 100%;
      }

      .superseded-banner__icon {
        color: #d97706;
        font-size: 16px;
      }

      .superseded-banner__text {
        font-size: 13px;
        color: #92400e;
        margin: 0;
      }

      .superseded-banner__link {
        background: none;
        border: none;
        font-weight: 600;
        color: #92400e;
        text-decoration: underline;
        cursor: pointer;
        padding: 0;
      }
    `
  ]
})
export class SupersededBannerComponent {
  @Input() replacementAdrNumber!: number;
  @Input() replacementTitle!: string;
  @Output() navigate = new EventEmitter<void>();
}
