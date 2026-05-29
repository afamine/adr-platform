import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-supersedes-badge',
  standalone: true,
  template: `
    <div class="supersedes-badge">
      🔗
      <span>
        Supersedes:
        <button type="button" (click)="navigate.emit()">ADR-{{ supersededAdrNumber }}: {{ supersededTitle }}</button>
      </span>
    </div>
  `,
  styles: [
    `
      .supersedes-badge {
        display: inline-flex;
        align-items: center;
        gap: 8px;
        padding: 8px 12px;
        background: #eff6ff;
        border: 1px solid #bfdbfe;
        border-radius: 6px;
      }

      .supersedes-badge span {
        font-size: 13px;
        color: #1e40af;
      }

      .supersedes-badge button {
        background: none;
        border: none;
        font-weight: 600;
        color: #1e40af;
        text-decoration: underline;
        cursor: pointer;
        padding: 0;
      }
    `
  ]
})
export class SupersedesBadgeComponent {
  @Input() supersededAdrNumber!: number;
  @Input() supersededTitle!: string;
  @Output() navigate = new EventEmitter<void>();
}
