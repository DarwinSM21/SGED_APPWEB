import { diagnosticar } from './diagnostico-error';

/**
 * Traduce una respuesta de error del backend al texto que ve el usuario.
 *
 * <p>El GlobalExceptionHandler responde en formato RFC 7807 (Problem Details).
 * Para errores de validacion agrega ademas una propiedad `errores` con el
 * detalle campo por campo ("cedula: must match ..."), mientras que `detail`
 * queda en un generico "Errores de validacion". Leer solo `detail` -que es
 * lo que hacia cada pantalla por su cuenta- descartaba justamente la parte
 * util: el usuario veia "Errores de validacion" sin saber que campo corregir.
 *
 * <p>Para todo lo demas delega en {@link diagnosticar}, que clasifica el fallo
 * por su ORIGEN en vez de por su codigo. La distincion importa porque cada
 * origen se arregla en un sitio distinto: si el dispositivo no tiene red, si
 * respondio un intermediario en vez del backend, si el servidor esta caido o
 * si lo que se envio estaba mal. Un "Error del servidor" generico manda a
 * buscar el problema donde no esta.
 *
 * <p>El orden no es casual: los errores campo por campo van primero porque son
 * los mas especificos que puede recibir el usuario, y ninguna clasificacion
 * general los mejora.
 */
export function mensajeDeError(err: unknown, porDefecto?: string): string {
  const cuerpo = (err as { error?: { errores?: unknown; detail?: unknown } } | null)?.error;

  const errores = cuerpo?.errores;
  if (Array.isArray(errores) && errores.length > 0) {
    return errores.join(' · ');
  }

  const d = diagnosticar(err);

  // El texto que la pantalla pasa como respaldo describe la operacion concreta
  // ("No se pudo guardar el horario"), asi que gana cuando el fallo viene del
  // propio backend: es mas util que un generico. Cuando el fallo es de red o
  // de servidor caido, manda el diagnostico, porque ahi la operacion es lo de
  // menos y lo que hace falta saber es que no se llego a intentar.
  if (porDefecto && d.origen === 'peticion' && !tieneDetalle(cuerpo)) {
    return porDefecto;
  }

  return d.sugerencia ? `${d.mensaje}. ${d.sugerencia}` : d.mensaje;
}

function tieneDetalle(cuerpo: { detail?: unknown } | null | undefined): boolean {
  return typeof cuerpo?.detail === 'string' && cuerpo.detail.trim().length > 0;
}
