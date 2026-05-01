import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Adr } from '../../../../models/adr.model';

type CollaborationTab = 'comments' | 'history' | 'team';

interface MockComment {
  initials: string;
  name: string;
  time: string;
  resolved: boolean;
  text: string;
}

@Component({
  selector: 'app-collaboration-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './collaboration-panel.component.html',
  styleUrl: './collaboration-panel.component.scss'
})
export class CollaborationPanelComponent {
  @Input() selectedAdr: Adr | null = null;
  @Input() currentUserInitials = 'ME';
  @Input() currentUserName = 'You';
  @Output() closePanel = new EventEmitter<void>();

  activeTab: CollaborationTab = 'comments';
  newComment = '';

  mockComments: MockComment[] = [
    {
      initials: 'SC',
      name: 'Sarah Chen',
      time: '2 hours ago',
      resolved: false,
      text: 'Have we considered the implications for our legacy systems? The migration path might be complex.'
    },
    {
      initials: 'MB',
      name: 'Michael Brown',
      time: '4 hours ago',
      resolved: true,
      text: 'Great analysis! The CQRS pattern seems like a perfect fit for our read-heavy workload.'
    },
    {
      initials: 'ED',
      name: 'Emily Davis',
      time: '1 day ago',
      resolved: false,
      text: 'Can we add more detail about the rollback strategy? This is critical for risk management.'
    }
  ];

  readonly mockHistory: string[] = [
    'Sarah Chen updated status to Proposed — 2h ago',
    'Michael Brown created this ADR — 1d ago',
    'Emily Davis commented on Alternatives — 1d ago'
  ];

  readonly mockTeam: Array<{ initials: string; name: string; role: string }> = [
    { initials: 'SC', name: 'Sarah Chen', role: 'AUTHOR' },
    { initials: 'MB', name: 'Michael Brown', role: 'REVIEWER' },
    { initials: 'ED', name: 'Emily Davis', role: 'APPROVER' }
  ];

  setTab(tab: CollaborationTab): void {
    this.activeTab = tab;
  }

  sendComment(): void {
    const trimmed = this.newComment.trim();
    if (!trimmed) {
      return;
    }

    this.mockComments = [
      {
        initials: this.currentUserInitials || 'ME',
        name: this.currentUserName || 'You',
        time: 'just now',
        resolved: false,
        text: trimmed
      },
      ...this.mockComments
    ];

    this.newComment = '';
  }

  getRoleBadgeClass(role: string): string {
    return `collab-panel__role collab-panel__role--${role.toLowerCase()}`;
  }
}
