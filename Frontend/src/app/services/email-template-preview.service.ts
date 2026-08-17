import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class EmailTemplatePreviewService {
  private readonly http = inject(HttpClient);

  getPreview(templateName: string): Observable<string> {
    return this.http.get(`${environment.apiUrl}/api/admin/email-templates/${templateName}/preview`, {
      responseType: 'text'
    });
  }
}
