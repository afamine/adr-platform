import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminLayoutComponent } from '../../../layouts/admin-layout/admin-layout.component';
import { AdminAuditEvent, AdminAuditService, AuditActorSummary } from '../../../services/admin-audit.service';
import { Subject, switchMap, takeUntil, tap } from 'rxjs';

@Component({
  selector: 'app-audit-log',
  standalone: true,
  imports: [CommonModule, FormsModule, AdminLayoutComponent],
  templateUrl: './audit-log.component.html',
  styleUrls: ['./audit-log.component.scss']
})
export class AuditLogComponent implements OnInit, OnDestroy {
  private readonly adminAuditService = inject(AdminAuditService);
  private readonly filterChange$ = new Subject<void>();
  private readonly destroy$ = new Subject<void>();

  events: AdminAuditEvent[] = [];
  actors: AuditActorSummary[] = [];
  totalElements = 0;
  totalPages = 0;
  isLoading = false;
  expandedRowId: string | null = null;

  selectedActorId = '';
  selectedAction = '';
  selectedDateRange = 'last-7-days';
  currentPage = 0;
  pageSize = 25;

  readonly actionOptions = [
    { value: '', label: 'All actions' },
    { value: 'ADR_CREATED', label: 'ADR Created' },
    { value: 'ADR_UPDATED', label: 'ADR Updated' },
    { value: 'ADR_STATUS_CHANGED', label: 'Status Changed' },
    { value: 'VOTE_CAST', label: 'Vote Cast' },
    { value: 'COMMENT_ADDED', label: 'Comment Added' },
    { value: 'ROLE_CHANGED', label: 'User Role Changed' },
    { value: 'USER_LOGGED_IN', label: 'User Logged In' },
    { value: 'USER_LOGGED_OUT', label: 'User Logged Out' },
    { value: 'PASSWORD_CHANGED', label: 'Password Changed' }
  ];

  readonly dateRangeOptions = [
    { value: 'last-7-days', label: 'Last 7 days' },
    { value: 'last-30-days', label: 'Last 30 days' },
    { value: 'last-90-days', label: 'Last 90 days' }
  ];

  readonly actionColors: Record<string, { bg: string; color: string }> = {
    ADR_CREATED: { bg: 'rgba(99,102,241,0.1)', color: '#6366f1' },
    ADR_UPDATED: { bg: 'rgba(186,117,23,0.1)', color: '#ba7517' },
    ADR_STATUS_CHANGED: { bg: 'rgba(29,158,117,0.1)', color: '#1d9e75' },
    STATUS_CHANGED: { bg: 'rgba(29,158,117,0.1)', color: '#1d9e75' },
    VOTE_CAST: { bg: 'rgba(59,130,246,0.1)', color: '#3b82f6' },
    COMMENT_ADDED: { bg: 'rgba(139,92,246,0.1)', color: '#8b5cf6' },
    ROLE_CHANGED: { bg: 'rgba(139,92,246,0.1)', color: '#8b5cf6' },
    USER_LOGGED_IN: { bg: 'rgba(156,163,175,0.1)', color: '#9ca3af' },
    USER_LOGGED_OUT: { bg: 'rgba(156,163,175,0.1)', color: '#9ca3af' },
    PASSWORD_CHANGED: { bg: 'rgba(156,163,175,0.1)', color: '#9ca3af' }
  };

  ngOnInit(): void {
    this.loadActors();
    this.filterChange$
      .pipe(
        tap(() => {
          this.isLoading = true;
        }),
        switchMap(() => {
          const { from, to } = this.getDateRange();

          return this.adminAuditService.getAuditLog({
            actorId: this.selectedActorId || undefined,
            action: this.selectedAction || undefined,
            from: from?.toISOString(),
            to: to?.toISOString(),
            page: this.currentPage,
            size: this.pageSize
          });
        }),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (result) => {
          this.events = result.content;
          this.totalElements = result.totalElements;
          this.totalPages = result.totalPages;
          this.isLoading = false;
        },
        error: () => {
          this.isLoading = false;
        }
      });

    this.filterChange$.next();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadActors(): void {
    this.adminAuditService.getActors().subscribe({
      next: (actors) => {
        this.actors = actors;
      }
    });
  }

  private getDateRange(): { from: Date | null; to: Date | null } {
    const now = new Date();
    const daysMap: Record<string, number> = {
      'last-7-days': 7,
      'last-30-days': 30,
      'last-90-days': 90
    };

    const days = daysMap[this.selectedDateRange];
    if (!days) {
      return { from: null, to: null };
    }

    const from = new Date(now.getTime() - days * 24 * 60 * 60 * 1000);
    return { from, to: now };
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.expandedRowId = null;
    this.filterChange$.next();
  }

  toggleRow(eventId: string): void {
    this.expandedRowId = this.expandedRowId === eventId ? null : eventId;
  }

  onPageChange(page: number): void {
    if (page < 0 || page >= this.totalPages) {
      return;
    }

    this.currentPage = page;
    this.expandedRowId = null;
    this.filterChange$.next();
  }

  onPageSizeChange(size: number): void {
    this.pageSize = Number(size);
    this.currentPage = 0;
    this.expandedRowId = null;
    this.filterChange$.next();
  }

  onExport(): void {
    const { from, to } = this.getDateRange();
    this.adminAuditService.exportCsv({
      actorId: this.selectedActorId || undefined,
      action: this.selectedAction || undefined,
      from: from?.toISOString(),
      to: to?.toISOString()
    });
  }

  formatTimestamp(isoString: string): string {
    if (!isoString) {
      return '';
    }

    const date = new Date(isoString);
    return (
      date.toLocaleDateString('en-US', { month: 'short', day: '2-digit', year: 'numeric' }) +
      ' · ' +
      date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: false })
    );
  }

  getActionBadgeStyle(action: string): { background: string; color: string } {
    const color = this.actionColors[action] || { bg: 'rgba(156,163,175,0.1)', color: '#9ca3af' };
    return { background: color.bg, color: color.color };
  }

  prettifyJson(jsonStr: string | null): string {
    if (!jsonStr) {
      return '';
    }

    try {
      return JSON.stringify(JSON.parse(jsonStr), null, 2);
    } catch {
      return jsonStr;
    }
  }

  get pageSizeOptions(): number[] {
    return [25, 50, 100];
  }

  get pages(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i);
  }
}
