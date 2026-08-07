import { Routes } from '@angular/router';
import { authGuard } from './auth/auth.guard';
import { roleGuard } from './auth/role.guard';
import { AppShellComponent } from './shell/app-shell.component';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./auth/login/login.component').then(m => m.LoginComponent) },
  {
    path: '', component: AppShellComponent, canActivate: [authGuard], children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent) },
      {
        path: 'entrenador/sesiones',
        canActivate: [roleGuard(['ENTRENADOR'])],
        loadComponent: () => import('./features/entrenador/sesiones.component').then(m => m.SesionesComponent),
      },
      {
        path: 'entrenador/sesion/:idSesion',
        loadComponent: () => import('./features/entrenador/evaluacion-diaria.component')
          .then(m => m.EvaluacionDiariaComponent),
      },
      {
        path: 'entrenador/sesion/:idSesion/plantilla',
        loadComponent: () => import('./features/entrenador/plantilla.component')
          .then(m => m.PlantillaComponent),
      },
      {
        path: 'admin/crear-usuario',
        canActivate: [roleGuard(['ADMINISTRADOR'])],
        loadComponent: () => import('./features/admin/crear-usuario.component').then(m => m.CrearUsuarioComponent),
      },
      {
        path: 'recepcion',
        canActivate: [roleGuard(['ADMINISTRADOR', 'RECEPCIONISTA'])],
        loadComponent: () => import('./features/recepcion/recepcion.component').then(m => m.RecepcionComponent),
      },
      {
        path: 'representante',
        canActivate: [roleGuard(['REPRESENTANTE'])],
        loadComponent: () => import('./features/representante/representante.component').then(m => m.RepresentanteComponent),
      },
      {
        path: 'estudiante/marcar-asistencia',
        canActivate: [roleGuard(['ESTUDIANTE'])],
        loadComponent: () => import('./features/estudiante/marcar-asistencia.component').then(m => m.MarcarAsistenciaComponent),
      },
    ]
  },
  { path: '**', redirectTo: '' }
];
