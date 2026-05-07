import { CommonModule, formatDate } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ADR_STATUS_OPTIONS, ADR_TAB_ORDER, Adr, AdrStatus, AdrTabKey, AuditEventDto, AuditEventType, CreateAdrRequest, UpdateAdrRequest } from '../../../../models/adr.model';
import { AdrService } from '../../../../services/adr.service';

@Component({
  selector: 'app-adr-editor',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './adr-editor.component.html',
  styleUrl: './adr-editor.component.scss'
})
export class AdrEditorComponent implements OnChanges {
  private readonly formBuilder = inject(FormBuilder);
  private readonly adrService = inject(AdrService);

  @Input() adr: Adr | null = null;
  @Input() editingAdr: Partial<Adr> | null = null;
  @Input() isEditing = false;
  @Input() activeTab: 'context' | 'decision' | 'consequences' | 'alternatives' | 'audit' = 'context';
  @Input() showAIPanel = false;
  @Input() showCollabPanel = false;
  @Input() isSaving = false;
  @Input() canEdit = false;
  @Input() canDelete = false;
  @Input() transitions: AdrStatus[] = [];
  @Input() auditRefreshToken = 0;

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

  readonly tabs = [...ADR_TAB_ORDER, { key: 'audit' as const, label: 'Audit Log' }];
  readonly statusOptions = ADR_STATUS_OPTIONS;
  auditEvents: AuditEventDto[] = [];
  auditLoading = false;
  auditFilter: AuditEventType | 'ALL' = 'ALL';
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

    if (changes['adr']) {
      this.auditEvents = [];
      if (this.activeTab === 'audit' && this.adr?.id) {
        this.loadAuditLog();
      }
    }

    if (changes['auditRefreshToken'] && !changes['auditRefreshToken'].firstChange && this.adr?.id) {
      this.loadAuditLog();
    }
  }

  get currentContent(): string {
    if (!this.adr) {
      return '';
    }

    if (this.activeTab === 'audit') {
      return '';
    }

    return this.adr[this.activeTab] ?? '';
  }

  get filteredAuditEvents(): AuditEventDto[] {
    if (this.auditFilter === 'ALL') {
      return this.auditEvents;
    }

    return this.auditEvents.filter((event) => event.type === this.auditFilter);
  }

  get currentTags(): string[] {
    return this.form.controls.tags.value;
  }

  get formattedCreatedAt(): string {
    return this.adr?.createdAt?.slice(0, 10) ?? '';
  }

  get statusClass(): string {
    const status = this.adr?.status ?? 'DRAFT';
    return `status-${status.toLowerCase()}`;
  }

  togglePreview(): void {
    this.isPreviewMode = !this.isPreviewMode;
  }

  onEditClick(): void {
    this.isPreviewMode = false;
    this.editRequested.emit();
  }

  onTabClick(tab: AdrTabKey | 'audit'): void {
    this.tabChanged.emit(tab as AdrTabKey);
    this.onTabChange(tab);
  }

  loadAuditLog(): void {
    if (!this.adr?.id) {
      return;
    }

    this.auditLoading = true;
    this.adrService.getAuditLog(this.adr.id).subscribe({
      next: (events) => {
        this.auditEvents = events.map((event) => ({
          ...event,
          actor: event.actor?.trim() ? event.actor : 'System'
        }));
        this.auditLoading = false;
      },
      error: () => {
        this.auditLoading = false;
      }
    });
  }

  onTabChange(tab: string): void {
    this.activeTab = tab as 'context' | 'decision' | 'consequences' | 'alternatives' | 'audit';
    if (tab === 'audit' && this.auditEvents.length === 0) {
      this.loadAuditLog();
    }
  }

  getDotColor(type: AuditEventType): string {
    const colors: Record<string, string> = {
      STATUS_CHANGED: '#1d9e75',
      ADR_CREATED: '#6366f1',
      ADR_UPDATED: '#ba7517',
      VOTE_CAST: '#3b82f6',
      COMMENT_ADDED: '#9ca3af'
    };
    return colors[type] || '#9ca3af';
  }

  getBadgeClass(type: AuditEventType): string {
    const map: Record<string, string> = {
      STATUS_CHANGED: 'badge-status',
      ADR_CREATED: 'badge-created',
      ADR_UPDATED: 'badge-updated',
      VOTE_CAST: 'badge-vote',
      COMMENT_ADDED: 'badge-comment'
    };
    return map[type] || 'badge-comment';
  }

  getBadgeLabel(type: AuditEventType): string {
    return type.replace('_', ' ');
  }

  formatEventTimestamp(timestamp: string): string {
    const parsedDate = new Date(timestamp);
    if (Number.isNaN(parsedDate.getTime())) {
      return timestamp;
    }

    return formatDate(parsedDate, 'yyyy-MM-dd · h:mm a', 'en-US');
  }

  exportCsv(): void {
    const rows = this.auditEvents.map(
      (event) => `"${event.timestamp}","${event.type}","${event.actor}","${event.action}","${event.detail || ''}"`
    );
    const csv = ['Timestamp,Type,Actor,Action,Detail', ...rows].join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `audit-adr-${this.adr?.adrNumber}.csv`;
    anchor.click();
    URL.revokeObjectURL(url);
  }

  onAddTag(): void {
    const trimmed = this.tagInput.trim();
    if (!trimmed || this.currentTags.includes(trimmed)) {
      return;
    }

    this.form.controls.tags.setValue([...this.currentTags, trimmed].filter(Boolean));
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
