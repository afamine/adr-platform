import { Component, input, output } from '@angular/core';
import { Adr, AdrStatusFilter } from '../../models/adr.model';
import { AdrCardComponent } from '../adr-card/adr-card.component';

@Component({
  selector: 'app-sidebar',
  imports: [AdrCardComponent],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css'
})
export class SidebarComponent {
  readonly adrs = input.required<Adr[]>();
  readonly selectedAdrId = input.required<string>();
  readonly searchQuery = input('');
  readonly statusFilter = input<AdrStatusFilter>('ALL');

  readonly createAdr = output<void>();
  readonly searchQueryChange = output<string>();
  readonly statusFilterChange = output<AdrStatusFilter>();
  readonly adrSelected = output<string>();

  protected readonly filters: ReadonlyArray<{ value: AdrStatusFilter; label: string }> = [
    { value: 'ALL', label: 'All Status' },
    { value: 'DRAFT', label: 'Draft' },
    { value: 'PROPOSED', label: 'Proposed' },
    { value: 'UNDER_REVIEW', label: 'Under Review' },
    { value: 'ACCEPTED', label: 'Accepted' },
    { value: 'REJECTED', label: 'Rejected' },
    { value: 'SUPERSEDED', label: 'Superseded' }
  ];
}
