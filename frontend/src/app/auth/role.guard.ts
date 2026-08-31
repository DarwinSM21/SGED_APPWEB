import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

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
