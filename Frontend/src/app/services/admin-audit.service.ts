import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AdminAuditEvent {
  id: string;
  timestamp: string;
  actorId: string | null;
  actorName: string;
  actorEmail: string | null;
  actorInitials: string;
  action: string;
  actionLabel: string;
  entityType: string;
  entityId: string | null;
  entityLabel: string | null;
  detail: string | null;
  oldValueJson: string | null;
  newValueJson: string | null;
  ipAddress: string | null;
}

export interface AuditActorSummary {
  id: string;
  fullName: string;
  email: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class AdminAuditService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  getAuditLog(params: {
    actorId?: string;
    action?: string;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
  }): Observable<PageResponse<AdminAuditEvent>> {
    let httpParams = new HttpParams();

    Object.entries(params).forEach(([key, val]) => {
      if (val !== undefined && val !== null && val !== '') {
        httpParams = httpParams.set(key, String(val));
      }
    });

    return this.http.get<PageResponse<AdminAuditEvent>>(`${this.baseUrl}/api/admin/audit`, {
      params: httpParams
    });
  }

  getActors(): Observable<AuditActorSummary[]> {
    return this.http.get<AuditActorSummary[]>(`${this.baseUrl}/api/admin/audit/actors`);
  }

  exportCsv(params: {
    actorId?: string;
    action?: string;
    from?: string;
    to?: string;
  }): void {
    let httpParams = new HttpParams();

    Object.entries(params).forEach(([key, val]) => {
      if (val) {
        httpParams = httpParams.set(key, val);
      }
    });

    this.http
      .get(`${this.baseUrl}/api/admin/audit/export`, { params: httpParams, responseType: 'text' })
      .subscribe({
        next: (csv) => {
          const blob = new Blob([csv], { type: 'text/csv' });
          const url = URL.createObjectURL(blob);
          const anchor = document.createElement('a');
          anchor.href = url;
          anchor.download = `audit-log-${new Date().toISOString().slice(0, 10)}.csv`;
          anchor.click();
          URL.revokeObjectURL(url);
        }
      });
  }
}
