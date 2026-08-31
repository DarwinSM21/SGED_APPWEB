import { Injectable, computed, inject, signal } from '@angular/core';
import { PersonasService } from './personas.service';
import {
  CategoriaOpcion, EntrenadorResponse, EspecialidadOpcion, EstudianteResponse, PersonaConEstado, PersonaResponse,
  PosicionOpcion, RepresentanteResponse, UsuarioResponse,
} from './personas.models';

@Injectable()
export class PersonasStateService {
  private readonly servicio = inject(PersonasService);

  readonly personasBase = signal<PersonaResponse[]>([]);
  readonly usuarios = signal<UsuarioResponse[]>([]);
  readonly estudiantes = signal<EstudianteResponse[]>([]);
  readonly entrenadores = signal<EntrenadorResponse[]>([]);
  readonly representantes = signal<RepresentanteResponse[]>([]);
  readonly categorias = signal<CategoriaOpcion[]>([]);
  readonly especialidades = signal<EspecialidadOpcion[]>([]);
  readonly posiciones = signal<PosicionOpcion[]>([]);
  readonly cargando = signal(true);

  readonly personas = computed<PersonaConEstado[]>(() => {
    const usuarios = this.usuarios(), estudiantes = this.estudiantes(),
      entrenadores = this.entrenadores(), representantes = this.representantes();
    return this.personasBase().map((persona) => ({
      persona,
      usuario: usuarios.find((u) => u.idPersona === persona.idPersona) ?? null,
      estudiante: estudiantes.find((e) => e.idPersona === persona.idPersona && e.activo) ?? null,
      entrenador: entrenadores.find((e) => e.idPersona === persona.idPersona) ?? null,
      representante: representantes.find((r) => r.idPersona === persona.idPersona) ?? null,
    }));
  });

  readonly seleccionada = signal<PersonaConEstado | null>(null);
  readonly esNueva = signal(false);
  readonly mostrandoDetalle = computed(() => this.seleccionada() !== null || this.esNueva());

  readonly representantesDelEstudiante = computed(() => {
    const idEstudiante = this.seleccionada()?.estudiante?.idEstudiante;
    if (idEstudiante === undefined) return [];
    return this.representantes().filter((r) => r.activo).flatMap((r) => {
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
    this.servicio.posicionesActivas().subscribe({ next: (p) => this.posiciones.set(p) });
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
