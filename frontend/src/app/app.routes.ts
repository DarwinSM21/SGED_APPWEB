import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth-guard';
import { roleGuard } from './core/guards/role-guard';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () => import('./auth/pages/login/login').then((m) => m.Login),
  },
  {
    path: 'admin',
    loadComponent: () =>
      import('./features/dashboard/pages/dashboard/dashboard').then((m) => m.Dashboard),
    canActivate: [roleGuard],
    data: { roles: ['ADMINISTRADOR', 'RECEPCIONISTA'] },
  },
  {
    path: 'entrenador',
    loadComponent: () =>
      import('./features/dashboard/pages/dashboard/dashboard').then((m) => m.Dashboard),
    canActivate: [roleGuard],
    data: { roles: ['ENTRENADOR'] },
  },
  {
    path: 'estudiante',
    loadComponent: () =>
      import('./features/dashboard/pages/dashboard/dashboard').then((m) => m.Dashboard),
    canActivate: [roleGuard],
    data: { roles: ['ESTUDIANTE'] },
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./features/dashboard/pages/dashboard/dashboard').then((m) => m.Dashboard),
    canActivate: [authGuard],
  },
  { path: '**', redirectTo: 'login' },
];