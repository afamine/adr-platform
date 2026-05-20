import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ChangeDetectorRef, Component, EventEmitter, Input, NgZone, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { environment } from '../../../../../environments/environment';
import { Adr, AiInsight } from '../../../../models/adr.model';

type InsightImpact = AiInsight['impact'];

@Component({
  selector: 'app-ai-assistant-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ai-assistant-panel.component.html',
  styleUrl: './ai-assistant-panel.component.scss'
})
export class AiAssistantPanelComponent implements OnChanges {
  private readonly http = inject(HttpClient);
  private readonly ngZone = inject(NgZone);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly apiUrl = environment.apiUrl;

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

    this.http.get<AiInsight[]>(`${this.apiUrl}/api/adrs/${adrId}/ai-insights`).subscribe({
      next: (insights) => {
        this.ngZone.run(() => {
          this.insights = insights;
          this.isLoadingInsights = false;
          this.cdr.detectChanges();
        });
      },
      error: () => {
        this.ngZone.run(() => {
          this.isLoadingInsights = false;
          this.insightsError = true;
          this.insights = [];
          this.cdr.detectChanges();
        });
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
