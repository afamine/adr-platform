import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { AdrStatus } from '../../../../models/adr.model';

@Component({
  selector: 'app-adr-status-badge',
  standalone: true,
  templateUrl: './adr-status-badge.component.html',
  styleUrl: './adr-status-badge.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AdrStatusBadgeComponent {
  @Input({ required: true }) status!: AdrStatus;
  @Input() compact = false;

  get label(): string {
    return {
      DRAFT: 'Draft',
      PROPOSED: 'Proposed',
      UNDER_REVIEW: 'In Review',
      ACCEPTED: 'Accepted',
      REJECTED: 'Rejected',
      SUPERSEDED: 'Superseded'
    }[this.status];
  }
}
