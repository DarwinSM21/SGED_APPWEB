import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { RestablecerComponent } from './restablecer.component';
import { AuthService } from '../auth.service';

function configurar(token: string | null) {
  const authServiceMock = { restablecerPassword: vi.fn() };
  TestBed.configureTestingModule({
    imports: [RestablecerComponent],
    providers: [
      provideRouter([]),
      { provide: AuthService, useValue: authServiceMock },
      {
        provide: ActivatedRoute,
        useValue: { snapshot: { queryParamMap: convertToParamMap(token ? { token } : {}) } },
      },
    ],
  });
  const fixture = TestBed.createComponent(RestablecerComponent);
  const router = TestBed.inject(Router);
  vi.spyOn(router, 'navigate').mockResolvedValue(true);
  fixture.detectChanges();
  return { fixture, component: fixture.componentInstance, authServiceMock, router };
}

describe('RestablecerComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('sin token en la URL, no muestra el formulario', () => {
    const { component } = configurar(null);
    expect(component.token).toBe('');
  });

  it('la politica se valida en vivo: 8+, letra, digito y coincidencia', () => {
    const { component } = configurar('tok');

    component.password = 'corta1';
    component.confirmar = 'corta1';
    expect(component.formularioValido()).toBe(false); // < 8

    component.password = 'sinnumero';
    component.confirmar = 'sinnumero';
    expect(component.formularioValido()).toBe(false); // sin digito

    component.password = 'clave1234';
    component.confirmar = 'clave9999';
    expect(component.formularioValido()).toBe(false); // no coinciden

    component.password = 'clave1234';
    component.confirmar = 'clave1234';
    expect(component.formularioValido()).toBe(true);
  });

  it('formulario valido: llama al backend y redirige a /login', () => {
    const { component, authServiceMock, router } = configurar('tok-123');
    authServiceMock.restablecerPassword.mockReturnValue(of(undefined));
    component.password = 'clave1234';
    component.confirmar = 'clave1234';

    component.onSubmit();

    expect(authServiceMock.restablecerPassword).toHaveBeenCalledWith('tok-123', 'clave1234');
    expect(router.navigate).toHaveBeenCalledWith(['/login'], { queryParams: { restablecida: '1' } });
  });

  it('formulario invalido no llama al backend', () => {
    const { component, authServiceMock } = configurar('tok');
    component.password = 'x';
    component.confirmar = 'x';

    component.onSubmit();

    expect(authServiceMock.restablecerPassword).not.toHaveBeenCalled();
  });

  it('un 400 marca el token como roto y ofrece pedir uno nuevo', () => {
    const { component, authServiceMock } = configurar('tok');
    authServiceMock.restablecerPassword.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 400, error: { detail: 'enlace invalido' } })));
    component.password = 'clave1234';
    component.confirmar = 'clave1234';

    component.onSubmit();

    expect(component.tokenRoto()).toBe(true);
    expect(component.fallo()?.mensaje).toBe('enlace invalido');
  });

  it('un 422 no marca el token como roto', () => {
    const { component, authServiceMock } = configurar('tok');
    authServiceMock.restablecerPassword.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 422, error: { detail: 'contrasena debil' } })));
    component.password = 'clave1234';
    component.confirmar = 'clave1234';

    component.onSubmit();

    expect(component.tokenRoto()).toBe(false);
  });
});
