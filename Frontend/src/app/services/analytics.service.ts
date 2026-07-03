import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { AnalyticsTimeRange, KpiResponse, RecentAdrDto, StatusCount, WeeklyActivity } from '../models/analytics.models';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = environment.apiUrl;

  getKpis(timeRange: AnalyticsTimeRange = '30d'): Observable<KpiResponse> {
    return this.http.get<KpiResponse>(`${this.API_URL}/api/analytics/kpis`, {
      params: { timeRange }
    });
  }

  getStatusDistribution(timeRange: AnalyticsTimeRange = '30d'): Observable<StatusCount[]> {
    return this.http.get<StatusCount[]>(`${this.API_URL}/api/analytics/status-distribution`, {
      params: { timeRange }
    });
  }

  getWeeklyActivity(timeRange: AnalyticsTimeRange = '30d'): Observable<WeeklyActivity[]> {
    return this.http.get<WeeklyActivity[]>(`${this.API_URL}/api/analytics/weekly-activity`, {
      params: { timeRange }
    });
  }

  getRecentAdrs(limit = 4, timeRange: AnalyticsTimeRange = '30d'): Observable<RecentAdrDto[]> {
    return this.http.get<RecentAdrDto[]>(`${this.API_URL}/api/adrs/recent`, {
      params: { limit, timeRange }
    });
  }
}
