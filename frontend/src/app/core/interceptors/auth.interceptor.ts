import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject, Injector } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('accessToken');
  if (token && req.url.startsWith('/api') && !req.url.startsWith('/api/auth')) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }

  const injector = inject(Injector);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !req.url.startsWith('/api/auth')) {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        const router = injector.get(Router);
        router.navigate(['/login']);
      }
      return throwError(() => error);
    }),
  );
};
