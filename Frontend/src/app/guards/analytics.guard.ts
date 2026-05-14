import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { Role } from '../models/auth.models';
import { AuthService } from '../services/auth.service';

export const analyticsGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }

  const role = auth.getCurrentUser()?.role;
  if (role === Role.ADMIN || role === Role.APPROVER) {
    return true;
  }

  return router.createUrlTree(['/unauthorized']);
};
