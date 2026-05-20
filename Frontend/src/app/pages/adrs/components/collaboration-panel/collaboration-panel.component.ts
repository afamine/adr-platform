import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, OnInit, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Adr, CommentDto, HistoryEventDto, TeamMemberDto } from '../../../../models/adr.model';
import { AdrService } from '../../../../services/adr.service';

type CollaborationTab = 'comments' | 'history' | 'team';

@Component({
  selector: 'app-collaboration-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './collaboration-panel.component.html',
  styleUrl: './collaboration-panel.component.scss'
})
export class CollaborationPanelComponent implements OnInit, OnChanges {
  @Input() selectedAdr: Adr | null = null;
  @Input() currentUserInitials = 'ME';
  @Input() currentUserName = 'You';
  @Output() closePanel = new EventEmitter<void>();

  private readonly adrService = inject(AdrService);
  private readonly cdr = inject(ChangeDetectorRef);

  activeTab: CollaborationTab = 'comments';
  comments: CommentDto[] = [];
  history: HistoryEventDto[] = [];
  team: TeamMemberDto[] = [];
  newComment = '';

  isLoadingComments = false;
  isLoadingHistory = false;
  isLoadingTeam = false;
  isSendingComment = false;
  isResolving: Record<string, boolean> = {};

  ngOnInit(): void {
    this.loadComments();
  }

  ngOnChanges(): void {
    if (this.selectedAdr?.id) {
      this.resetData();
      this.loadComments();
    }
  }

  setTab(tab: CollaborationTab): void {
    this.activeTab = tab;
    if (tab === 'history' && this.history.length === 0 && !this.isLoadingHistory) {
      this.loadHistory();
    }
    if (tab === 'team' && this.team.length === 0 && !this.isLoadingTeam) {
      this.loadTeam();
    }
  }

  private resetData(): void {
    this.comments = [];
    this.history = [];
    this.team = [];
    if (this.activeTab !== 'comments') {
      this.activeTab = 'comments';
    }
  }

  private loadComments(): void {
    const adrId = this.selectedAdr?.id;
    if (!adrId) return;
    this.isLoadingComments = true;
    this.adrService.getComments(adrId).subscribe({
      next: (items) => {
        this.comments = items;
        this.isLoadingComments = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoadingComments = false;
        this.cdr.detectChanges();
      }
    });
  }

  private loadHistory(): void {
    const adrId = this.selectedAdr?.id;
    if (!adrId) return;
    this.isLoadingHistory = true;
    this.adrService.getAdrHistory(adrId).subscribe({
      next: (items) => {
        this.history = items;
        this.isLoadingHistory = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoadingHistory = false;
        this.cdr.detectChanges();
      }
    });
  }

  private loadTeam(): void {
    const adrId = this.selectedAdr?.id;
    if (!adrId) return;
    this.isLoadingTeam = true;
    this.adrService.getAdrTeam(adrId).subscribe({
      next: (items) => {
        this.team = items;
        this.isLoadingTeam = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoadingTeam = false;
        this.cdr.detectChanges();
      }
    });
  }

  sendComment(): void {
    const trimmed = this.newComment.trim();
    const adrId = this.selectedAdr?.id;
    if (!trimmed || !adrId || this.isSendingComment) return;

    this.isSendingComment = true;
    this.adrService.addComment(adrId, trimmed).subscribe({
      next: (comment) => {
        this.comments = [...this.comments, comment];
        this.newComment = '';
        this.isSendingComment = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isSendingComment = false;
        this.cdr.detectChanges();
      }
    });
  }

  resolveComment(comment: CommentDto): void {
    const adrId = this.selectedAdr?.id;
    if (!adrId || this.isResolving[comment.id]) return;
    this.isResolving[comment.id] = true;
    this.adrService.resolveComment(adrId, comment.id, !comment.resolved).subscribe({
      next: (updated) => {
        this.comments = this.comments.map(c => c.id === updated.id ? updated : c);
        this.isResolving[comment.id] = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isResolving[comment.id] = false;
        this.cdr.detectChanges();
      }
    });
  }

  getRoleBadgeClass(role: string): string {
    return `collab-panel__role collab-panel__role--${role.toLowerCase()}`;
  }
}
