import { Component, input, output } from '@angular/core';
import { AdrTabKey } from '../../models/adr.model';

@Component({
  selector: 'app-tab-nav',
  templateUrl: './tab-nav.component.html',
  styleUrl: './tab-nav.component.css'
})
export class TabNavComponent {
  readonly tabs = input.required<ReadonlyArray<{ key: AdrTabKey; label: string }>>();
  readonly activeTab = input.required<AdrTabKey>();
  readonly tabChange = output<AdrTabKey>();
}
