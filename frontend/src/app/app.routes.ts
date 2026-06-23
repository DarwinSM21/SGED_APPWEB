import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth-guard';

export const routes: Routes = [

  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  },

  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/pages/login/login')
        .then(m => m.Login)
  },

  {
    path: '',
    canActivate: [authGuard],

    loadComponent: () =>
      import('./layout/dashboard-layout/dashboard-layout')
        .then(m => m.DashboardLayout),

    children: [

      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/pages/dashboard')
            .then(m => m.Dashboard)
      }

    ]
  },

  {
    path: '**',
    redirectTo: 'login'
  }

];