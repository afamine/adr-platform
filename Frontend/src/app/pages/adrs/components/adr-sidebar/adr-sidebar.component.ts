import { CommonModule } from '@angular/common';
import { Component, DestroyRef, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { debounceTime, Subject } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ADR_FILTER_OPTIONS, Adr, AdrStatus, AdrStatusFilter } from '../../../../models/adr.model';
import { AdrCardComponent } from '../adr-card/adr-card.component';

@Component({
  selector: 'app-adr-sidebar',
  standalone: true,
  imports: [CommonModule, FormsModule, AdrCardComponent],
  templateUrl: './adr-sidebar.component.html',
  styleUrl: './adr-sidebar.component.scss'
})
export class AdrSidebarComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly searchSubject = new Subject<string>();

  @Input() adrs: Adr[] = [];
  @Input() selectedId: string | null = null;
  @Input() searchQuery = '';
  @Input() statusFilter: AdrStatus | 'ALL' = 'ALL';

  @Output() adrSelected = new EventEmitter<Adr>();
  @Output() createNew = new EventEmitter<void>();
  @Output() searchChanged = new EventEmitter<string>();
  @Output() filterChanged = new EventEmitter<AdrStatusFilter>();

  readonly filterOptions = ADR_FILTER_OPTIONS;

  ngOnInit(): void {
    this.searchSubject
      .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
      .subscribe((query) => this.searchChanged.emit(query));
  }

  onSearchInput(query: string): void {
    this.searchSubject.next(query);
  }
}
