import { HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, shareReplay, switchMap, throwError } from 'rxjs';

const SIN_RENOVACION = ['/api/auth/login', '/api/auth/logout', '/api/auth/refresh'];

let renovacionEnCurso: Observable<void> | null = null;

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const http = inject(HttpClient);
  const router = inject(Router);

  const conCookies = req.clone({ withCredentials: true });

  if (SIN_RENOVACION.some((ruta) => req.url.includes(ruta))) {
    return next(conCookies);
  }

  return next(conCookies).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
        return throwError(() => error);
      }

      if (!renovacionEnCurso) {
        renovacionEnCurso = http
          .post<void>('/api/auth/refresh', null, { withCredentials: true })
          .pipe(
            catchError((fallo: unknown) => {
              router.navigate(['/login']);
              return throwError(() => fallo);
            }),
            finalize(() => { renovacionEnCurso = null; }),
            shareReplay({ bufferSize: 1, refCount: false }),
          );
      }

      return renovacionEnCurso.pipe(switchMap(() => next(conCookies)));
    }),
  );
};
