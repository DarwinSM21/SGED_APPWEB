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
        path: 'recepcion',
        canActivate: [roleGuard(['ADMINISTRADOR', 'RECEPCIONISTA'])],
        loadComponent: () => import('./features/recepcion/recepcion.component').then(m => m.RecepcionComponent),
      },
      {
        path: 'personas',
        canActivate: [roleGuard(['ADMINISTRADOR', 'RECEPCIONISTA'])],
        loadComponent: () => import('./features/personas/personas.component').then(m => m.PersonasComponent),
      },
      {
        path: 'pagos',
        canActivate: [roleGuard(['ADMINISTRADOR', 'RECEPCIONISTA'])],
        loadComponent: () => import('./features/pagos/pagos.component').then(m => m.PagosComponent),
      },
      {
        path: 'inventario',
        canActivate: [roleGuard(['ADMINISTRADOR', 'RECEPCIONISTA', 'ENTRENADOR'])],
        loadComponent: () => import('./features/inventario/inventario.component').then(m => m.InventarioComponent),
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
      {
        path: 'estudiante/mi-historial',
        canActivate: [roleGuard(['ESTUDIANTE'])],
        loadComponent: () => import('./features/estudiante/mi-historial.component').then(m => m.MiHistorialComponent),
      },
    ]
  },
  { path: '**', redirectTo: '' }
];
