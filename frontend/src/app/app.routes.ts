import { Routes } from '@angular/router';

import { LoginComponent } from './auth/pages/login/login';
import { DashboardComponent } from './features/dashboard/pages/dashboard/dashboard';

export const routes: Routes = [

  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  },

  {
    path: 'login',
    component: LoginComponent
  },

  {
    path: 'dashboard',
    component: DashboardComponent
  },

  {
    path: '**',
    redirectTo: 'login'
  }

];