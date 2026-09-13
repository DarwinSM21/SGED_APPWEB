import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { FichaEstudianteComponent } from './ficha-estudiante.component';
import { PersonasService } from './personas.service';
import { PersonasStateService } from './personas-state.service';
import { PersonaConEstado } from './personas.models';

describe('FichaEstudianteComponent', () => {
  let fixture: ComponentFixture<FichaEstudianteComponent>;
  let component: FichaEstudianteComponent;
  let state: PersonasStateService;
  let servicioMock: {
    crearEstudiante: ReturnType<typeof vi.fn>;
    siguienteCodigoEstudiante: ReturnType<typeof vi.fn>;
  };

  const personaSinFicha: PersonaConEstado = {
    persona: {
      personId: 1, name: 'Ana', lastName: 'Vera', nationalId: '0912345678', email: 'ana@sged.test',
      phone: null, photo: null, birthDate: '2012-05-10', active: true, createdAt: '2026-01-01T00:00:00Z',
    },
    user: null, student: null, coach: null, representante: null,
  };

  beforeEach(async () => {
    servicioMock = {
      crearEstudiante: vi.fn(),
      siguienteCodigoEstudiante: vi.fn(() => of('EST-2026-0007')),
    };

    await TestBed.configureTestingModule({
      imports: [FichaEstudianteComponent],
      providers: [PersonasStateService, { provide: PersonasService, useValue: servicioMock }],
    }).compileComponents();

    fixture = TestBed.createComponent(FichaEstudianteComponent);
    component = fixture.componentInstance;
    state = TestBed.inject(PersonasStateService);
    state.seleccionar(personaSinFicha);
    fixture.detectChanges();
  });

  it('sin categoria seleccionada no llama al backend', () => {
    component.formEstudiante.categoryId = null;

    component.crearEstudiante();

    expect(servicioMock.crearEstudiante).not.toHaveBeenCalled();
  });

  it('con categoria valida crea la ficha con el idPersona de la persona seleccionada', () => {
    servicioMock.crearEstudiante.mockReturnValue(of({}));
    vi.spyOn(state, 'cargarPersonas').mockImplementation(() => {});
    component.formEstudiante = { categoryId: 3, studentCode: 'EST-2026-001', enrollmentDate: '2026-08-10', positionId: null };

    component.crearEstudiante();

    expect(servicioMock.crearEstudiante).toHaveBeenCalledWith({
      personId: 1, categoryId: 3, generalStatusId: 1,
      studentCode: 'EST-2026-001', enrollmentDate: '2026-08-10',
      weight: null, height: null, positionId: null,
    });
    expect(component.guardandoEstudiante()).toBe(false);
    expect(state.cargarPersonas).toHaveBeenCalledWith(true);
  });

  it('un codigo duplicado muestra el error del backend sin romper el formulario', () => {
    servicioMock.crearEstudiante.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 409, error: { detail: 'El código ya existe.' },
    })));
    component.formEstudiante = { categoryId: 3, studentCode: 'EST-2026-001', enrollmentDate: '2026-08-10', positionId: null };

    component.crearEstudiante();

    expect(component.errorEstudiante()).toBe('El código ya existe.');
    expect(component.guardandoEstudiante()).toBe(false);
  });

  it('cambiar de persona seleccionada pide un codigo nuevo al servidor', () => {
    component.formEstudiante.studentCode = 'algo-a-medio-escribir';

    state.seleccionar({ ...personaSinFicha, persona: { ...personaSinFicha.persona, personId: 2 } });
    fixture.detectChanges();

    expect(servicioMock.siguienteCodigoEstudiante).toHaveBeenCalled();
    expect(component.formEstudiante.studentCode).toBe('EST-2026-0007');
  });
});
