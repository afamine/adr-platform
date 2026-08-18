import { CommonModule } from '@angular/common';
import { Component, EventEmitter, HostListener, Input, Output, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { GenerateAdrDraftResponse } from '../../../../models/adr.model';
import { AdrService } from '../../../../services/adr.service';

@Component({
  selector: 'app-generate-adr-draft-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './generate-adr-draft-modal.component.html',
  styleUrl: './generate-adr-draft-modal.component.scss'
})
export class GenerateAdrDraftModalComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly adrService = inject(AdrService);

  @Input() hasExistingContent = false;
  @Output() closed = new EventEmitter<void>();
  @Output() draftGenerated = new EventEmitter<GenerateAdrDraftResponse>();

  readonly form = this.formBuilder.nonNullable.group({
    problemDescription: ['', [Validators.required, Validators.minLength(20), Validators.maxLength(2000)]]
  });
  isGenerating = false;
  errorMessage = '';

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (!this.isGenerating) this.close();
  }

  close(): void {
    if (!this.isGenerating) this.closed.emit();
  }

  onSubmit(): void {
    if (this.form.invalid || this.isGenerating) {
      this.form.markAllAsTouched();
      return;
    }

    this.isGenerating = true;
    this.errorMessage = '';
    this.adrService.generateAdrDraft({
      problemDescription: this.form.controls.problemDescription.value.trim()
    }).subscribe({
      next: (draft) => {
        this.isGenerating = false;
        this.draftGenerated.emit(draft);
      },
      error: (error) => {
        this.isGenerating = false;
        this.errorMessage = error?.message || 'AI could not generate a draft. Please try again.';
      }
    });
  }
}
