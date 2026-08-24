import { HttpErrorResponse } from '@angular/common/http';

/**
 * De qué tipo es un fallo, para poder decírselo a quien lo sufre.
 *
 * <p>La distinción que importa no es el número del código sino <b>dónde</b> se
 * rompió: si el problema está en el dispositivo, en el camino, o en el
 * servidor. Cada uno se arregla en un sitio distinto, y un mensaje genérico
 * manda a buscar donde no está.
 */
export type OrigenFallo = 'dispositivo' | 'camino' | 'servidor' | 'peticion';

export interface Diagnostico {
  origen: OrigenFallo;
  /** Lo que se le muestra a quien está usando el sistema. */
  mensaje: string;
  /** Qué puede hacer al respecto, si hay algo. */
  sugerencia?: string;
}

/**
 * La pista más fiable de que la petición nunca llegó al backend.
 *
 * <p>Este proyecto responde SIEMPRE en JSON con formato RFC 7807, incluidos
 * los errores. Si lo que vuelve es HTML, quien contestó fue algo intermedio
 * —una página de error de Cloudflare, un portal cautivo de wifi, un proxy—
 * y no la aplicación. Ocurrió de verdad: con el túnel caído, el sistema
 * mostraba el texto en inglés de Cloudflare como si fuera un error propio.
 */
function respondioAlgoQueNoEsElBackend(err: HttpErrorResponse): boolean {
  const cuerpo = err.error;
  if (typeof cuerpo === 'string') {
    return /<html|<!doctype|cloudflare|<body/i.test(cuerpo);
  }
  return false;
}

export function diagnosticar(err: unknown): Diagnostico {
  if (!(err instanceof HttpErrorResponse)) {
    return {
      origen: 'servidor',
      mensaje: 'Ocurrió un error inesperado',
      sugerencia: 'Vuelve a intentarlo; si persiste, avisa a quien administra el sistema.',
    };
  }

  // El dispositivo sabe que no tiene red: es lo primero que hay que descartar
  // porque no depende del servidor en absoluto.
  if (typeof navigator !== 'undefined' && navigator.onLine === false) {
    return {
      origen: 'dispositivo',
      mensaje: 'Este dispositivo no tiene conexión',
      sugerencia: 'Revisa el wifi o los datos móviles y vuelve a intentarlo.',
    };
  }

  if (respondioAlgoQueNoEsElBackend(err)) {
    return {
      origen: 'camino',
      mensaje: 'No se llegó hasta el servidor',
      sugerencia: 'Respondió un intermediario de la red, no el sistema. Comprueba la dirección o la conexión de la red.',
    };
  }

  switch (err.status) {
    case 0:
      // Angular usa 0 para todo lo que ni siquiera obtuvo respuesta: servidor
      // apagado, dirección inalcanzable, CORS o certificado rechazado.
      return {
        origen: 'camino',
        mensaje: 'No se pudo contactar al servidor',
        sugerencia: 'Comprueba que el sistema esté encendido y que la dirección sea la correcta.',
      };

    case 401:
      return { origen: 'peticion', mensaje: 'Usuario o contraseña incorrectos' };

    case 403:
      return {
        origen: 'peticion',
        mensaje: 'Esta cuenta no tiene permiso para entrar aquí',
      };

    case 400:
    case 422:
      // Del formulario, no del servidor: decir lo contrario manda a buscar el
      // problema donde no está.
      return { origen: 'peticion', mensaje: 'Revisa el usuario y la contraseña' };

    case 429:
      // El backend ya redacta el mensaje con el tiempo de espera concreto.
      return {
        origen: 'peticion',
        mensaje: leerDetalle(err) ?? 'Demasiados intentos seguidos',
        sugerencia: 'Espera unos minutos antes de volver a intentarlo.',
      };

    case 404:
      // Con la app servida y la API ausente, casi siempre es un despliegue a
      // medias, no un error de quien la usa.
      return {
        origen: 'servidor',
        mensaje: 'El servidor no reconoce esta operación',
        sugerencia: 'Puede que la aplicación y el servidor estén en versiones distintas: recarga la página.',
      };

    case 502:
    case 503:
    case 504:
      // El servidor web contesta pero la aplicación detrás no: es distinto de
      // "no hay conexión", y se arregla en otro sitio.
      return {
        origen: 'servidor',
        mensaje: 'El servidor está encendido pero la aplicación no responde',
        sugerencia: 'Suele durar poco. Espera unos segundos y vuelve a intentarlo.',
      };

    default:
      if (err.status >= 500) {
        return {
          origen: 'servidor',
          mensaje: leerDetalle(err) ?? 'Error interno del servidor',
          sugerencia: 'No es problema tuyo. Si sigue ocurriendo, avisa a quien administra el sistema.',
        };
      }
      return { origen: 'peticion', mensaje: leerDetalle(err) ?? 'No se pudo completar la operación' };
  }
}

/** El `detail` de RFC 7807, si el backend llegó a redactarlo. */
function leerDetalle(err: HttpErrorResponse): string | null {
  const cuerpo = err.error as { detail?: unknown } | null;
  const detalle = cuerpo?.detail;
  return typeof detalle === 'string' && detalle.trim() ? detalle : null;
}
