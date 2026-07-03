import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  AuthResponse,
  MessageResponse,
  UpdateWorkspaceRequest,
  WorkspaceInfo,
  WorkspaceMembership,
  WorkspaceSlugStatus
} from '../models/auth.models';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class WorkspaceService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = environment.apiUrl;

  getWorkspace(): Observable<WorkspaceInfo> {
    return this.http.get<WorkspaceInfo>(`${this.API_URL}/api/workspace`);
  }

  checkSlug(slug: string): Observable<WorkspaceSlugStatus> {
    return this.http.get<WorkspaceSlugStatus>(`${this.API_URL}/api/workspace/slug-status`, {
      params: { slug }
    });
  }

  updateWorkspace(request: UpdateWorkspaceRequest): Observable<WorkspaceInfo> {
    return this.http.put<WorkspaceInfo>(`${this.API_URL}/api/workspace`, request);
  }

  resetWorkspace(): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.API_URL}/api/workspace/reset`, {});
  }

  getMyWorkspaces(): Observable<WorkspaceMembership[]> {
    return this.http.get<WorkspaceMembership[]>(`${this.API_URL}/api/workspace/memberships`);
  }

  switchWorkspace(workspaceId: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/api/workspace/switch/${workspaceId}`, {});
  }
}
