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
        canActivate: [roleGuard(['ADMINISTRADOR', 'ENTRENADOR'])],
        loadComponent: () => import('./features/entrenador/sesiones.component').then(m => m.SesionesComponent),
      },
      {
        path: 'entrenador/sesion/:idSesion',
        loadComponent: () => import('./features/entrenador/evaluacion-diaria.component')
          .then(m => m.EvaluacionDiariaComponent),
      },
      {
        path: 'entrenador/sesion/:idSesion/asistencia',
        canActivate: [roleGuard(['ADMINISTRADOR', 'ENTRENADOR'])],
        loadComponent: () => import('./features/entrenador/lista-asistencia.component')
          .then(m => m.ListaAsistenciaComponent),
      },
      {
        path: 'entrenador/sesion/:idSesion/historial',
        canActivate: [roleGuard(['ADMINISTRADOR', 'ENTRENADOR'])],
        loadComponent: () => import('./features/entrenador/historial-sesion.component')
          .then(m => m.HistorialSesionComponent),
      },
      // La formacion salio de las sesiones: es la decision de un partido, no
      // un hecho del entrenamiento.
      {
        path: 'partidos',
        canActivate: [roleGuard(['ADMINISTRADOR', 'ENTRENADOR'])],
        loadComponent: () => import('./features/partidos/partidos.component')
          .then(m => m.PartidosComponent),
      },
      {
        path: 'partidos/:idPartido/alineacion',
        canActivate: [roleGuard(['ADMINISTRADOR', 'ENTRENADOR'])],
        loadComponent: () => import('./features/partidos/alineacion.component')
          .then(m => m.AlineacionComponent),
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
        path: 'categorias',
        // El entrenador entra en modo consulta: el propio componente le oculta
        // el formulario y los botones, y el backend ya le niega POST/PUT/DELETE
        // con 403 aunque llegara por otra via.
        canActivate: [roleGuard(['ADMINISTRADOR', 'ENTRENADOR'])],
        loadComponent: () => import('./features/categorias/categorias.component').then(m => m.CategoriasComponent),
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
      {
        path: 'estudiante/mi-equipo',
        canActivate: [roleGuard(['ESTUDIANTE'])],
        loadComponent: () => import('./features/estudiante/mi-equipo.component').then(m => m.MiEquipoComponent),
      },
      {
        path: 'admin/consentimientos',
        canActivate: [roleGuard(['ADMINISTRADOR'])],
        loadComponent: () => import('./features/consentimientos/consentimientos.component').then(m => m.ConsentimientosComponent),
      },
      {
        path: 'admin/auditorias',
        canActivate: [roleGuard(['ADMINISTRADOR'])],
        loadComponent: () => import('./features/auditorias/auditorias.component').then(m => m.AuditoriasComponent),
      },
      {
        path: 'reportes',
        canActivate: [roleGuard(['ADMINISTRADOR', 'RECEPCIONISTA', 'ENTRENADOR'])],
        loadComponent: () => import('./features/reportes/reportes.component').then(m => m.ReportesComponent),
      },
      {
        path: 'configuracion',
        loadComponent: () => import('./features/configuracion/configuracion.component').then(m => m.ConfiguracionComponent),
      },
    ]
  },
  { path: '**', redirectTo: '' }
];
