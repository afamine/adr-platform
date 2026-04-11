import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-editor-header',
  imports: [MatButtonModule],
  templateUrl: './editor-header.component.html',
  styleUrl: './editor-header.component.css'
})
export class EditorHeaderComponent {
  readonly title = input.required<string>();
  readonly updatedAt = input.required<string>();
  readonly author = input.required<string>();
  readonly previewActive = input(false);
  readonly collaborateActive = input(false);
  readonly aiAssistActive = input(false);

  readonly titleChange = output<string>();
  readonly previewClick = output<void>();
  readonly collaborateClick = output<void>();
  readonly aiAssistClick = output<void>();
  readonly saveClick = output<void>();
}
