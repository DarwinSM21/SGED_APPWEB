import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { EvaluacionDiariaComponent } from './evaluacion-diaria.component';
import { EvaluacionService } from './evaluacion.service';
import { EvaluacionSesion, JugadorEvaluable } from './evaluacion.models';

describe('EvaluacionDiariaComponent', () => {
  let fixture: ComponentFixture<EvaluacionDiariaComponent>;
  let component: EvaluacionDiariaComponent;
  let servicioMock: {
    abrirSesion: ReturnType<typeof vi.fn>;
    finalizar: ReturnType<typeof vi.fn>;
    guardarConRetardo: ReturnType<typeof vi.fn>;
    registrarLesion: ReturnType<typeof vi.fn>;
    darDeAltaLesion: ReturnType<typeof vi.fn>;
    posicionesActivas: ReturnType<typeof vi.fn>;
    status: ReturnType<typeof signal>;
    pendientes: ReturnType<typeof signal>;
  };

  const jugadorHabilitado: JugadorEvaluable = {
    studentId: 1, fullName: 'Ana Vera', category: 'SUB-12', positionId: null, position: null,
    attendanceStatus: 'PRESENTE', scores: { Actitud: 5 }, preloaded: false, injured: false,
    injuryId: null, canBeEvaluated: true, blockReason: null,
  };
  const jugadorBloqueado: JugadorEvaluable = {
    studentId: 2, fullName: 'Luis Mora', category: 'SUB-12', positionId: null, position: null,
    attendanceStatus: 'AUSENTE', scores: {}, preloaded: false, injured: false,
    injuryId: null, canBeEvaluated: false, blockReason: 'No marcó asistencia',
  };
  const sesionBase: EvaluacionSesion = {
    evaluationId: 100, sessionId: 5, date: '2026-08-10', category: 'SUB-12', status: 'BORRADOR',
    criteria: [{ criterionId: 1, name: 'Actitud', description: null, maxScore: 10 }],
    players: [jugadorHabilitado, jugadorBloqueado], generalNote: null,
  };

  async function crearComponente(idSesion = '5') {
    servicioMock = {
      abrirSesion: vi.fn().mockReturnValue(of(sesionBase)),
      finalizar: vi.fn(),
      guardarConRetardo: vi.fn(),
      registrarLesion: vi.fn(),
      darDeAltaLesion: vi.fn(),
      posicionesActivas: vi.fn().mockReturnValue(of([])),
      status: signal('guardado'),
      pendientes: signal(0),
    };

    await TestBed.configureTestingModule({
      imports: [EvaluacionDiariaComponent],
      providers: [
        { provide: EvaluacionService, useValue: servicioMock },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map([['idSesion', idSesion]]) } } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(EvaluacionDiariaComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('carga la sesion con todas las fichas plegadas', async () => {
    await crearComponente();

    expect(component.cargando()).toBe(false);
    expect(component.sesion()?.sessionId).toBe(5);

    expect(component.estaExpandido(1)).toBe(false);
    expect(component.estaExpandido(2)).toBe(false);
  });

  it('alternar abre y cierra la ficha de un jugador', async () => {
    await crearComponente();

    component.alternar(1);
    expect(component.estaExpandido(1)).toBe(true);
    expect(component.estaExpandido(2)).toBe(false);

    component.alternar(1);
    expect(component.estaExpandido(1)).toBe(false);
  });

  it('sesion inexistente (404) muestra el mensaje correspondiente', async () => {
    servicioMock = {
      abrirSesion: vi.fn().mockReturnValue(throwError(() => ({ status: 404 }))),
      finalizar: vi.fn(), guardarConRetardo: vi.fn(), registrarLesion: vi.fn(), darDeAltaLesion: vi.fn(),
      posicionesActivas: vi.fn().mockReturnValue(of([])),
      status: signal('guardado'), pendientes: signal(0),
    };
    await TestBed.configureTestingModule({
      imports: [EvaluacionDiariaComponent],
      providers: [
        { provide: EvaluacionService, useValue: servicioMock },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map([['idSesion', '999']]) } } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(EvaluacionDiariaComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.error()).toBe('Esa sesión de entrenamiento no existe.');
    expect(component.cargando()).toBe(false);
  });

  it('mover un criterio programa el autoguardado con el puntaje nuevo y deja de estar precargado', async () => {
    await crearComponente();
    const jugador = component.sesion()!.players[0];
    jugador.preloaded = true;

    component.cambiar(jugador, 'Actitud', 1, 8);

    expect(jugador.scores['Actitud']).toBe(8);
    expect(jugador.preloaded).toBe(false);
    expect(servicioMock.guardarConRetardo).toHaveBeenCalledWith(5, {
      studentId: 1, lineupPositionId: null, scores: [{ criterionId: 1, score: 8 }],
    });
  });

  it('finalizar marca la sesion como FINALIZADA', async () => {
    await crearComponente();
    servicioMock.finalizar.mockReturnValue(of(undefined));

    component.finalizar();

    expect(servicioMock.finalizar).toHaveBeenCalledWith(5, '');
    expect(component.sesion()?.status).toBe('FINALIZADA');
    expect(component.finalizando()).toBe(false);
  });

  it('guardar una lesión sin descripción no llama al backend y muestra el error', async () => {
    await crearComponente();

    component.guardarLesion(component.sesion()!.players[0]);

    expect(servicioMock.registrarLesion).not.toHaveBeenCalled();
    expect(component.errorLesion()).toBe('Describí qué le pasó antes de guardar.');
  });

  it('RNF-25: guardar una lesión con descripción demasiado larga no llama al backend', async () => {
    await crearComponente();

    component.descripcionLesion.set('x'.repeat(component.MAX_DESCRIPCION_LESION + 1));
    component.guardarLesion(component.sesion()!.players[0]);

    expect(servicioMock.registrarLesion).not.toHaveBeenCalled();
    expect(component.errorLesion()).toContain(String(component.MAX_DESCRIPCION_LESION));
  });

  it('guardar una lesión válida marca al jugador como lesionado', async () => {
    await crearComponente();
    servicioMock.registrarLesion.mockReturnValue(of({
      injuryId: 55, studentId: 1, student: 'Ana Vera', description: 'Esguince',
      injuryDate: '2026-08-10', estimatedReturnDate: null, dischargeDate: null, active: true,
    }));
    component.descripcionLesion.set('Esguince de tobillo');
    const jugador = component.sesion()!.players[0];

    component.guardarLesion(jugador);

    expect(jugador.injured).toBe(true);
    expect(jugador.injuryId).toBe(55);
    expect(component.formularioLesionAbierto()).toBeNull();
  });

  it('dar de alta una lesión la desmarca del jugador', async () => {
    await crearComponente();
    const jugador = component.sesion()!.players[0];
    jugador.injured = true;
    jugador.injuryId = 55;
    servicioMock.darDeAltaLesion.mockReturnValue(of({
      injuryId: 55, studentId: 1, student: 'Ana Vera', description: 'Esguince',
      injuryDate: '2026-08-10', estimatedReturnDate: null, dischargeDate: '2026-08-15', active: false,
    }));

    component.darDeAlta(jugador);

    expect(jugador.injured).toBe(false);
    expect(jugador.injuryId).toBeNull();
  });
});
