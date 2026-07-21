import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Adjunta las cookies de sesion (access_token, refresh_token) a cada
 * solicitud hacia la API. El JWT viaja en cookies HttpOnly (Bloque A.1),
 * por lo que JavaScript no lo lee ni lo adjunta manualmente: solo hace
 * falta pedirle al navegador que envie las cookies con withCredentials.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req.clone({ withCredentials: true }));
};
