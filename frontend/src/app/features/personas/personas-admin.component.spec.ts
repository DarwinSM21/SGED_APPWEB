import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { PersonasAdminComponent } from './personas-admin.component';
import { PersonasService } from './personas.service';
import { EntrenadorResponse, EstudianteResponse, PersonaResponse, RepresentanteResponse, UsuarioResponse } from './personas.models';

describe('PersonasAdminComponent', () => {
  let fixture: ComponentFixture<PersonasAdminComponent>;
  let servicioMock: Partial<PersonasService>;

  const persona1: PersonaResponse = {
    idPersona: 1, nombre: 'Ana', apellido: 'Vera', cedula: '0912345678', correo: 'ana@sged.test',
    telefono: null, foto: null, fechaNacimiento: '2010-01-01', activo: true, createdAt: '2026-01-01T00:00:00Z',
  };
  const persona2: PersonaResponse = {
    idPersona: 2, nombre: 'Luis', apellido: 'Mora', cedula: '0987654321', correo: 'luis@sged.test',
    telefono: null, foto: null, fechaNacimiento: '2011-01-01', activo: true, createdAt: '2026-01-01T00:00:00Z',
  };
  const usuario1: UsuarioResponse = {
    idUsuario: 10, idPersona: 1, nombrePersona: 'Ana', apellidoPersona: 'Vera', correoPersona: 'ana@sged.test',
    idEstadoGeneral: 1, estadoGeneralNombre: 'Activo', username: 'ana@sged.test', roles: ['ESTUDIANTE'],
    ultimoAcceso: null, activo: true, createdAt: '2026-01-01T00:00:00Z',
  };

  beforeEach(async () => {
    servicioMock = {
      listarPersonas: () => of({ content: [persona1, persona2] }),
      listarUsuarios: () => of({ content: [usuario1] as UsuarioResponse[] }),
      listarEstudiantes: () => of({ content: [] as EstudianteResponse[] }),
      listarEntrenadores: () => of({ content: [] as EntrenadorResponse[] }),
      listarRepresentantes: () => of({ content: [] as RepresentanteResponse[] }),
      categoriasActivas: () => of([]),
      especialidadesActivas: () => of([]),
      posicionesActivas: () => of([]),
    };

    await TestBed.configureTestingModule({
      imports: [PersonasAdminComponent],
      providers: [{ provide: PersonasService, useValue: servicioMock }],
    }).compileComponents();

    fixture = TestBed.createComponent(PersonasAdminComponent);
    fixture.detectChanges();
  });

  it('carga las personas al iniciar y las muestra en el listado', () => {
    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('Ana Vera');
    expect(texto).toContain('Luis Mora');
  });

  it('sin seleccion muestra el aviso de "Seleccioná una persona"', () => {
    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('Seleccioná una persona de la lista');
  });

  it('seleccionar una persona en el listado la muestra en el panel de detalle', () => {
    const filas = fixture.nativeElement.querySelectorAll('.fila-persona') as NodeListOf<HTMLButtonElement>;
    expect(filas.length).toBe(2);

    filas[0].click();
    fixture.detectChanges();

    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('Cuenta de usuario');
    expect(texto).toContain('ana@sged.test');
  });

  it('cambiar a la pestaña Usuarios renderiza PersonasGestionComponent con los datos cargados', () => {
    const botonesTab = fixture.nativeElement.querySelectorAll('.tab') as NodeListOf<HTMLButtonElement>;
    const tabUsuarios = Array.from(botonesTab).find((b) => b.textContent?.trim() === 'Usuarios');
    expect(tabUsuarios).toBeTruthy();

    tabUsuarios!.click();
    fixture.detectChanges();

    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('ana@sged.test');
    expect(texto).not.toContain('Seleccioná una persona de la lista');
  });

  it('hacer click en una fila de la pestaña Usuarios vuelve a Personas con esa persona seleccionada', () => {
    const botonesTab = fixture.nativeElement.querySelectorAll('.tab') as NodeListOf<HTMLButtonElement>;
    Array.from(botonesTab).find((b) => b.textContent?.trim() === 'Usuarios')!.click();
    fixture.detectChanges();

    const filaGestion = fixture.nativeElement.querySelector('.fila-gestion') as HTMLButtonElement;
    filaGestion.click();
    fixture.detectChanges();

    const tabsActivos = Array.from(fixture.nativeElement.querySelectorAll('.tab--activo')) as HTMLButtonElement[];
    expect(tabsActivos.length).toBe(1);
    expect(tabsActivos[0].textContent?.trim()).toBe('Personas');

    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('Cuenta de usuario');
  });
});
