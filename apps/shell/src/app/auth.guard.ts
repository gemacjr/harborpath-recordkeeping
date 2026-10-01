import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthSession, Role } from '@harborpath/shared';

export const authGuard: CanActivateFn = () => {
  const session = inject(AuthSession);
  const router = inject(Router);
  return session.token() ? true : router.createUrlTree(['/login']);
};

export const roleGuard = (role: Role): CanActivateFn => () => {
  const session = inject(AuthSession);
  const router = inject(Router);
  if (!session.token()) {
    return router.createUrlTree(['/login']);
  }
  return session.role() === role ? true : router.createUrlTree(['/forbidden']);
};
