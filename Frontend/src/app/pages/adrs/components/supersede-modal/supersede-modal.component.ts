import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdrDto, AdrStatus, AdrSummary } from '../../../../models/adr.model';
import { AdrService } from '../../../../services/adr.service';

@Component({
  selector: 'app-supersede-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './supersede-modal.component.html',
  styleUrls: ['./supersede-modal.component.scss']
})
export class SupersedeModalComponent implements OnInit {
  @Input() adr!: AdrDto;
  @Input() isSubmitting = false;
  @Output() close = new EventEmitter<void>();
  @Output() supersedeConfirmed = new EventEmitter<{ supersededByAdrId: string; reason?: string }>();

  private readonly adrService = inject(AdrService);

  availableAdrs: AdrSummary[] = [];
  filteredAdrs: AdrSummary[] = [];
  selectedAdr: AdrSummary | null = null;
  reason = '';
  searchQuery = '';
  isDropdownOpen = false;
  isLoading = false;

  ngOnInit(): void {
    this.loadEligibleAdrs();
  }

  loadEligibleAdrs(): void {
    this.isLoading = true;
    this.adrService.getEligibleReplacements().subscribe({
      next: (adrs) => {
        this.availableAdrs = adrs.filter((item) => item.id !== this.adr.id);
        this.filteredAdrs = [...this.availableAdrs];
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      }
    });
  }

  onSearch(query: string): void {
    this.searchQuery = query;
    const normalizedQuery = query.toLowerCase().trim();
    this.filteredAdrs = normalizedQuery
      ? this.availableAdrs.filter(
          (item) => item.adrNumber.toString().includes(normalizedQuery) || item.title.toLowerCase().includes(normalizedQuery)
        )
      : [...this.availableAdrs];
  }

  selectAdr(adr: AdrSummary): void {
    this.selectedAdr = adr;
    this.isDropdownOpen = false;
    this.searchQuery = '';
    this.filteredAdrs = [...this.availableAdrs];
  }

  clearSelection(): void {
    this.selectedAdr = null;
  }

  onConfirm(): void {
    if (!this.selectedAdr || this.isSubmitting) {
      return;
    }

    this.supersedeConfirmed.emit({
      supersededByAdrId: this.selectedAdr.id,
      reason: this.reason.trim() || undefined
    });
  }

  onClose(): void {
    this.close.emit();
  }

  getStatusClass(status: AdrStatus): string {
    const map: Record<string, string> = {
      DRAFT: 'badge-draft',
      PROPOSED: 'badge-proposed',
      UNDER_REVIEW: 'badge-review',
      ACCEPTED: 'badge-accepted'
    };
    return map[status] ?? 'badge-draft';
  }
}
