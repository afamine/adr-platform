import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { Adr, AiInsight } from '../../../../models/adr.model';
import { AdrService } from '../../../../services/adr.service';

type InsightImpact = AiInsight['impact'];

@Component({
  selector: 'app-ai-assistant-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ai-assistant-panel.component.html',
  styleUrl: './ai-assistant-panel.component.scss'
})
export class AiAssistantPanelComponent implements OnChanges {
  private readonly adrService = inject(AdrService);
  private readonly cdr = inject(ChangeDetectorRef);

  @Input() selectedAdr: Adr | null = null;
  @Output() closePanel = new EventEmitter<void>();

  insights: AiInsight[] = [];
  isLoadingInsights = false;
  insightsError = false;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['selectedAdr']) {
      this.insights = [];
      this.insightsError = false;

      if (this.selectedAdr?.id) {
        this.loadAiInsights(this.selectedAdr.id);
      }
    }
  }

  loadAiInsights(adrId: string): void {
    if (!adrId) return;
    this.isLoadingInsights = true;
    this.insightsError = false;

    this.adrService.getAiInsights(adrId).subscribe({
      next: (insights: AiInsight[]) => {
        this.insights = insights;
        this.isLoadingInsights = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoadingInsights = false;
        this.insightsError = true;
        this.insights = [];
        this.cdr.detectChanges();
      }
    });
  }

  getImpactLabel(impact: InsightImpact): string {
    if (impact === 'high') {
      return 'high impact';
    }

    if (impact === 'medium') {
      return 'medium impact';
    }

    return 'low impact';
  }

  getConfidenceBadgeClass(impact: InsightImpact): string {
    if (impact === 'high') {
      return 'ai-panel__confidence--high';
    }

    if (impact === 'medium') {
      return 'ai-panel__confidence--medium';
    }

    return 'ai-panel__confidence--low';
  }

  getInsightIcon(impact: InsightImpact): string {
    if (impact === 'medium') {
      return '⚠';
    }

    if (impact === 'low') {
      return '✓';
    }

    return '💡';
  }
}
