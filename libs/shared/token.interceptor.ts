import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthSession } from './auth-session';

export const tokenInterceptor: HttpInterceptorFn = (req, next) => {
  const session = inject(AuthSession);
  const router = inject(Router);
  const token = session.token() ?? readStoredToken();
  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 0) {
        session.apiDown.set(true);
      }
      if (err.status === 401) {
        session.clear();
        void router.navigateByUrl('/login');
      }
      return throwError(() => err);
    })
  );
};

function readStoredToken(): string | null {
  const raw = localStorage.getItem('harborpath.session');
  if (!raw) {
    return null;
  }
  try {
    return (JSON.parse(raw) as { accessToken?: string }).accessToken ?? null;
  } catch {
    return null;
  }
}
