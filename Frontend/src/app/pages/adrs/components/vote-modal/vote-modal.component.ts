import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdrDto, VoteDto, VoteType } from '../../../../models/adr.model';
import { AuthService } from '../../../../services/auth.service';

type ModalState = 'can-vote' | 'already-voted' | 'approver-decision';

@Component({
  selector: 'app-vote-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './vote-modal.component.html',
  styleUrls: ['./vote-modal.component.scss']
})
export class VoteModalComponent {
  @Input() adr!: AdrDto;
  @Input() existingVotes: VoteDto[] = [];
  @Input() myVote: VoteDto | null = null;
  @Input() isLoadingVotes = false;
  @Input() isSubmitting = false;
  @Output() close = new EventEmitter<void>();
  @Output() voteSubmitted = new EventEmitter<{ voteType: VoteType; comment: string }>();

  selectedVote: VoteType | null = 'APPROVE';
  comment = '';
  maxComment = 500;
  submissionAttempted = false;

  currentUser = inject(AuthService).getCurrentUser();

  get modalState(): ModalState {
    if (this.myVote) {
      return 'already-voted';
    }

    if (this.currentUser?.role === 'APPROVER' || this.currentUser?.role === 'ADMIN') {
      return 'approver-decision';
    }

    return 'can-vote';
  }

  get charCount(): number {
    return this.comment.length;
  }

  get canSubmit(): boolean {
    const trimmedComment = this.comment.trim();
    return !!this.selectedVote
      && this.modalState !== 'already-voted'
      && trimmedComment.length > 0
      && trimmedComment.length <= this.maxComment
      && !this.isSubmitting
      && !this.isLoadingVotes;
  }

  get showCommentError(): boolean {
    if (!this.submissionAttempted) {
      return false;
    }

    const trimmedComment = this.comment.trim();
    return trimmedComment.length === 0 || trimmedComment.length > this.maxComment;
  }

  selectVote(type: VoteType): void {
    this.selectedVote = type;
  }

  onSubmit(): void {
    this.submissionAttempted = true;
    if (!this.selectedVote || !this.canSubmit) {
      return;
    }

    this.voteSubmitted.emit({ voteType: this.selectedVote, comment: this.comment.trim() });
  }

  onClose(): void {
    this.close.emit();
  }
}
