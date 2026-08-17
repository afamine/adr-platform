import { CommonModule } from '@angular/common';
import { Component, EventEmitter, HostListener, Input, OnChanges, OnDestroy, Output, SimpleChanges, inject } from '@angular/core';
import { Adr, AdrTabKey, AiAnalysisResult, AiInsight } from '../../../../models/adr.model';
import { AdrService } from '../../../../services/adr.service';

@Component({
  selector: 'app-ai-assistant-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ai-assistant-panel.component.html',
  styleUrl: './ai-assistant-panel.component.scss'
})
export class AiAssistantPanelComponent implements OnChanges, OnDestroy {
  private readonly adrService = inject(AdrService);
  private pollTimer: ReturnType<typeof setTimeout> | null = null;

  @Input() selectedAdr: Adr | null = null;
  @Output() closePanel = new EventEmitter<void>();
  @Output() sourceNavigate = new EventEmitter<AdrTabKey>();

  analysis: AiAnalysisResult | null = null;
  isLoadingLatest = false;
  isRefreshing = false;
  isCollapsed = false;
  loadError: string | null = null;
  readonly expandedInsightIds = new Set<string>();

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['selectedAdr']) {
      this.cancelPolling();
      this.analysis = null;
      this.loadError = null;
      this.expandedInsightIds.clear();
      if (this.selectedAdr?.id) this.loadLatest(this.selectedAdr.id);
    }
  }

  ngOnDestroy(): void { this.cancelPolling(); }

  @HostListener('document:keydown.escape')
  onEscape(): void { this.closePanel.emit(); }

  refreshAnalysis(): void {
    const adrId = this.selectedAdr?.id;
    if (!adrId || this.isRefreshing) return;
    this.cancelPolling();
    this.isRefreshing = true;
    this.loadError = null;
    this.adrService.analyzeAiInsights(adrId).subscribe({
      next: () => this.loadLatest(adrId, true),
      error: (error) => {
        this.isRefreshing = false;
        this.loadError = error?.message || 'AI service temporarily unavailable. Please try again.';
      }
    });
  }

  toggleInsight(insight: AiInsight): void {
    if (this.expandedInsightIds.has(insight.id)) this.expandedInsightIds.delete(insight.id);
    else this.expandedInsightIds.add(insight.id);
  }

  isExpanded(insight: AiInsight): boolean { return this.expandedInsightIds.has(insight.id); }

  navigateToSource(source: AdrTabKey): void { this.sourceNavigate.emit(source); }

  confidenceClass(confidence: number): string {
    if (confidence >= 80) return 'ai-panel__confidence--high';
    if (confidence >= 60) return 'ai-panel__confidence--medium';
    return 'ai-panel__confidence--low';
  }

  impactLabel(impact: AiInsight['impact']): string { return `${impact[0]}${impact.slice(1).toLowerCase()} impact`; }

  private loadLatest(adrId: string, afterRefresh = false): void {
    if (!afterRefresh) this.isLoadingLatest = true;
    this.adrService.getLatestAiAnalysis(adrId).subscribe({
      next: (analysis) => {
        if (this.selectedAdr?.id !== adrId) return;
        this.analysis = analysis;
        this.isLoadingLatest = false;
        if (analysis.status === 'IN_PROGRESS') {
          this.schedulePolling(adrId);
        } else {
          this.isRefreshing = false;
        }
      },
      error: (error) => {
        if (this.selectedAdr?.id !== adrId) return;
        this.isLoadingLatest = false;
        this.isRefreshing = false;
        this.loadError = error?.message || 'Unable to load AI analysis.';
      }
    });
  }

  private schedulePolling(adrId: string): void {
    this.cancelPolling();
    this.pollTimer = setTimeout(() => this.loadLatest(adrId, true), 1200);
  }

  private cancelPolling(): void {
    if (this.pollTimer) clearTimeout(this.pollTimer);
    this.pollTimer = null;
  }
}
