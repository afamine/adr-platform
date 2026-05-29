import { DestroyRef, Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, interval } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { NotificationApiDto } from '../models/notification.models';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class NotificationCenterService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = environment.apiUrl;

  private readonly _unreadCount$ = new BehaviorSubject<number>(0);
  readonly unreadCount$ = this._unreadCount$.asObservable();

  getNotifications(limit = 20, unreadOnly = false): Observable<NotificationApiDto[]> {
    return this.http.get<NotificationApiDto[]>(
      `${this.API_URL}/api/notifications?limit=${limit}&unreadOnly=${unreadOnly}`
    );
  }

  markRead(id: string): Observable<{ message: string }> {
    return this.http.patch<{ message: string }>(
      `${this.API_URL}/api/notifications/${id}/read`, {}
    );
  }

  markAllRead(): Observable<{ message: string; count: number }> {
    return this.http.patch<{ message: string; count: number }>(
      `${this.API_URL}/api/notifications/mark-all-read`, {}
    );
  }

  fetchUnreadCount(): void {
    this.http.get<{ count: number }>(`${this.API_URL}/api/notifications/unread-count`)
      .pipe(catchError(() => []))
      .subscribe(res => {
        if (res && typeof (res as any).count === 'number') {
          this._unreadCount$.next((res as any).count);
        }
      });
  }

  startPolling(destroyRef: DestroyRef): void {
    this.fetchUnreadCount();
    interval(30_000)
      .pipe(takeUntilDestroyed(destroyRef))
      .subscribe(() => this.fetchUnreadCount());
  }
}
