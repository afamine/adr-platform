import { HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';

import { AuthService } from '../../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
) => {
  const token = inject(AuthService).getToken();

  const options = token
    ? { withCredentials: true, setHeaders: { Authorization: `Bearer ${token}` } }
    : { withCredentials: true };
  return next(req.clone(options));
};
