import { ApplicationConfig, LOCALE_ID, provideBrowserGlobalErrorListeners, isDevMode } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeEsEc from '@angular/common/locales/es-EC';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideServiceWorker } from '@angular/service-worker';

import { routes } from './app.routes';
import { authInterceptor } from './auth/auth.interceptor';

registerLocaleData(localeEsEc);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    // Sin esto el pipe `date` (usado en Inventario, Personas, Notificaciones)
    // cae al locale por defecto de Angular (en-US): fechas en ingles y en
    // orden mes/dia en una app que es 100% en espanol para Ecuador.
    { provide: LOCALE_ID, useValue: 'es-EC' },
    // Solo fuera de desarrollo: un service worker cacheando durante `ng serve`
    // sirve versiones viejas del codigo y confunde mas de lo que ayuda.
    // registerWhenStable evita competir con la carga inicial por el ancho de
    // banda, que en un celular en la cancha es escaso.
    provideServiceWorker('ngsw-worker.js', {
      enabled: !isDevMode(),
      registrationStrategy: 'registerWhenStable:30000',
    }),
  ]
};
