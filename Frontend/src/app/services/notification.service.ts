import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export type NotifType = 'success' | 'error' | 'warning' | 'info';

export interface Notif {
  id: string;
  type: NotifType;
  title: string;
  message?: string;
  duration: number;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private _notifs = new BehaviorSubject<Notif[]>([]);
  notifs$ = this._notifs.asObservable();

  success(title: string, message?: string) {
    this.add('success', title, message, 4000);
  }
  error(title: string, message?: string) {
    this.add('error', title, message, 6000);
  }
  warning(title: string, message?: string) {
    this.add('warning', title, message, 5000);
  }
  info(title: string, message?: string) {
    this.add('info', title, message, 4000);
  }

  dismiss(id: string) {
    this._notifs.next(this._notifs.value.filter((n) => n.id !== id));
  }

  private add(type: NotifType, title: string, message?: string, duration = 4000) {
    const id = Math.random().toString(36).slice(2);
    this._notifs.next([...this._notifs.value, { id, type, title, message, duration }]);
    if (duration > 0) setTimeout(() => this.dismiss(id), duration);
  }

  // Backward-compatible helpers used by existing ADR dashboard template
  notifications(): Array<{ id: string; type: NotifType; message: string }> {
    return this._notifs.value.map((n) => ({
      id: n.id,
      type: n.type,
      message: n.message?.trim() ? `${n.title} — ${n.message}` : n.title
    }));
  }

  remove(id: string): void {
    this.dismiss(id);
  }
}
