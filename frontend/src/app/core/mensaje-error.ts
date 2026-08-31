import { diagnosticar } from './diagnostico-error';

export function mensajeDeError(err: unknown, porDefecto?: string): string {
  const cuerpo = (err as { error?: { errores?: unknown; detail?: unknown } } | null)?.error;

  const errores = cuerpo?.errores;
  if (Array.isArray(errores) && errores.length > 0) {
    return errores.join(' · ');
  }

  const d = diagnosticar(err);

  if (porDefecto && d.origen === 'peticion' && !tieneDetalle(cuerpo)) {
    return porDefecto;
  }

  return d.sugerencia ? `${d.mensaje}. ${d.sugerencia}` : d.mensaje;
}

function tieneDetalle(cuerpo: { detail?: unknown } | null | undefined): boolean {
  return typeof cuerpo?.detail === 'string' && cuerpo.detail.trim().length > 0;
}
