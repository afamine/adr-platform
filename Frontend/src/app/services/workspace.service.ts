import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { MessageResponse, UpdateWorkspaceRequest, WorkspaceInfo } from '../models/auth.models';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class WorkspaceService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = environment.apiUrl;

  getWorkspace(): Observable<WorkspaceInfo> {
    return this.http.get<WorkspaceInfo>(`${this.API_URL}/api/workspace`);
  }

  updateWorkspace(request: UpdateWorkspaceRequest): Observable<WorkspaceInfo> {
    return this.http.put<WorkspaceInfo>(`${this.API_URL}/api/workspace`, request);
  }

  resetWorkspace(): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.API_URL}/api/workspace/reset`, {});
  }
}
