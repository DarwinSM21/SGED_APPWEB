/**
 * Traduce una respuesta de error del backend al texto que ve el usuario.
 *
 * El GlobalExceptionHandler responde en formato RFC 7807 (Problem Details).
 * Para errores de validacion agrega ademas una propiedad `errores` con el
 * detalle campo por campo ("cedula: must match ..."), mientras que `detail`
 * queda en un generico "Errores de validacion". Leer solo `detail` -que es
 * lo que hacia cada pantalla por su cuenta- descartaba justamente la parte
 * util: el usuario veia "Errores de validacion" sin saber que campo corregir.
 */
export function mensajeDeError(err: unknown, porDefecto = 'Error del servidor'): string {
  const cuerpo = (err as { error?: { errores?: unknown; detail?: unknown } } | null)?.error;

  const errores = cuerpo?.errores;
  if (Array.isArray(errores) && errores.length > 0) {
    return errores.join(' · ');
  }

  return typeof cuerpo?.detail === 'string' && cuerpo.detail ? cuerpo.detail : porDefecto;
}
