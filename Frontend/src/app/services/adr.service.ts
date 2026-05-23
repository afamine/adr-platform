import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { catchError, map, Observable, of, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { AdrDto, AdrStatus, AuditEventDto, CastVoteRequest, CommentDto, CreateAdrRequest, HistoryEventDto, PageResponse, StatusTransitionRequest, TeamMemberDto, UpdateAdrRequest, VoteDto } from '../models/adr.model';

@Injectable({ providedIn: 'root' })
export class AdrService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  getAdrs(params?: {
    status?: AdrStatus | 'ALL' | string;
    search?: string;
    page?: number;
    size?: number;
    sort?: string;
  }): Observable<AdrDto[]> {
    let httpParams = new HttpParams();

    if (params?.status && params.status !== 'ALL') {
      httpParams = httpParams.set('status', params.status);
    }

    if (params?.search?.trim()) {
      httpParams = httpParams.set('search', params.search.trim());
    }

    if (params?.page !== undefined) {
      httpParams = httpParams.set('page', params.page.toString());
    }

    if (params?.size !== undefined) {
      httpParams = httpParams.set('size', params.size.toString());
    }

    if (params?.sort) {
      httpParams = httpParams.set('sort', params.sort);
    }

    return this.http
      .get<AdrDto[]>(`${this.baseUrl}/api/adrs`, { params: httpParams })
      .pipe(
        map((adrs) => adrs.map((adr) => this.normalizeTags(adr))),
        catchError((err) => this.handleError(err))
      );
  }

  getAdrsPaged(params?: {
    status?: AdrStatus | 'ALL' | string;
    search?: string;
    page?: number;
    size?: number;
    sort?: string;
    direction?: string;
  }): Observable<PageResponse<AdrDto>> {
    let httpParams = new HttpParams();
    if (params?.status && params.status !== 'ALL') {
      httpParams = httpParams.set('status', params.status);
    }
    if (params?.search?.trim()) {
      httpParams = httpParams.set('search', params.search.trim());
    }
    httpParams = httpParams.set('page', (params?.page ?? 0).toString());
    httpParams = httpParams.set('size', (params?.size ?? 20).toString());
    httpParams = httpParams.set('sort', params?.sort ?? 'adrNumber');
    httpParams = httpParams.set('direction', params?.direction ?? 'DESC');

    return this.http
      .get<PageResponse<AdrDto>>(`${this.baseUrl}/api/adrs/paged`, { params: httpParams })
      .pipe(
        map((page) => ({ ...page, content: page.content.map((adr) => this.normalizeTags(adr)) })),
        catchError((err) => this.handleError(err))
      );
  }

  getAdr(id: string): Observable<AdrDto> {
    return this.http
      .get<AdrDto>(`${this.baseUrl}/api/adrs/${id}`)
      .pipe(
        map((adr) => this.normalizeTags(adr)),
        catchError((err) => this.handleError(err))
      );
  }

  createAdr(body: CreateAdrRequest): Observable<AdrDto> {
    return this.http
      .post<AdrDto>(`${this.baseUrl}/api/adrs`, body)
      .pipe(
        map((adr) => this.normalizeTags(adr)),
        catchError((err) => this.handleError(err))
      );
  }

  updateAdr(id: string, body: UpdateAdrRequest): Observable<AdrDto> {
    return this.http
      .put<AdrDto>(`${this.baseUrl}/api/adrs/${id}`, body)
      .pipe(
        map((adr) => this.normalizeTags(adr)),
        catchError((err) => this.handleError(err))
      );
  }

  transitionStatus(id: string, status: AdrStatus): Observable<AdrDto> {
    const body: StatusTransitionRequest = { status };

    return this.http
      .patch<AdrDto>(`${this.baseUrl}/api/adrs/${id}/status`, body)
      .pipe(
        map((adr) => this.normalizeTags(adr)),
        catchError((err) => this.handleError(err))
      );
  }

  deleteAdr(id: string): Observable<void> {
    return this.http
      .delete<void>(`${this.baseUrl}/api/adrs/${id}`)
      .pipe(catchError((err) => this.handleError(err)));
  }

  castVote(adrId: string, body: CastVoteRequest): Observable<VoteDto> {
    return this.http
      .post<VoteDto>(`${this.baseUrl}/api/adrs/${adrId}/votes`, body)
      .pipe(catchError((err) => this.handleError(err)));
  }

  getVotes(adrId: string): Observable<VoteDto[]> {
    return this.http
      .get<VoteDto[]>(`${this.baseUrl}/api/adrs/${adrId}/votes`)
      .pipe(catchError((err) => this.handleError(err)));
  }

  getMyVote(adrId: string): Observable<VoteDto | null> {
    return this.http
      .get<VoteDto>(`${this.baseUrl}/api/adrs/${adrId}/votes/my-vote`)
      .pipe(
        catchError((err) => {
          if (err?.status === 404) {
            return of(null);
          }

          return this.handleError(err);
        })
      );
  }

  getAuditLog(adrId: string): Observable<AuditEventDto[]> {
    return this.http
      .get<AuditEventDto[]>(`${this.baseUrl}/api/adrs/${adrId}/audit`)
      .pipe(catchError((err) => this.handleError(err)));
  }

  getComments(adrId: string): Observable<CommentDto[]> {
    return this.http
      .get<CommentDto[]>(`${this.baseUrl}/api/adrs/${adrId}/comments`)
      .pipe(catchError((err) => this.handleError(err)));
  }

  addComment(adrId: string, content: string): Observable<CommentDto> {
    return this.http
      .post<CommentDto>(`${this.baseUrl}/api/adrs/${adrId}/comments`, { content })
      .pipe(catchError((err) => this.handleError(err)));
  }

  resolveComment(adrId: string, commentId: string, resolved: boolean): Observable<CommentDto> {
    return this.http
      .patch<CommentDto>(`${this.baseUrl}/api/adrs/${adrId}/comments/${commentId}/resolve`, { resolved })
      .pipe(catchError((err) => this.handleError(err)));
  }

  getAdrHistory(adrId: string): Observable<HistoryEventDto[]> {
    return this.http
      .get<HistoryEventDto[]>(`${this.baseUrl}/api/adrs/${adrId}/history`)
      .pipe(catchError((err) => this.handleError(err)));
  }

  getAdrTeam(adrId: string): Observable<TeamMemberDto[]> {
    return this.http
      .get<TeamMemberDto[]>(`${this.baseUrl}/api/adrs/${adrId}/team`)
      .pipe(catchError((err) => this.handleError(err)));
  }

  private handleError(err: any) {
    const msg = err.error?.message || 'An error occurred';
    return throwError(() => ({ status: err.status, message: msg, errorType: err.error?.errorType }));
  }

  private normalizeTags(adr: AdrDto): AdrDto {
    const rawTags: unknown = (adr as any)?.tags;
    const normalizedTags = Array.isArray(rawTags)
      ? rawTags
          .filter(Boolean)
          .map((t) => (typeof t === 'string' ? t.trim() : String(t).trim()))
          .filter(Boolean)
      : typeof rawTags === 'string' && rawTags.trim()
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

