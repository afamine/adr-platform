import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { AfterViewInit, ChangeDetectorRef, Component, DestroyRef, ElementRef, NgZone, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { Chart, registerables } from 'chart.js';
import { forkJoin } from 'rxjs';

import { AnalyticsTimeRange, KpiResponse, RecentAdrDto, StatusCount, WeeklyActivity } from '../../../models/analytics.models';
import { AnalyticsService } from '../../../services/analytics.service';
import { NotificationService } from '../../../services/notification.service';
import { WorkspaceEventsService } from '../../../services/workspace-events.service';
import { AxiomLogoComponent } from '../../../shared/axiom-logo/axiom-logo.component';

Chart.register(...registerables);

interface BarItem { status: string; count: number; color: string; }
interface ActivityRow { id: string; title: string; status: string; author: string; action: string; date: string; }
interface TimeRangeOption { label: string; value: AnalyticsTimeRange; }

@Component({
  selector: 'app-analytics-dashboard',
  standalone: true,
  imports: [CommonModule, AxiomLogoComponent],
  templateUrl: './analytics-dashboard.component.html',
  styleUrls: ['./analytics-dashboard.component.scss']
})
export class AnalyticsDashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly router = inject(Router);
  private readonly analyticsService = inject(AnalyticsService);
  private readonly notificationService = inject(NotificationService);
  private readonly ngZone = inject(NgZone);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);
  private readonly workspaceEvents = inject(WorkspaceEventsService);
  private lineChart: Chart | null = null;
  private chartReady = false;
  private pendingWeekly: WeeklyActivity[] | null = null;

  @ViewChild('lineChart') lineChartRef!: ElementRef<HTMLCanvasElement>;

  isLoading = false;
  selectedTimeRange: AnalyticsTimeRange = '30d';
  readonly timeRangeOptions: TimeRangeOption[] = [
    { label: 'Last 24 hours', value: '24h' },
    { label: 'Last 7 days', value: '7d' },
    { label: 'Last 30 days', value: '30d' },
    { label: 'Last 90 days', value: '90d' },
    { label: 'All time', value: 'all' }
  ];

  kpis = {
    totalAdrs: 0,
    acceptanceRate: '—',
    avgReviewTime: '—',
    pendingVotes: 0,
    totalAdrsSubtext: '+0 this month',
    acceptanceSubtext: '0 accepted / 0 rejected',
    reviewTimeDelta: '',
    reviewTimeDeltaImproving: true,
    pendingSubtext: '0 ADRs awaiting APPROVER decision'
  };

  barData: BarItem[] = [];
  maxBarCount = 1;
  recentActivity: ActivityRow[] = [];

  private readonly statusColors: Record<string, string> = {
    ACCEPTED:     '#1d9e75',
    PROPOSED:     '#6366f1',
    DRAFT:        '#9ca3af',
    UNDER_REVIEW: '#ba7517',
    REJECTED:     '#ef4444',
    SUPERSEDED:   '#6b7280'
  };

  ngOnInit(): void {
    this.loadDashboard();
    this.workspaceEvents.events$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.loadDashboard());
  }

  loadDashboard(): void {
    this.isLoading = true;
    const timeRange = this.selectedTimeRange;
    forkJoin({
      kpis: this.analyticsService.getKpis(timeRange),
      distribution: this.analyticsService.getStatusDistribution(timeRange),
      weekly: this.analyticsService.getWeeklyActivity(timeRange),
      recent: this.analyticsService.getRecentAdrs(4, timeRange)
    }).subscribe({
      next: ({ kpis, distribution, weekly, recent }) => {
        this.ngZone.run(() => {
          this.isLoading = false;
          this.applyKpis(kpis);
          this.applyDistribution(distribution);
          this.applyWeekly(weekly);
          this.applyRecent(recent);
          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          this.isLoading = false;
          this.notificationService.error('Failed to load analytics', this.getErrorMessage(err));
          this.cdr.detectChanges();
        });
      }
    });
  }

  get selectedTimeRangeLabel(): string {
    return this.timeRangeOptions.find((option) => option.value === this.selectedTimeRange)?.label ?? 'Last 30 days';
  }

  setTimeRange(timeRange: AnalyticsTimeRange): void {
    if (this.selectedTimeRange === timeRange || this.isLoading) {
      return;
    }
    this.selectedTimeRange = timeRange;
    this.loadDashboard();
  }

  ngAfterViewInit(): void {
    this.initChart(['W1', 'W2', 'W3', 'W4', 'W5'], [0, 0, 0, 0, 0]);
    this.chartReady = true;
    if (this.pendingWeekly) {
      this.updateChart(this.pendingWeekly);
      this.pendingWeekly = null;
    }
  }

  ngOnDestroy(): void {
    this.lineChart?.destroy();
  }

  barWidthPct(count: number): number {
    return (count / this.maxBarCount) * 100;
  }

  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      ACCEPTED:     'badge-accepted',
      PROPOSED:     'badge-proposed',
      UNDER_REVIEW: 'badge-under-review',
      DRAFT:        'badge-draft',
      REJECTED:     'badge-rejected'
    };
    return map[status] ?? 'badge-draft';
  }

  navigateTo(path: string): void {
    void this.router.navigate([path]);
  }

  private applyKpis(k: KpiResponse): void {
    const delta = k.avgReviewTimeDelta;
    this.kpis = {
      totalAdrs: k.totalAdrs,
      acceptanceRate: k.acceptanceRate.toFixed(0) + '%',
      avgReviewTime: k.avgReviewTimeDays.toFixed(1) + 'd',
      pendingVotes: k.pendingVotes,
      totalAdrsSubtext: this.selectedTimeRangeLabel,
      acceptanceSubtext: `${k.acceptedCount} accepted / ${k.rejectedCount} rejected`,
      reviewTimeDelta: delta != null ? `${delta > 0 ? '+' : ''}${delta.toFixed(1)}d vs previous period` : '',
      reviewTimeDeltaImproving: delta == null || delta <= 0,
      pendingSubtext: `${k.pendingApproverDecisions} ADRs awaiting APPROVER decision`
    };
  }

  private applyDistribution(dist: StatusCount[]): void {
    this.barData = dist.map(d => ({
      status: d.status,
      count: d.count,
      color: this.statusColors[d.status] ?? '#9ca3af'
    }));
    this.maxBarCount = Math.max(...this.barData.map(b => b.count), 1);
  }

  private applyWeekly(weekly: WeeklyActivity[]): void {
    if (!this.chartReady) {
      this.pendingWeekly = weekly;
      return;
    }
    this.updateChart(weekly);
  }

  private updateChart(weekly: WeeklyActivity[]): void {
    if (!this.lineChart) return;
    const labels = weekly.map(w => w.label || w.week);
    const counts = weekly.map(w => w.count);
    this.lineChart.data.labels = labels;
    this.lineChart.data.datasets[0].data = counts;
    (this.lineChart.options.scales!['y'] as Record<string, unknown>)['max'] = Math.max(...counts, 1) + 1;
    this.lineChart.update();
  }

  private applyRecent(adrs: RecentAdrDto[]): void {
    this.recentActivity = adrs.map(a => ({
      id: '#' + a.adrNumber,
      title: a.title,
      status: a.status,
      author: a.authorName,
      action: a.lastAction ?? 'Created',
      date: this.formatDate(a.lastActionDate)
    }));
  }

  private initChart(labels: string[], data: number[]): void {
    this.lineChart = new Chart(this.lineChartRef.nativeElement, {
      type: 'line',
      data: {
        labels,
        datasets: [{
          data,
          borderColor: '#6366f1',
          backgroundColor: 'rgba(99,102,241,0.10)',
          fill: true,
          tension: 0.4,
          pointBackgroundColor: '#6366f1',
          pointBorderColor: '#fff',
          pointBorderWidth: 2,
          pointRadius: 5
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          y: { beginAtZero: true, max: 6, ticks: { stepSize: 1, font: { size: 11 } }, grid: { color: '#f3f4f6' } },
          x: { grid: { display: false }, ticks: { font: { size: 11 } } }
        }
      }
    });
  }

  private formatDate(iso: string | null | undefined): string {
    if (!iso) return '—';
    const d = new Date(iso);
    if (isNaN(d.getTime())) return '—';
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric' }).format(d);
  }

  private getErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const body = error.error as { message?: string } | null;
      return body?.message || error.message || 'Please try again.';
    }
    return 'Please try again.';
  }
}
