import { HttpErrorResponse } from '@angular/common/http';

export type OrigenFallo = 'dispositivo' | 'camino' | 'servidor' | 'peticion';

export interface Diagnostico {
  origen: OrigenFallo;
  mensaje: string;
  sugerencia?: string;
}

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
      return {
        origen: 'peticion',
        mensaje: leerDetalle(err) ?? 'Revisa los datos del formulario',
      };

    case 429:
      return {
        origen: 'peticion',
        mensaje: leerDetalle(err) ?? 'Demasiados intentos seguidos',
        sugerencia: 'Espera unos minutos antes de volver a intentarlo.',
      };

    case 404:
      return {
        origen: 'servidor',
        mensaje: 'El servidor no reconoce esta operación',
        sugerencia: 'Puede que la aplicación y el servidor estén en versiones distintas: recarga la página.',
      };

    case 502:
    case 503:
    case 504:
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

function leerDetalle(err: HttpErrorResponse): string | null {
  const cuerpo = err.error as { detail?: unknown } | null;
  const detalle = cuerpo?.detail;
  return typeof detalle === 'string' && detalle.trim() ? detalle : null;
}
