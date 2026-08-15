import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { roleGuard } from './role.guard';
import { AuthService } from './auth.service';

describe('roleGuard', () => {
  let authServiceMock: { currentUser: () => { rol: string } | null };
  let router: Router;

  function configurar(rol: string | null) {
    authServiceMock = { currentUser: () => (rol ? { rol } : null) };
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: authServiceMock }],
    });
    router = TestBed.inject(Router);
  }

  it('permite el acceso a /admin/auditorias cuando el rol es ADMINISTRADOR', () => {
    configurar('ADMINISTRADOR');
    const guard = roleGuard(['ADMINISTRADOR']);

    const resultado = TestBed.runInInjectionContext(() => guard({} as any, {} as any));

    expect(resultado).toBe(true);
  });

  it('redirige a /dashboard cuando el rol no está en la lista permitida (ej. RECEPCIONISTA en /admin/auditorias)', () => {
    configurar('RECEPCIONISTA');
    const guard = roleGuard(['ADMINISTRADOR']);
    const crearUrlTreeSpy = vi.spyOn(router, 'createUrlTree');

    TestBed.runInInjectionContext(() => guard({} as any, {} as any));

    expect(crearUrlTreeSpy).toHaveBeenCalledWith(['/dashboard']);
  });

  it('permite el acceso a /reportes para cualquiera de los roles permitidos (ADMINISTRADOR, RECEPCIONISTA, ENTRENADOR)', () => {
    configurar('ENTRENADOR');
    const guard = roleGuard(['ADMINISTRADOR', 'RECEPCIONISTA', 'ENTRENADOR']);

    const resultado = TestBed.runInInjectionContext(() => guard({} as any, {} as any));

    expect(resultado).toBe(true);
  });

  it('redirige a /dashboard cuando no hay usuario autenticado', () => {
    configurar(null);
    const guard = roleGuard(['ADMINISTRADOR']);
    const crearUrlTreeSpy = vi.spyOn(router, 'createUrlTree');

    TestBed.runInInjectionContext(() => guard({} as any, {} as any));

    expect(crearUrlTreeSpy).toHaveBeenCalledWith(['/dashboard']);
  });
});
