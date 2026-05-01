import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ADR_STATUS_OPTIONS, ADR_TAB_ORDER, Adr, AdrStatus, AdrTabKey, CreateAdrRequest, UpdateAdrRequest } from '../../../../models/adr.model';

@Component({
  selector: 'app-adr-editor',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './adr-editor.component.html',
  styleUrl: './adr-editor.component.scss'
})
export class AdrEditorComponent implements OnChanges {
  private readonly formBuilder = inject(FormBuilder);

  @Input() adr: Adr | null = null;
  @Input() editingAdr: Partial<Adr> | null = null;
  @Input() isEditing = false;
  @Input() activeTab: 'context' | 'decision' | 'consequences' | 'alternatives' = 'context';
  @Input() showAIPanel = false;
  @Input() showCollabPanel = false;
  @Input() isSaving = false;
  @Input() canEdit = false;
  @Input() canDelete = false;
  @Input() transitions: AdrStatus[] = [];

  @Output() tabChanged = new EventEmitter<AdrTabKey>();
  @Output() editRequested = new EventEmitter<void>();
  @Output() deleteRequested = new EventEmitter<void>();
  @Output() transitionRequested = new EventEmitter<AdrStatus>();
  @Output() statusChanged = new EventEmitter<AdrStatus>();
  @Output() saveCreate = new EventEmitter<CreateAdrRequest>();
  @Output() saveUpdate = new EventEmitter<UpdateAdrRequest>();
  @Output() cancelEdit = new EventEmitter<void>();
  @Output() showAI = new EventEmitter<void>();
  @Output() showCollab = new EventEmitter<void>();

  readonly tabs = ADR_TAB_ORDER;
  readonly statusOptions = ADR_STATUS_OPTIONS;
  isPreviewMode = false;
  tagInput = '';
  readonly form = this.formBuilder.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    context: [''],
    decision: [''],
    consequences: [''],
    alternatives: [''],
    tags: this.formBuilder.nonNullable.control<string[]>([])
  });

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['editingAdr'] || changes['isEditing']) && this.isEditing) {
      this.syncFormFromInput();
    }
  }

  get currentContent(): string {
    if (!this.adr) {
      return '';
    }

    return this.adr[this.activeTab] ?? '';
  }

  get currentTags(): string[] {
    return this.form.controls.tags.value;
  }

  get formattedCreatedAt(): string {
    return this.adr?.createdAt?.slice(0, 10) ?? '';
  }

  togglePreview(): void {
    this.isPreviewMode = !this.isPreviewMode;
  }

  onEditClick(): void {
    this.isPreviewMode = false;
    this.editRequested.emit();
  }

  onAddTag(): void {
    const trimmed = this.tagInput.trim();
    if (!trimmed || this.currentTags.includes(trimmed)) {
      return;
    }

    this.form.controls.tags.setValue([...this.currentTags, trimmed]);
    this.tagInput = '';
  }

  onRemoveTag(tag: string): void {
    this.form.controls.tags.setValue(this.currentTags.filter((item) => item !== tag));
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const payload = {
      title: value.title.trim(),
      context: value.context,
      decision: value.decision,
      consequences: value.consequences,
      alternatives: value.alternatives,
      tags: value.tags
    };

    if (this.adr?.id) {
      this.saveUpdate.emit(payload);
      return;
    }

    this.saveCreate.emit(payload);
  }

  getTransitionLabel(status: AdrStatus): string {
    const labels: Record<AdrStatus, string> = {
      DRAFT: '← Reopen',
      PROPOSED: '→ Propose',
      UNDER_REVIEW: '→ Start Review',
      ACCEPTED: '→ Accept',
      REJECTED: '→ Reject',
      SUPERSEDED: '→ Supersede'
    };

    return labels[status];
  }

  private syncFormFromInput(): void {
    this.tagInput = '';
    this.form.reset({
      title: this.editingAdr?.title ?? '',
      context: this.editingAdr?.context ?? '',
      decision: this.editingAdr?.decision ?? '',
      consequences: this.editingAdr?.consequences ?? '',
      alternatives: this.editingAdr?.alternatives ?? '',
      tags: [...(this.editingAdr?.tags ?? [])]
    });
  }
}
