import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Adr } from '../../../../models/adr.model';

@Component({
  selector: 'app-adr-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './adr-card.component.html',
  styleUrl: './adr-card.component.scss'
})
export class AdrCardComponent {
  @Input({ required: true }) adr!: Adr;
  @Input() isSelected = false;
  @Output() selected = new EventEmitter<Adr>();

  readonly statusLabels: Record<Adr['status'], string> = {
    DRAFT: 'Draft',
    PROPOSED: 'proposed',
    UNDER_REVIEW: 'Under Review',
    ACCEPTED: 'accepted',
    REJECTED: 'Rejected',
    SUPERSEDED: 'Superseded'
  };

  get badgeClass(): string {
    return `badge badge-${this.adr.status}`;
  }

  get summary(): string {
    const source = this.adr.context || this.adr.decision || this.adr.consequences || this.adr.alternatives || '';
    return source.trim() || 'No summary available.';
  }

  get visibleTags(): string[] {
    return this.adr.tags.slice(0, 3);
  }

  get hiddenTagCount(): number {
    return Math.max(this.adr.tags.length - this.visibleTags.length, 0);
  }

  onSelect(): void {
    this.selected.emit(this.adr);
  }
}
