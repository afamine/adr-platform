import { CommonModule } from '@angular/common';
import { Component, DestroyRef, ElementRef, HostListener, NgZone, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';

import { BellNotification, NotificationApiDto } from '../../models/notification.models';
import { NotificationCenterService } from '../../services/notification-center.service';

@Component({
  selector: 'app-notification-dropdown',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notification-dropdown.component.html',
  styleUrls: ['./notification-dropdown.component.scss']
})
export class NotificationDropdownComponent implements OnInit {
  private readonly notifService = inject(NotificationCenterService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly elementRef = inject(ElementRef<HTMLElement>);
  private readonly ngZone = inject(NgZone);
  private readonly router = inject(Router);

  readonly liveUnreadCount$ = this.notifService.unreadCount$;

  notifications: BellNotification[] = [];
  activeTab: 'all' | 'unread' = 'all';
  isOpen = false;
  isLoading = false;

  private readonly dotColors: Record<string, string> = {
    ADR_SUBMITTED_FOR_REVIEW: '#ba7517',
    VOTE_CAST_ON_MY_ADR: '#6366f1',
    ADR_STATUS_CHANGED: '#1d9e75',
    ADR_REJECTED: '#ef4444',
    NEW_TEAM_MEMBER: '#9ca3af',
    COMMENT_ADDED: '#9ca3af'
  };

  private readonly actionLabels: Record<string, string> = {
    ADR_SUBMITTED_FOR_REVIEW: 'Review ADR',
    VOTE_CAST_ON_MY_ADR: '',
    ADR_STATUS_CHANGED: 'View ADR',
    ADR_REJECTED: 'View ADR',
    NEW_TEAM_MEMBER: '',
    COMMENT_ADDED: 'View ADR'
  };

  ngOnInit(): void {
    this.notifService.startPolling(this.destroyRef);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.isOpen || this.elementRef.nativeElement.contains(event.target as Node)) {
      return;
    }
    this.close();
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.close();
  }

  get unreadCount(): number {
    return this.notifications.filter((n) => n.unread).length;
  }

  get displayedNotifications(): BellNotification[] {
    return this.activeTab === 'unread'
      ? this.notifications.filter((n) => n.unread)
      : this.notifications;
  }

  toggle(event: Event): void {
    event.stopPropagation();
    this.isOpen = !this.isOpen;
    if (this.isOpen) {
      this.loadNotifications();
    }
  }

  close(): void {
    this.isOpen = false;
  }

  setActiveTab(tab: 'all' | 'unread'): void {
    this.activeTab = tab;
  }

  markAllRead(): void {
    if (!this.notifications.length) {
      return;
    }

    const previous = this.notifications.map((n) => n.unread);
    this.notifications.forEach((n) => (n.unread = false));
    this.notifService.markAllRead().subscribe({
      next: () => this.notifService.fetchUnreadCount(),
      error: () => this.notifications.forEach((n, i) => (n.unread = previous[i]))
    });
  }

  markRead(id: string): void {
    const notification = this.notifications.find((n) => n.id === id);
    if (!notification || !notification.unread) {
      return;
    }

    notification.unread = false;
    this.notifService.markRead(id).subscribe({
      next: () => this.notifService.fetchUnreadCount(),
      error: () => {
        notification.unread = true;
      }
    });
  }

  openNotificationAction(event: Event, notification: BellNotification): void {
    event.stopPropagation();
    this.markRead(notification.id);
    this.close();

    if (notification.adrId) {
      void this.router.navigate(['/adrs']);
    }
  }

  loadNotifications(): void {
    this.isLoading = true;
    this.notifService.getNotifications(20, false).subscribe({
      next: (items) => {
        this.ngZone.run(() => {
          this.notifications = items.map((dto) => this.map(dto));
          this.isLoading = false;
        });
      },
      error: () => {
        this.ngZone.run(() => {
          this.notifications = [];
          this.isLoading = false;
        });
      }
    });
  }

  private map(dto: NotificationApiDto): BellNotification {
    return {
      id: dto.id,
      type: dto.type,
      dotColor: this.dotColors[dto.type] ?? '#9ca3af',
      title: dto.title,
      body: dto.body,
      time: dto.timeAgo,
      action: this.actionLabels[dto.type] ?? '',
      adrId: dto.adrId,
      unread: !dto.isRead
    };
  }
}
