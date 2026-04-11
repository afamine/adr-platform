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
    accepted: 'Accepted',
    proposed: 'Proposed',
    draft: 'Draft',
    rejected: 'Rejected'
  };

  protected onSelect(): void {
    this.selectAdr.emit(this.adr().id);
  }
}
