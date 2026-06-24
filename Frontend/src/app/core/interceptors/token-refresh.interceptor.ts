import { HttpErrorResponse, HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, finalize, shareReplay, switchMap, throwError } from 'rxjs';

import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';

export const tokenRefreshInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const notificationService = inject(NotificationService);

  const handleRefreshFailure = (refreshError: unknown) => {
    authService.logout();
    router.navigate(['/login']);
    return throwError(() => refreshError);
  };

  const retryRequest = () => {
    const token = authService.getToken();
    const retryReq = token
      ? req.clone({
          setHeaders: { Authorization: `Bearer ${token}` }
        })
      : req;
    return next(retryReq);
  };

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !req.url.includes('/api/auth/')) {
        if (authService.isRefreshing$.value && authService.refreshToken$) {
          return authService.refreshToken$.pipe(
            switchMap(() => retryRequest()),
            catchError((refreshError) => handleRefreshFailure(refreshError))
          );
        }

        authService.isRefreshing$.next(true);
        authService.refreshToken$ = authService.refreshTokenOnce().pipe(
          shareReplay(1),
          finalize(() => {
            authService.isRefreshing$.next(false);
            authService.refreshToken$ = null;
          })
        );

        return authService.refreshToken$.pipe(
          switchMap(() => retryRequest()),
          catchError((refreshError) => handleRefreshFailure(refreshError))
        );
      }

      if (error.status === 403) {
        notificationService.error(
          'Access denied',
          'You do not have permission to perform this action.'
        );
      }

      return throwError(() => error);
    })
  );
};
