import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ProjectDto, ProjectRequest } from '../models/project.model';

@Injectable({ providedIn: 'root' })
export class ProjectService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/api/projects`;

  getProjects(): Observable<ProjectDto[]> { return this.http.get<ProjectDto[]>(this.url); }
  createProject(request: ProjectRequest): Observable<ProjectDto> { return this.http.post<ProjectDto>(this.url, request); }
  updateProject(id: string, request: ProjectRequest): Observable<ProjectDto> { return this.http.put<ProjectDto>(`${this.url}/${id}`, request); }
  archiveProject(id: string): Observable<ProjectDto> { return this.http.patch<ProjectDto>(`${this.url}/${id}/archive`, {}); }
}
