import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, NgZone, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';

import { BellNotification, NotificationApiDto } from '../../models/notification.models';
import { NotificationCenterService } from '../../services/notification-center.service';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notifications.component.html',
  styleUrls: ['./notifications.component.scss']
})
export class NotificationsComponent implements OnInit {
  private readonly notifService = inject(NotificationCenterService);
  private readonly router = inject(Router);
  private readonly ngZone = inject(NgZone);
  private readonly cdr = inject(ChangeDetectorRef);

  notifications: BellNotification[] = [];
  activeTab: 'all' | 'unread' = 'all';
  isLoading = false;

  private readonly dotColors: Record<string, string> = {
    ADR_SUBMITTED_FOR_REVIEW: '#ba7517',
    VOTE_CAST_ON_MY_ADR:      '#6366f1',
    ADR_STATUS_CHANGED:       '#1d9e75',
    ADR_REJECTED:             '#ef4444',
    NEW_TEAM_MEMBER:          '#9ca3af',
    COMMENT_ADDED:            '#9ca3af'
  };

  private readonly actionLabels: Record<string, string> = {
    ADR_SUBMITTED_FOR_REVIEW: 'Review ADR',
    VOTE_CAST_ON_MY_ADR:      '',
    ADR_STATUS_CHANGED:       'View ADR',
    ADR_REJECTED:             'View ADR',
    NEW_TEAM_MEMBER:          '',
    COMMENT_ADDED:            'View ADR'
  };

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading = true;
    this.notifService.getNotifications(100, false).subscribe({
      next: (items) => {
        this.ngZone.run(() => {
          this.notifications = items.map((dto) => this.map(dto));
          this.isLoading = false;
          this.cdr.detectChanges();
        });
      },
      error: () => {
        this.ngZone.run(() => {
          this.isLoading = false;
          this.notifications = [];
          this.cdr.detectChanges();
        });
      }
    });
  }

  get displayed(): BellNotification[] {
    return this.activeTab === 'unread'
      ? this.notifications.filter((n) => n.unread)
      : this.notifications;
  }

  get unreadCount(): number {
    return this.notifications.filter((n) => n.unread).length;
  }

  markAllRead(): void {
    const prev = this.notifications.map((n) => n.unread);
    this.notifications.forEach((n) => (n.unread = false));
    this.notifService.markAllRead().subscribe({
      error: () => this.notifications.forEach((n, i) => (n.unread = prev[i]))
    });
  }

  markRead(id: string): void {
    const n = this.notifications.find((n) => n.id === id);
    if (!n || !n.unread) return;
    n.unread = false;
    this.notifService.markRead(id).subscribe({
      error: () => { if (n) n.unread = true; }
    });
  }

  goBack(): void {
    void this.router.navigate(['/adrs']);
  }

  private map(dto: NotificationApiDto): BellNotification {
    return {
      id:       dto.id,
      type:     dto.type,
      dotColor: this.dotColors[dto.type] ?? '#9ca3af',
      title:    dto.title,
      body:     dto.body,
      time:     dto.timeAgo,
      action:   this.actionLabels[dto.type] ?? '',
      adrId:    dto.adrId,
      unread:   !dto.isRead
    };
  }
}
