import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { RecuperarComponent } from './recuperar.component';
import { AuthService } from '../auth.service';

describe('RecuperarComponent', () => {
  let fixture: ComponentFixture<RecuperarComponent>;
  let component: RecuperarComponent;
  let authServiceMock: { solicitarRecuperacion: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    authServiceMock = { solicitarRecuperacion: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [RecuperarComponent],
      providers: [provideRouter([]), { provide: AuthService, useValue: authServiceMock }],
    }).compileComponents();

    fixture = TestBed.createComponent(RecuperarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('envia la solicitud y muestra la confirmacion generica', () => {
    authServiceMock.solicitarRecuperacion.mockReturnValue(of({ mensaje: 'ok' }));
    component.identificador = '  ana@sged.test ';

    component.onSubmit();

    expect(authServiceMock.solicitarRecuperacion).toHaveBeenCalledWith('ana@sged.test');
    expect(component.enviado()).toBe(true);
  });

  it('identificador en blanco no llama al backend', () => {
    component.identificador = '   ';

    component.onSubmit();

    expect(authServiceMock.solicitarRecuperacion).not.toHaveBeenCalled();
    expect(component.fallo()?.mensaje).toContain('usuario');
  });

  it('un error del servidor se muestra y no marca como enviado', () => {
    authServiceMock.solicitarRecuperacion.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500 })));
    component.identificador = 'ana@sged.test';

    component.onSubmit();

    expect(component.enviado()).toBe(false);
    expect(component.fallo()).not.toBeNull();
    expect(component.loading()).toBe(false);
  });

  it('no dispara una segunda llamada mientras carga', () => {
    authServiceMock.solicitarRecuperacion.mockReturnValue(of({ mensaje: 'ok' }));
    component.identificador = 'ana@sged.test';
    component.loading.set(true);

    component.onSubmit();

    expect(authServiceMock.solicitarRecuperacion).not.toHaveBeenCalled();
  });
});
