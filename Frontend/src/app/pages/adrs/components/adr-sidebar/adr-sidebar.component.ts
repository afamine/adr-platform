import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ADR_FILTER_OPTIONS, Adr, AdrStatus, AdrStatusFilter } from '../../../../models/adr.model';
import { AdrCardComponent } from '../adr-card/adr-card.component';

@Component({
  selector: 'app-adr-sidebar',
  standalone: true,
  imports: [CommonModule, FormsModule, AdrCardComponent],
  templateUrl: './adr-sidebar.component.html',
  styleUrl: './adr-sidebar.component.scss'
})
export class AdrSidebarComponent {
  @Input() adrs: Adr[] = [];
  @Input() selectedId: string | null = null;
  @Input() searchQuery = '';
  @Input() statusFilter: AdrStatus | 'ALL' = 'ALL';
  @Input() canCreate = true;
  @Input() isLoading = false;
  @Input() currentPage = 0;
  @Input() totalPages = 0;
  @Input() totalElements = 0;

  @Output() adrSelected = new EventEmitter<Adr>();
  @Output() createNew = new EventEmitter<void>();
  @Output() searchChanged = new EventEmitter<string>();
  @Output() filterChanged = new EventEmitter<AdrStatusFilter>();
  @Output() previousPage = new EventEmitter<void>();
  @Output() nextPage = new EventEmitter<void>();

  readonly filterOptions = ADR_FILTER_OPTIONS;

  onSearchInput(query: string): void {
    this.searchChanged.emit(query);
  }
}
