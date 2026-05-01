import { Injectable, signal } from '@angular/core';

export type NotificationType = 'success' | 'warning' | 'error' | 'info';

export interface NotificationItem {
  id: number;
  type: NotificationType;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  readonly notifications = signal<NotificationItem[]>([]);
  private nextId = 1;

  success(message: string): void {
    this.push('success', message);
  }

  warning(message: string): void {
    this.push('warning', message);
  }

  error(message: string): void {
    this.push('error', message);
  }

  info(message: string): void {
    this.push('info', message);
  }

  remove(id: number): void {
    this.notifications.update((items) => items.filter((item) => item.id !== id));
  }

  private push(type: NotificationType, message: string): void {
    const id = this.nextId++;
    this.notifications.update((items) => [...items, { id, type, message }]);
    window.setTimeout(() => this.remove(id), 4000);
  }
}
