import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { EvaluacionDiariaComponent } from './evaluacion-diaria.component';
import { EvaluacionService } from './evaluacion.service';
import { EvaluacionSesion, JugadorEvaluable } from './evaluacion.models';

/** R-08 (informe de evaluacion de calidad): flujo critico de evaluación diaria. */
describe('EvaluacionDiariaComponent', () => {
  let fixture: ComponentFixture<EvaluacionDiariaComponent>;
  let component: EvaluacionDiariaComponent;
  let servicioMock: {
    abrirSesion: ReturnType<typeof vi.fn>;
    finalizar: ReturnType<typeof vi.fn>;
    guardarConRetardo: ReturnType<typeof vi.fn>;
    registrarLesion: ReturnType<typeof vi.fn>;
    darDeAltaLesion: ReturnType<typeof vi.fn>;
    estado: ReturnType<typeof signal>;
    pendientes: ReturnType<typeof signal>;
  };

  const jugadorHabilitado: JugadorEvaluable = {
    idEstudiante: 1, nombreCompleto: 'Ana Vera', categoria: 'SUB-12', idPosicion: null, posicion: null,
    estadoAsistencia: 'PRESENTE', puntajes: { Actitud: 5 }, precargado: false, lesionado: false,
    idLesion: null, puedeEvaluarse: true, motivoBloqueo: null,
  };
  const jugadorBloqueado: JugadorEvaluable = {
    idEstudiante: 2, nombreCompleto: 'Luis Mora', categoria: 'SUB-12', idPosicion: null, posicion: null,
    estadoAsistencia: 'AUSENTE', puntajes: {}, precargado: false, lesionado: false,
    idLesion: null, puedeEvaluarse: false, motivoBloqueo: 'No marcó asistencia',
  };
  const sesionBase: EvaluacionSesion = {
    idEvaluacion: 100, idSesion: 5, fecha: '2026-08-10', categoria: 'SUB-12', estado: 'BORRADOR',
    criterios: [{ idCriterio: 1, nombre: 'Actitud', descripcion: null, puntajeMaximo: 10 }],
    jugadores: [jugadorHabilitado, jugadorBloqueado], observacionGeneral: null,
  };

  async function crearComponente(idSesion = '5') {
    servicioMock = {
      abrirSesion: vi.fn().mockReturnValue(of(sesionBase)),
      finalizar: vi.fn(),
      guardarConRetardo: vi.fn(),
      registrarLesion: vi.fn(),
      darDeAltaLesion: vi.fn(),
      estado: signal('guardado'),
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

  it('carga la sesion y expande al primer jugador habilitado para evaluarse', async () => {
    await crearComponente();

    expect(component.cargando()).toBe(false);
    expect(component.sesion()?.idSesion).toBe(5);
    expect(component.estaExpandido(1)).toBe(true);
    expect(component.estaExpandido(2)).toBe(false);
  });

  it('sesion inexistente (404) muestra el mensaje correspondiente', async () => {
    servicioMock = {
      abrirSesion: vi.fn().mockReturnValue(throwError(() => ({ status: 404 }))),
      finalizar: vi.fn(), guardarConRetardo: vi.fn(), registrarLesion: vi.fn(), darDeAltaLesion: vi.fn(),
      estado: signal('guardado'), pendientes: signal(0),
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
    const jugador = component.sesion()!.jugadores[0];
    jugador.precargado = true;

    component.cambiar(jugador, 'Actitud', 1, 8);

    expect(jugador.puntajes['Actitud']).toBe(8);
    expect(jugador.precargado).toBe(false);
    expect(servicioMock.guardarConRetardo).toHaveBeenCalledWith(5, {
      idEstudiante: 1, idPosicionJugada: null, puntajes: [{ idCriterio: 1, puntaje: 8 }],
    });
  });

  it('finalizar marca la sesion como FINALIZADA', async () => {
    await crearComponente();
    servicioMock.finalizar.mockReturnValue(of(undefined));

    component.finalizar();

    expect(servicioMock.finalizar).toHaveBeenCalledWith(5, '');
    expect(component.sesion()?.estado).toBe('FINALIZADA');
    expect(component.finalizando()).toBe(false);
  });

  it('guardar una lesión sin descripción no llama al backend y muestra el error', async () => {
    await crearComponente();

    component.guardarLesion(component.sesion()!.jugadores[0]);

    expect(servicioMock.registrarLesion).not.toHaveBeenCalled();
    expect(component.errorLesion()).toBe('Describí qué le pasó antes de guardar.');
  });

  it('guardar una lesión válida marca al jugador como lesionado', async () => {
    await crearComponente();
    servicioMock.registrarLesion.mockReturnValue(of({
      idLesion: 55, idEstudiante: 1, estudiante: 'Ana Vera', descripcion: 'Esguince',
      fechaLesion: '2026-08-10', fechaEstimadaRetorno: null, fechaAlta: null, activa: true,
    }));
    component.descripcionLesion.set('Esguince de tobillo');
    const jugador = component.sesion()!.jugadores[0];

    component.guardarLesion(jugador);

    expect(jugador.lesionado).toBe(true);
    expect(jugador.idLesion).toBe(55);
    expect(component.formularioLesionAbierto()).toBeNull();
  });

  it('dar de alta una lesión la desmarca del jugador', async () => {
    await crearComponente();
    const jugador = component.sesion()!.jugadores[0];
    jugador.lesionado = true;
    jugador.idLesion = 55;
    servicioMock.darDeAltaLesion.mockReturnValue(of({
      idLesion: 55, idEstudiante: 1, estudiante: 'Ana Vera', descripcion: 'Esguince',
      fechaLesion: '2026-08-10', fechaEstimadaRetorno: null, fechaAlta: '2026-08-15', activa: false,
    }));

    component.darDeAlta(jugador);

    expect(jugador.lesionado).toBe(false);
    expect(jugador.idLesion).toBeNull();
  });
});
