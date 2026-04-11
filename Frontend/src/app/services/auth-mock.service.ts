import { Injectable } from '@angular/core';
import { Observable, of, throwError, timer } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
  workspaceSlug?: string;
}

export interface RegisterResponse {
  message: string;
}

@Injectable({ providedIn: 'root' })
export class AuthMockService {
  register(request: RegisterRequest): Observable<RegisterResponse> {
    const shouldFail = Math.random() < 0.1;
    const cleanedSlug = request.workspaceSlug?.trim() || undefined;

    return timer(1000).pipe(
      mergeMap(() => {
        if (shouldFail) {
          return throwError(() => new Error('Unable to create account right now. Please try again.'));
        }

        return of({
          message: cleanedSlug
            ? `Account created for workspace ${cleanedSlug}.`
            : 'Account created successfully.'
        });
      })
    );
  }
}
