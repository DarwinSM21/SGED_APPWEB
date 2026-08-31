export function homeRouteForRole(rol: string | undefined | null): string {
  switch (rol) {
    case 'RECEPCIONISTA': return '/recepcion';
    case 'REPRESENTANTE': return '/representante';
    case 'ESTUDIANTE': return '/estudiante/marcar-asistencia';
    default: return '/dashboard';
  }
}
