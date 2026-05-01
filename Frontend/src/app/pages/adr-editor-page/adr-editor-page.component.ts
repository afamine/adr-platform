import { Component } from '@angular/core';
import { AdrDashboardComponent } from '../adrs/adr-dashboard.component';

@Component({
  selector: 'app-adr-editor-page',
  standalone: true,
  imports: [AdrDashboardComponent],
  templateUrl: './adr-editor-page.component.html',
  styleUrl: './adr-editor-page.component.css'
})
export class AdrEditorPageComponent {}
