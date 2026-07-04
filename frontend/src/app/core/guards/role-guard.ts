import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Auth } from '../../auth/services/auth';

export const roleGuard: CanActivateFn = (route, state) => {
  const auth = inject(Auth);
  const router = inject(Router);

  if (!auth.isAuthenticated()) {
    router.navigate(['/login']);
    return false;
  }

  const rolesPermitidos = route.data?.['roles'] as string[] | undefined;
  const rolActual = auth.getRol();

  if (!rolesPermitidos || rolesPermitidos.length === 0) {
    return true;
  }

  if (rolActual && rolesPermitidos.includes(rolActual)) {
    return true;
  }

  router.navigate(['/login']);
  return false;
};