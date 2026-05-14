import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { NotificationApiDto } from '../models/notification.models';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class NotificationCenterService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = environment.apiUrl;

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
}
