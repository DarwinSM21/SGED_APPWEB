import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from './auth.service';

/**
 * Guard de autenticacion: protege rutas que requieren sesion.
 * El JWT vive en una cookie HttpOnly que JavaScript no puede leer, asi
 * que si no hay usuario en memoria (p.ej. tras recargar la pagina) se
 * confirma la sesion contra /api/auth/me antes de decidir.
 */
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()) {
    return true;
  }

  return authService.getProfile().pipe(
    map(() => true),
    catchError(() => of(router.createUrlTree(['/login']))),
  );
};
