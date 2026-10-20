import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { BehaviorSubject, catchError, filter, switchMap, take, throwError } from 'rxjs';
import { AuthService } from '../auth/auth.service';

let isRefreshing = false;
const refreshTokenSubject = new BehaviorSubject<string | null>(null);

export function resetErrorInterceptorState(): void {
  isRefreshing = false;
  refreshTokenSubject.next(null);
}

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  return next(req).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        const isAuthEndpoint =
          req.url.includes('/api/auth/login') || req.url.includes('/api/auth/refresh');

        if (isAuthEndpoint) {
          authService.logout();
          return throwError(() => error);
        }

        const refreshToken = authService.getRefreshToken();
        if (!refreshToken) {
          authService.logout();
          return throwError(() => error);
        }

        if (!isRefreshing) {
          isRefreshing = true;
          refreshTokenSubject.next(null);

          return authService.refreshToken().pipe(
            switchMap((authResponse) => {
              isRefreshing = false;
              refreshTokenSubject.next(authResponse.accessToken);

              const clonedReq = req.clone({
                setHeaders: {
                  Authorization: `Bearer ${authResponse.accessToken}`,
                },
              });
              return next(clonedReq);
            }),
            catchError((refreshErr) => {
              isRefreshing = false;
              refreshTokenSubject.next(null);
              authService.logout();
              return throwError(() => refreshErr);
            }),
          );
        } else {
          return refreshTokenSubject.pipe(
            filter((token): token is string => token !== null),
            take(1),
            switchMap((token) => {
              const clonedReq = req.clone({
                setHeaders: {
                  Authorization: `Bearer ${token}`,
                },
              });
              return next(clonedReq);
            }),
          );
        }
      }

      return throwError(() => error);
    }),
  );
};

