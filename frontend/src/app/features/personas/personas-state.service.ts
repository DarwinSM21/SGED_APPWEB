import { Injectable, computed, inject, signal } from '@angular/core';
import { PersonasService } from './personas.service';
import {
  CategoriaOpcion, EntrenadorResponse, EspecialidadOpcion, EstudianteResponse, PersonaConEstado, PersonaResponse,
  RepresentanteResponse, UsuarioResponse,
} from './personas.models';

/**
 * Estado compartido del maestro-detalle de ADMINISTRADOR (R-05, informe de
 * evaluacion de calidad): antes vivia como campos de instancia de
 * PersonasAdminComponent (840 lineas, 32 miembros); ahora lo consume por
 * inyeccion cualquier componente de la pantalla -lista, detalle, fichas de
 * usuario/estudiante/entrenador/representante- sin pasarlo por @Input.
 *
 * Se provee a nivel de PersonasAdminComponent (no 'root'): el estado debe
 * nacer limpio cada vez que se entra a la pantalla, no sobrevivir a la
 * navegacion como lo haria un servicio singleton de la app.
 */
@Injectable()
export class PersonasStateService {
  private readonly servicio = inject(PersonasService);

  // Cada lista se carga y falla de forma independiente (ver
  // personas.service.ts): una que tarde o falle no bloquea a las demas,
  // solo deja esa columna de estado vacia hasta el proximo refresco.
  readonly personasBase = signal<PersonaResponse[]>([]);
  readonly usuarios = signal<UsuarioResponse[]>([]);
  readonly estudiantes = signal<EstudianteResponse[]>([]);
  readonly entrenadores = signal<EntrenadorResponse[]>([]);
  readonly representantes = signal<RepresentanteResponse[]>([]);
  readonly categorias = signal<CategoriaOpcion[]>([]);
  readonly especialidades = signal<EspecialidadOpcion[]>([]);
  readonly cargando = signal(true);

  readonly personas = computed<PersonaConEstado[]>(() => {
    const usuarios = this.usuarios(), estudiantes = this.estudiantes(),
      entrenadores = this.entrenadores(), representantes = this.representantes();
    return this.personasBase().map((persona) => ({
      persona,
      usuario: usuarios.find((u) => u.idPersona === persona.idPersona) ?? null,
      estudiante: estudiantes.find((e) => e.idPersona === persona.idPersona) ?? null,
      entrenador: entrenadores.find((e) => e.idPersona === persona.idPersona) ?? null,
      representante: representantes.find((r) => r.idPersona === persona.idPersona) ?? null,
    }));
  });

  readonly seleccionada = signal<PersonaConEstado | null>(null);
  readonly esNueva = signal(false);
  readonly mostrandoDetalle = computed(() => this.seleccionada() !== null || this.esNueva());

  /**
   * Los representantes del estudiante seleccionado, cruzados en el cliente
   * desde representados[] -- mismo criterio que el resto de la pantalla, sin
   * endpoint agregador nuevo. `relacion`/`contactoPrincipal` viven en el
   * vinculo, asi que se leen de la fila de representados que apunta a este
   * estudiante, no del representante.
   */
  readonly representantesDelEstudiante = computed(() => {
    const idEstudiante = this.seleccionada()?.estudiante?.idEstudiante;
    if (idEstudiante === undefined) return [];
    return this.representantes().flatMap((r) => {
      const vinculo = r.representados.find((e) => e.idEstudiante === idEstudiante);
      return vinculo
        ? [{ idRepresentante: r.idRepresentante, nombre: r.nombre, apellido: r.apellido,
             relacion: vinculo.relacion, contactoPrincipal: vinculo.contactoPrincipal }]
        : [];
    });
  });

  readonly representantesDisponibles = computed(() => {
    const yaVinculados = new Set(this.representantesDelEstudiante().map((v) => v.idRepresentante));
    return this.representantes().filter((r) => r.activo && !yaVinculados.has(r.idRepresentante));
  });

  cargarDatosIniciales(): void {
    this.cargarPersonas();
    this.servicio.categoriasActivas().subscribe({ next: (c) => this.categorias.set(c) });
    this.servicio.especialidadesActivas().subscribe({ next: (e) => this.especialidades.set(e) });
  }

  cargarPersonas(mantenerSeleccion = false): void {
    this.cargando.set(true);
    const idSeleccionado = this.seleccionada()?.persona?.idPersona ?? null;

    this.servicio.listarPersonas().subscribe({
      next: (pagina) => { this.personasBase.set(pagina.content); this.cargando.set(false); this.reaplicarSeleccion(mantenerSeleccion, idSeleccionado); },
      error: () => this.cargando.set(false),
    });
    this.servicio.listarUsuarios().subscribe({
      next: (pagina) => { this.usuarios.set(pagina.content); this.reaplicarSeleccion(mantenerSeleccion, idSeleccionado); },
      error: () => {},
    });
    this.servicio.listarEstudiantes().subscribe({
      next: (pagina) => { this.estudiantes.set(pagina.content); this.reaplicarSeleccion(mantenerSeleccion, idSeleccionado); },
      error: () => {},
    });
    this.servicio.listarEntrenadores().subscribe({
      next: (pagina) => { this.entrenadores.set(pagina.content); this.reaplicarSeleccion(mantenerSeleccion, idSeleccionado); },
      error: () => {},
    });
    this.servicio.listarRepresentantes().subscribe({
      next: (pagina) => { this.representantes.set(pagina.content); this.reaplicarSeleccion(mantenerSeleccion, idSeleccionado); },
      error: () => {},
    });
  }

  /** La seleccion actual debe reflejar los datos mas frescos a medida que cada lista llega, no solo al final. */
  private reaplicarSeleccion(mantenerSeleccion: boolean, idSeleccionado: number | null): void {
    if (!mantenerSeleccion || idSeleccionado === null) return;
    const actualizada = this.personas().find((p) => p.persona.idPersona === idSeleccionado);
    if (actualizada) this.seleccionada.set(actualizada);
  }

  seleccionar(p: PersonaConEstado): void {
    this.esNueva.set(false);
    this.seleccionada.set(p);
  }

  nuevaPersona(): void {
    this.seleccionada.set(null);
    this.esNueva.set(true);
  }

  buscarPorIdPersona(idPersona: number): PersonaConEstado | undefined {
    return this.personas().find((x) => x.persona.idPersona === idPersona);
  }
}
