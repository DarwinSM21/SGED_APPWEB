import { HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, shareReplay, switchMap, throwError } from 'rxjs';

/** Rutas de sesion que NO deben disparar una renovacion. */
const SIN_RENOVACION = ['/api/auth/login', '/api/auth/logout', '/api/auth/refresh'];

/**
 * Una sola renovacion en vuelo, compartida por todas las peticiones que
 * fallen a la vez. El tablero dispara seis llamadas juntas: sin compartirla,
 * al vencer el token saldrian seis refresh en paralelo y cinco trabajarian
 * sobre una cookie que el primero ya rotó.
 *
 * Va con shareReplay porque un observable de HttpClient es frio: cada
 * suscriptor dispara su propia peticion, que es exactamente lo que se quiere
 * evitar aqui.
 */
let renovacionEnCurso: Observable<void> | null = null;

/**
 * Adjunta las cookies de sesion (sged_access, sged_refresh) a cada solicitud
 * hacia la API, y renueva el acceso cuando vence.
 *
 * El JWT viaja en cookies HttpOnly (Bloque A.1), asi que JavaScript no lo lee
 * ni lo adjunta a mano: solo hay que pedirle al navegador que las envie con
 * withCredentials.
 *
 * Sobre la renovacion: el access token dura una hora y el refresh siete dias,
 * pero hasta ahora nadie llamaba a /api/auth/refresh. El efecto era que a la
 * hora exacta la sesion se caia sola -cada pantalla mostrando su propio
 * error- aunque el servidor todavia tenia con que renovarla. Ahora un 401
 * dispara un unico intento de refresh y se reintenta la peticion original;
 * si el refresh tambien falla, recien ahi se va al login.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // inject() solo vale durante la ejecucion sincrona del interceptor: si se
  // llamara dentro del catchError -que corre despues- lanzaria NG0203.
  const http = inject(HttpClient);
  const router = inject(Router);

  const conCookies = req.clone({ withCredentials: true });

  if (SIN_RENOVACION.some((ruta) => req.url.includes(ruta))) {
    return next(conCookies);
  }

  return next(conCookies).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
        return throwError(() => error);
      }

      if (!renovacionEnCurso) {
        renovacionEnCurso = http
          .post<void>('/api/auth/refresh', null, { withCredentials: true })
          .pipe(
            catchError((fallo: unknown) => {
              // El refresh tambien vencio o fue revocado: la sesion ya no se
              // puede salvar, asi que al login.
              router.navigate(['/login']);
              return throwError(() => fallo);
            }),
            // Se libera pase lo que pase; si quedara colgada, un 401 futuro
            // reutilizaria un observable ya terminado y nunca reintentaria.
            finalize(() => { renovacionEnCurso = null; }),
            shareReplay({ bufferSize: 1, refCount: false }),
          );
      }

      return renovacionEnCurso.pipe(switchMap(() => next(conCookies)));
    }),
  );
};
