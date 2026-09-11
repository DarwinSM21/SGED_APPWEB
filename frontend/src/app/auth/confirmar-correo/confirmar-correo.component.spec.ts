import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ConfirmarCorreoComponent } from './confirmar-correo.component';
import { AuthService } from '../auth.service';

function configurar(token: string | null) {
  const authServiceMock = { confirmarCorreo: vi.fn() };
  TestBed.configureTestingModule({
    imports: [ConfirmarCorreoComponent],
    providers: [
      provideRouter([]),
      { provide: AuthService, useValue: authServiceMock },
      {
        provide: ActivatedRoute,
        useValue: { snapshot: { queryParamMap: convertToParamMap(token ? { token } : {}) } },
      },
    ],
  });
  return { authServiceMock };
}

function crear() {
  const fixture = TestBed.createComponent(ConfirmarCorreoComponent);
  fixture.detectChanges();
  return fixture.componentInstance;
}

describe('ConfirmarCorreoComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('sin token en la URL: no llama al backend y queda en estado inicial', () => {
    const { authServiceMock } = configurar(null);
    const component = crear();

    expect(component.token).toBe('');
    expect(authServiceMock.confirmarCorreo).not.toHaveBeenCalled();
  });

  it('con token: canjea contra el backend y pasa a estado ok', () => {
    const { authServiceMock } = configurar('tok-123');
    authServiceMock.confirmarCorreo.mockReturnValue(of(undefined));

    const component = crear();

    expect(authServiceMock.confirmarCorreo).toHaveBeenCalledWith('tok-123');
    expect(component.estado()).toBe('ok');
  });

  it('un 400 del backend deja el estado en error con el diagnostico', () => {
    const { authServiceMock } = configurar('tok');
    authServiceMock.confirmarCorreo.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 400, error: { detail: 'enlace invalido' } })));

    const component = crear();

    expect(component.estado()).toBe('error');
    expect(component.fallo()?.mensaje).toBe('enlace invalido');
  });
});
