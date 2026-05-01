import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { catchError, map, Observable, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { AdrDto, AdrStatus, CreateAdrRequest, StatusTransitionRequest, UpdateAdrRequest } from '../models/adr.model';

@Injectable({ providedIn: 'root' })
export class AdrService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  getAdrs(params?: { status?: AdrStatus; search?: string }): Observable<AdrDto[]> {
    let httpParams = new HttpParams();

    if (params?.status) {
      httpParams = httpParams.set('status', params.status);
    }

    if (params?.search) {
      httpParams = httpParams.set('search', params.search);
    }

    return this.http
      .get<AdrDto[]>(`${this.baseUrl}/api/adrs`, { params: httpParams })
      .pipe(
        map((adrs) => adrs.map((adr) => this.normalizeAdr(adr))),
        catchError((err) => this.handleError(err))
      );
  }

  getAdr(id: string): Observable<AdrDto> {
    return this.http
      .get<AdrDto>(`${this.baseUrl}/api/adrs/${id}`)
      .pipe(
        map((adr) => this.normalizeAdr(adr)),
        catchError((err) => this.handleError(err))
      );
  }

  createAdr(body: CreateAdrRequest): Observable<AdrDto> {
    return this.http
      .post<AdrDto>(`${this.baseUrl}/api/adrs`, body)
      .pipe(catchError((err) => this.handleError(err)));
  }

  updateAdr(id: string, body: UpdateAdrRequest): Observable<AdrDto> {
    return this.http
      .put<AdrDto>(`${this.baseUrl}/api/adrs/${id}`, body)
      .pipe(catchError((err) => this.handleError(err)));
  }

  transitionStatus(id: string, status: AdrStatus): Observable<AdrDto> {
    const body: StatusTransitionRequest = { status };

    return this.http
      .patch<AdrDto>(`${this.baseUrl}/api/adrs/${id}/status`, body)
      .pipe(catchError((err) => this.handleError(err)));
  }

  deleteAdr(id: string): Observable<void> {
    return this.http
      .delete<void>(`${this.baseUrl}/api/adrs/${id}`)
      .pipe(catchError((err) => this.handleError(err)));
  }

  private handleError(err: any) {
    const msg = err.error?.message || 'An error occurred';
    return throwError(() => ({ status: err.status, message: msg, errorType: err.error?.errorType }));
  }

  private normalizeAdr(adr: AdrDto): AdrDto {
    const rawTags: unknown = (adr as any)?.tags;
    const normalizedTags = Array.isArray(rawTags)
      ? rawTags
      : typeof rawTags === 'string'
        ? rawTags
            .split(',')
            .map((t) => t.trim())
            .filter(Boolean)
        : [];

    return {
      ...adr,
      tags: normalizedTags
    };
  }
}

