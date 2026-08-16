import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { MarcarAsistenciaComponent } from './marcar-asistencia.component';
import { MarcarAsistenciaService } from './marcar-asistencia.service';

/**
 * R-08 (informe de evaluacion de calidad): flujo critico de registro de
 * asistencia por QR.
 *
 * enviarToken() y mensajeDeError() son privados: la ruta publica hacia
 * ellos pasa por iniciarCamara() -> escanearCuadro(), que depende de
 * getUserMedia y de decodificar pixeles reales de un <canvas> con jsQR.
 * Simular esa cadena completa probaria mas los mocks de camara que el
 * componente. Se accede a los metodos privados directamente (tecnica
 * pragmatica y comun en TypeScript) para cubrir las reglas que si
 * importan: como reacciona el estado de la pantalla a cada respuesta del
 * backend.
 */
describe('MarcarAsistenciaComponent', () => {
  let fixture: ComponentFixture<MarcarAsistenciaComponent>;
  let component: MarcarAsistenciaComponent;
  let servicioMock: { marcar: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    servicioMock = { marcar: vi.fn() };

    Object.defineProperty(navigator, 'mediaDevices', {
      value: { getUserMedia: vi.fn().mockRejectedValue(new Error('no camera in test env')) },
      configurable: true,
    });

    await TestBed.configureTestingModule({
      imports: [MarcarAsistenciaComponent],
      providers: [{ provide: MarcarAsistenciaService, useValue: servicioMock }],
    }).compileComponents();

    fixture = TestBed.createComponent(MarcarAsistenciaComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('un token valido dentro de tolerancia marca PRESENTE', () => {
    servicioMock.marcar.mockReturnValue(of({ estado: 'PRESENTE' }));

    (component as any).enviarToken('token-valido');

    expect(component.resultado()).toEqual({ estado: 'PRESENTE' });
    expect(component.enviando()).toBe(false);
  });

  it('un token valido fuera de tolerancia marca TARDE', () => {
    servicioMock.marcar.mockReturnValue(of({ estado: 'TARDE' }));

    (component as any).enviarToken('token-tarde');

    expect(component.resultado()?.estado).toBe('TARDE');
  });

  it('token expirado o ya usado (410) muestra el mensaje de reintentar en recepción', () => {
    servicioMock.marcar.mockReturnValue(throwError(() => ({ status: 410 })));

    (component as any).enviarToken('token-viejo');

    expect(component.fallo()).toBe('Ese código ya expiró o ya se usó. Pide uno nuevo en recepción y vuelve a intentar.');
    expect(component.resultado()).toBeNull();
  });

  it('asistencia duplicada (400) muestra "ya marcaste tu asistencia"', () => {
    servicioMock.marcar.mockReturnValue(throwError(() => ({ status: 400 })));

    (component as any).enviarToken('token-repetido');

    expect(component.fallo()).toBe('Ya marcaste tu asistencia en esta sesión.');
  });

  it('cuenta sin ficha de estudiante (404) pide contactar a un administrador', () => {
    servicioMock.marcar.mockReturnValue(throwError(() => ({ status: 404 })));

    (component as any).enviarToken('token-huerfano');

    expect(component.fallo()).toBe('Tu cuenta no está vinculada a un estudiante. Contacta a un administrador.');
  });

  it('reintentar limpia el fallo anterior', () => {
    servicioMock.marcar.mockReturnValue(throwError(() => ({ status: 400 })));
    (component as any).enviarToken('token-repetido');
    expect(component.fallo()).not.toBeNull();

    component.reintentar();

    expect(component.fallo()).toBeNull();
  });

  it('reiniciar limpia el resultado anterior para poder escanear otro código', () => {
    servicioMock.marcar.mockReturnValue(of({ estado: 'PRESENTE' }));
    (component as any).enviarToken('token-valido');
    expect(component.resultado()).not.toBeNull();

    component.reiniciar();

    expect(component.resultado()).toBeNull();
  });
});
