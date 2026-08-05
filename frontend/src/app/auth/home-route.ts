/**
 * A donde va cada rol despues de autenticarse. Un solo lugar para esta
 * decision: la usan tanto LoginComponent (redirect sin parpadeo tras un
 * login exitoso) como DashboardComponent (red de seguridad para refresh,
 * boton atras, o un marcador guardado en /dashboard) para que no se
 * desincronicen entre si.
 */
export function homeRouteForRole(rol: string | undefined | null): string {
  switch (rol) {
    case 'RECEPCIONISTA': return '/recepcion';
    case 'REPRESENTANTE': return '/representante';
    case 'ESTUDIANTE': return '/estudiante/marcar-asistencia';
    default: return '/dashboard';
  }
}
