import { Component, input, output } from '@angular/core';
import { Adr } from '../../models/adr.model';

@Component({
  selector: 'app-adr-card',
  templateUrl: './adr-card.component.html',
  styleUrl: './adr-card.component.css'
})
export class AdrCardComponent {
  readonly adr = input.required<Adr>();
  readonly selected = input(false);
  readonly selectAdr = output<string>();

  protected readonly statusLabels: Record<Adr['status'], string> = {
    DRAFT: 'Draft',
    PROPOSED: 'Proposed',
    UNDER_REVIEW: 'Under Review',
    ACCEPTED: 'Accepted',
    REJECTED: 'Rejected',
    SUPERSEDED: 'Superseded'
  };

  protected get summary(): string {
    const adr = this.adr();
    const source = adr.context || adr.decision || adr.consequences || adr.alternatives || '';
    return source.trim() || 'No summary available.';
  }

  protected onSelect(): void {
    this.selectAdr.emit(this.adr().id);
  }
}
