import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

/**
 * Interceptor JWT: adjunta el accessToken en memoria
 * a cada solicitud HTTP saliente.
 * El accessToken NO se almacena en localStorage (vulnerable a XSS).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getAccessToken();

  if (token) {
    const cloned = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    });
    return next(cloned);
  }

  return next(req);
};
