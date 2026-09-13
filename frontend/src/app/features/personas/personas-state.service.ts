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
  readonly students = signal<EstudianteResponse[]>([]);
  readonly entrenadores = signal<EntrenadorResponse[]>([]);
  readonly representantes = signal<RepresentanteResponse[]>([]);
  readonly categorias = signal<CategoriaOpcion[]>([]);
  readonly especialidades = signal<EspecialidadOpcion[]>([]);
  readonly posiciones = signal<PosicionOpcion[]>([]);
  readonly cargando = signal(true);

  readonly personas = computed<PersonaConEstado[]>(() => {
    const usuarios = this.usuarios(), estudiantes = this.students(),
      entrenadores = this.entrenadores(), representantes = this.representantes();
    return this.personasBase().map((persona) => ({
      persona,
      user: usuarios.find((u) => u.personId === persona.personId) ?? null,
      student: estudiantes.find((e) => e.personId === persona.personId && e.active) ?? null,
      coach: entrenadores.find((e) => e.personId === persona.personId) ?? null,
      representante: representantes.find((r) => r.personId === persona.personId) ?? null,
    }));
  });

  readonly seleccionada = signal<PersonaConEstado | null>(null);
  readonly esNueva = signal(false);
  readonly mostrandoDetalle = computed(() => this.seleccionada() !== null || this.esNueva());

  readonly representantesDelEstudiante = computed(() => {
    const idEstudiante = this.seleccionada()?.student?.studentId;
    if (idEstudiante === undefined) return [];
    return this.representantes().filter((r) => r.active).flatMap((r) => {
      const vinculo = r.wards.find((e) => e.studentId === idEstudiante);
      return vinculo
        ? [{ guardianId: r.guardianId, name: r.name, lastName: r.lastName,
             relationship: vinculo.relationship, primaryContact: vinculo.primaryContact }]
        : [];
    });
  });

  readonly representantesDisponibles = computed(() => {
    const yaVinculados = new Set(this.representantesDelEstudiante().map((v) => v.guardianId));
    return this.representantes().filter((r) => r.active && !yaVinculados.has(r.guardianId));
  });

  cargarDatosIniciales(): void {
    this.cargarPersonas();
    this.servicio.categoriasActivas().subscribe({ next: (c) => this.categorias.set(c) });
    this.servicio.especialidadesActivas().subscribe({ next: (e) => this.especialidades.set(e) });
    this.servicio.posicionesActivas().subscribe({ next: (p) => this.posiciones.set(p) });
  }

  cargarPersonas(mantenerSeleccion = false): void {
    this.cargando.set(true);
    const idSeleccionado = this.seleccionada()?.persona?.personId ?? null;

    this.servicio.listarPersonas().subscribe({
      next: (pagina) => { this.personasBase.set(pagina.content); this.cargando.set(false); this.reaplicarSeleccion(mantenerSeleccion, idSeleccionado); },
      error: () => this.cargando.set(false),
    });
    this.servicio.listarUsuarios().subscribe({
      next: (pagina) => { this.usuarios.set(pagina.content); this.reaplicarSeleccion(mantenerSeleccion, idSeleccionado); },
      error: () => {},
    });
    this.servicio.listarEstudiantes().subscribe({
      next: (pagina) => { this.students.set(pagina.content); this.reaplicarSeleccion(mantenerSeleccion, idSeleccionado); },
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
    const actualizada = this.personas().find((p) => p.persona.personId === idSeleccionado);
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
    return this.personas().find((x) => x.persona.personId === idPersona);
  }
}
