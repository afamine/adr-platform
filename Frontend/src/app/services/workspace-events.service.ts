import { Injectable, inject } from '@angular/core';
import { Subject } from 'rxjs';

import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

export interface WorkspaceEvent {
  type: string;
  workspaceId: string;
  adrId: string | null;
  occurredAt: string;
}

@Injectable({ providedIn: 'root' })
export class WorkspaceEventsService {
  private readonly auth = inject(AuthService);
  private readonly eventsSubject = new Subject<WorkspaceEvent>();
  readonly events$ = this.eventsSubject.asObservable();
  private abortController: AbortController | null = null;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private connecting = false;

  connect(): void {
    if (this.connecting || this.abortController) return;
    void this.openStream();
  }

  disconnect(): void {
    this.abortController?.abort();
    this.abortController = null;
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer);
    this.reconnectTimer = null;
    this.connecting = false;
  }

  private async openStream(): Promise<void> {
    const token = this.auth.getToken();
    if (!token) return;
    this.connecting = true;
    const controller = new AbortController();
    this.abortController = controller;
    try {
      const response = await fetch(`${environment.apiUrl}/api/events/stream`, {
        headers: { Authorization: `Bearer ${token}`, Accept: 'text/event-stream' },
        credentials: 'include',
        signal: controller.signal
      });
      if (!response.ok || !response.body) throw new Error(`Event stream failed: ${response.status}`);
      await this.readStream(response.body, controller.signal);
    } catch (error) {
      if (!controller.signal.aborted) console.warn('[Axiom] Workspace event stream disconnected', error);
    } finally {
      if (this.abortController === controller) this.abortController = null;
      this.connecting = false;
      if (!controller.signal.aborted && this.auth.getToken()) {
        this.reconnectTimer = setTimeout(() => this.connect(), 5000);
      }
    }
  }

  private async readStream(stream: ReadableStream<Uint8Array>, signal: AbortSignal): Promise<void> {
    const reader = stream.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    while (!signal.aborted) {
      const { done, value } = await reader.read();
      if (done) return;
      buffer += decoder.decode(value, { stream: true });
      const blocks = buffer.split('\n\n');
      buffer = blocks.pop() ?? '';
      blocks.forEach(block => this.parseBlock(block));
    }
  }

  private parseBlock(block: string): void {
    const normalized = block.replace(/\r/g, '');
    const event = normalized.split('\n').find(line => line.startsWith('event:'))?.slice(6).trim();
    const data = normalized.split('\n').find(line => line.startsWith('data:'))?.slice(5).trim();
    if (event !== 'workspace-event' || !data) return;
    try { this.eventsSubject.next(JSON.parse(data) as WorkspaceEvent); } catch { /* ignore malformed event */ }
  }
}