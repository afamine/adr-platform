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
  readonly statusFilter = input<AdrStatusFilter>('all');

  readonly createAdr = output<void>();
  readonly searchQueryChange = output<string>();
  readonly statusFilterChange = output<AdrStatusFilter>();
  readonly adrSelected = output<string>();

  protected readonly filters: ReadonlyArray<{ value: AdrStatusFilter; label: string }> = [
    { value: 'all', label: 'All Status' },
    { value: 'accepted', label: 'Accepted' },
    { value: 'proposed', label: 'Proposed' },
    { value: 'draft', label: 'Draft' },
    { value: 'rejected', label: 'Rejected' }
  ];
}
