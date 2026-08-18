import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { authInterceptor } from './auth.interceptor';

/**
 * El vencimiento del token no se puede esperar en una prueba -dura una hora-,
 * asi que se simula respondiendo 401 a la primera llamada. Es justo el caso
 * que antes tumbaba la sesion en medio del uso.
 */
describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let navegaciones: string[][];

  beforeEach(() => {
    navegaciones = [];
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: { navigate: (r: string[]) => { navegaciones.push(r); } } },
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('manda las cookies de sesion en toda peticion a la API', () => {
    http.get('/api/alertas').subscribe();
    const req = httpMock.expectOne('/api/alertas');
    expect(req.request.withCredentials).toBe(true);
    req.flush({});
  });

  it('ante un 401 renueva y reintenta la peticion original', () => {
    let recibido: unknown = null;
    http.get('/api/alertas').subscribe((r) => (recibido = r));

    httpMock.expectOne('/api/alertas').flush('', { status: 401, statusText: 'Unauthorized' });

    httpMock.expectOne('/api/auth/refresh').flush(null, { status: 204, statusText: 'No Content' });

    // El reintento es la misma peticion, no una nueva inventada.
    const reintento = httpMock.expectOne('/api/alertas');
    expect(reintento.request.method).toBe('GET');
    reintento.flush({ ok: true });

    expect(recibido).toEqual({ ok: true });
    expect(navegaciones).toEqual([]);
  });

  it('si el refresh tambien falla, va al login', () => {
    let fallo: unknown = null;
    http.get('/api/alertas').subscribe({ error: (e) => (fallo = e) });

    httpMock.expectOne('/api/alertas').flush('', { status: 401, statusText: 'Unauthorized' });
    httpMock.expectOne('/api/auth/refresh').flush('', { status: 401, statusText: 'Unauthorized' });

    expect(fallo).toBeTruthy();
    expect(navegaciones).toEqual([['/login']]);
  });

  it('varias peticiones que vencen juntas comparten un solo refresh', () => {
    http.get('/api/alertas').subscribe();
    http.get('/api/sesiones/hoy').subscribe();
    http.get('/api/estudiantes').subscribe();

    httpMock.expectOne('/api/alertas').flush('', { status: 401, statusText: 'Unauthorized' });
    httpMock.expectOne('/api/sesiones/hoy').flush('', { status: 401, statusText: 'Unauthorized' });
    httpMock.expectOne('/api/estudiantes').flush('', { status: 401, statusText: 'Unauthorized' });

    // Uno solo: si fueran tres, dos trabajarian sobre una cookie ya rotada.
    const refrescos = httpMock.match('/api/auth/refresh');
    expect(refrescos).toHaveLength(1);
    refrescos[0].flush(null, { status: 204, statusText: 'No Content' });

    httpMock.expectOne('/api/alertas').flush({});
    httpMock.expectOne('/api/sesiones/hoy').flush({});
    httpMock.expectOne('/api/estudiantes').flush({});
  });

  it('el login que devuelve 401 no intenta renovar nada', () => {
    let fallo: unknown = null;
    http.post('/api/auth/login', {}).subscribe({ error: (e) => (fallo = e) });

    httpMock.expectOne('/api/auth/login').flush('', { status: 401, statusText: 'Unauthorized' });

    // Credenciales equivocadas no son una sesion vencida: renovar aqui seria
    // un bucle y ademas taparia el mensaje real al usuario.
    httpMock.expectNone('/api/auth/refresh');
    expect(fallo).toBeTruthy();
    expect(navegaciones).toEqual([]);
  });

  it('un error que no sea 401 se propaga tal cual', () => {
    let fallo: unknown = null;
    http.get('/api/alertas').subscribe({ error: (e) => (fallo = e) });

    httpMock.expectOne('/api/alertas').flush('', { status: 500, statusText: 'Server Error' });

    httpMock.expectNone('/api/auth/refresh');
    expect(fallo).toBeTruthy();
  });
});
