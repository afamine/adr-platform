import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { AdrStatus } from '../../../../models/adr.model';

interface WorkflowAction {
  status: AdrStatus;
  label: string;
  tone: 'propose' | 'review' | 'accept' | 'reject' | 'neutral';
}

@Component({
  selector: 'app-adr-workflow-bar',
  standalone: true,
  templateUrl: './adr-workflow-bar.component.html',
  styleUrl: './adr-workflow-bar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AdrWorkflowBarComponent {
  @Input() transitions: AdrStatus[] = [];
  @Input() currentStatus: AdrStatus | null = null;
  @Output() transitionRequested = new EventEmitter<AdrStatus>();

  readonly actions: WorkflowAction[] = [
    { status: 'PROPOSED', label: 'Propose', tone: 'propose' },
    { status: 'UNDER_REVIEW', label: 'Start Review', tone: 'review' },
    { status: 'ACCEPTED', label: 'Accept', tone: 'accept' },
    { status: 'REJECTED', label: 'Reject', tone: 'reject' },
    { status: 'DRAFT', label: 'Reopen', tone: 'neutral' },
    { status: 'SUPERSEDED', label: 'Supersede', tone: 'neutral' }
  ];

  isAvailable(status: AdrStatus): boolean { return this.transitions.includes(status); }
  request(status: AdrStatus): void { if (this.isAvailable(status)) this.transitionRequested.emit(status); }
}
