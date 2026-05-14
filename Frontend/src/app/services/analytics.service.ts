import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { KpiResponse, RecentAdrDto, StatusCount, WeeklyActivity } from '../models/analytics.models';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = environment.apiUrl;

  getKpis(): Observable<KpiResponse> {
    return this.http.get<KpiResponse>(`${this.API_URL}/api/analytics/kpis`);
  }

  getStatusDistribution(): Observable<StatusCount[]> {
    return this.http.get<StatusCount[]>(`${this.API_URL}/api/analytics/status-distribution`);
  }

  getWeeklyActivity(weeks = 5): Observable<WeeklyActivity[]> {
    return this.http.get<WeeklyActivity[]>(`${this.API_URL}/api/analytics/weekly-activity?weeks=${weeks}`);
  }

  getRecentAdrs(limit = 4): Observable<RecentAdrDto[]> {
    return this.http.get<RecentAdrDto[]>(`${this.API_URL}/api/adrs/recent?limit=${limit}`);
  }
}
