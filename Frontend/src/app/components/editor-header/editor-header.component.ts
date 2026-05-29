import { Component, computed, inject, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { Adr, AdrStatus, CompletenessResult } from '../../models/adr.model';
import { AdrService } from '../../services/adr.service';

@Component({
  selector: 'app-editor-header',
  imports: [MatButtonModule],
  templateUrl: './editor-header.component.html',
  styleUrl: './editor-header.component.scss'
})
export class EditorHeaderComponent {
  private readonly adrService = inject(AdrService);

  readonly title = input.required<string>();
  readonly updatedAt = input.required<string>();
  readonly author = input.required<string>();
  readonly status = input<AdrStatus | null>(null);
  readonly isEditing = input(false);
  readonly adr = input<Adr | null>(null);
  readonly completeness = input<CompletenessResult | null>(null);
  readonly previewActive = input(false);
  readonly collaborateActive = input(false);
  readonly aiAssistActive = input(false);

  readonly shouldShowCompleteness = computed(() => {
    const status = this.status();
    const completeness = this.completeness();

    if (!status || !completeness) {
      return false;
    }

    if (status === 'ACCEPTED' || status === 'REJECTED' || status === 'SUPERSEDED') {
      return false;
    }

    return this.isEditing() || status === 'DRAFT' || status === 'PROPOSED';
  });

  readonly completenessText = computed(() => {
    const result = this.completeness();
    if (!result) {
      return '';
    }

    const tagState = result.hasTags ? 'Tags ready' : 'Tags missing';
    return `${result.filledSections}/${result.totalSections} sections · ${tagState}`;
  });

  readonly titleChange = output<string>();
  readonly previewClick = output<void>();
  readonly collaborateClick = output<void>();
  readonly aiAssistClick = output<void>();
  readonly saveClick = output<void>();
  readonly exportClick = output<void>();

  onExport(): void {
    const adr = this.adr();
    if (adr) {
      this.adrService.exportMarkdown(adr.id, adr.adrNumber);
    }
  }
}
