import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login.component';
import { AuthService } from '../auth.service';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;
  let authServiceMock: { login: ReturnType<typeof vi.fn> };
  let router: Router;

  beforeEach(async () => {
    authServiceMock = { login: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [{ provide: AuthService, useValue: authServiceMock }],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
  });

  it('login exitoso navega a la ruta segun el rol devuelto', () => {
    authServiceMock.login.mockReturnValue(of({ username: 'admin@sged.test', nombre: 'Admin', rol: 'ADMINISTRADOR' }));
    component.username = 'admin@sged.test';
    component.password = 'Admin2026!';

    component.onSubmit();

    expect(authServiceMock.login).toHaveBeenCalledWith({ username: 'admin@sged.test', password: 'Admin2026!' });
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('login exitoso de un ESTUDIANTE navega a marcar-asistencia', () => {
    authServiceMock.login.mockReturnValue(of({ username: 'est@sged.test', nombre: 'Est', rol: 'ESTUDIANTE' }));
    component.username = 'est@sged.test';
    component.password = 'clave123';

    component.onSubmit();

    expect(router.navigate).toHaveBeenCalledWith(['/estudiante/marcar-asistencia']);
  });

  it('credenciales incorrectas (401) muestra el mensaje y limpia la contraseña', () => {
    authServiceMock.login.mockReturnValue(throwError(() => ({ status: 401 })));
    component.username = 'admin@sged.test';
    component.password = 'incorrecta';

    component.onSubmit();

    expect(component.error()).toBe('Usuario o contraseña incorrectos');
    expect(component.password).toBe('');
    expect(component.loading()).toBe(false);
  });

  it('demasiados intentos (429) muestra el detalle que manda el backend', () => {
    authServiceMock.login.mockReturnValue(throwError(() => ({
      status: 429, error: { detail: 'Intenta de nuevo en 15 minutos.' },
    })));
    component.username = 'admin@sged.test';
    component.password = 'x';

    component.onSubmit();

    expect(component.error()).toBe('Intenta de nuevo en 15 minutos.');
  });

  it('sin conexion (status 0) muestra el mensaje de servidor inalcanzable', () => {
    authServiceMock.login.mockReturnValue(throwError(() => ({ status: 0 })));
    component.username = 'admin@sged.test';
    component.password = 'x';

    component.onSubmit();

    expect(component.error()).toBe('No hay conexión con el servidor');
  });

  it('un doble envio mientras carga no dispara una segunda llamada al backend', () => {
    authServiceMock.login.mockReturnValue(of({ username: 'admin@sged.test', nombre: 'Admin', rol: 'ADMINISTRADOR' }));
    component.username = 'admin@sged.test';
    component.password = 'Admin2026!';

    component.loading.set(true);
    component.onSubmit();

    expect(authServiceMock.login).not.toHaveBeenCalled();
  });
});
