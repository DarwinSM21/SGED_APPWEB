import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Restringe una ruta a roles concretos. Corre siempre anidado bajo
 * authGuard (que ya confirmo la sesion contra /api/auth/me si hacia
 * falta), asi que leer currentUser() aqui de forma sincrona es seguro:
 * ya esta poblado para cuando este guard se evalua.
 */
export function roleGuard(rolesPermitidos: string[]): CanActivateFn {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    const rol = authService.currentUser()?.rol;
    if (rol && rolesPermitidos.includes(rol)) {
      return true;
    }
    return router.createUrlTree(['/dashboard']);
  };
}
