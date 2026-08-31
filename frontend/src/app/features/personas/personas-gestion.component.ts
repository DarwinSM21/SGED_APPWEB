import { Component, computed, inject, input, output, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PersonasStateService } from './personas-state.service';
import { RolUsuario, ROLES_USUARIO } from './personas.models';
import { fechaHoraCorta } from '../../core/formato-fecha';
import { Observable } from 'rxjs';
import { PersonasService } from './personas.service';
import { mensajeDeError } from '../../core/mensaje-error';
import { ConfirmarAccionComponent } from '../../core/confirmar-accion.component';

export type TabGestion = 'usuarios' | 'estudiantes' | 'entrenadores' | 'representantes';

@Component({
  selector: 'app-personas-gestion',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, ConfirmarAccionComponent],
  template: `
    @if (tabActiva() === 'usuarios') {
      <div class="card panel-gestion">
        <div class="barra-filtros">
          <input class="buscador" type="search" placeholder="Buscar por nombre o usuario…"
                 [ngModel]="busquedaUsuarios()" (ngModelChange)="busquedaUsuarios.set($event)" name="busquedaUsuarios" />
          <select class="filtro-rol" [ngModel]="filtroRolUsuarios()" (ngModelChange)="filtroRolUsuarios.set($event)" name="filtroRolUsuarios">
            <option value="TODOS">Todos los roles</option>
            @for (r of roles; track r) { <option [value]="r">{{ r }}</option> }
          </select>
          <label class="toggle-inactivos">
            <input type="checkbox" [ngModel]="mostrarInactivosUsuarios()" (ngModelChange)="mostrarInactivosUsuarios.set($event)" name="mostrarInactivosUsuarios" />
            Mostrar inactivos
          </label>
        </div>
        @if (errorReactivar()) { <div class="alert alert--danger" role="alert">{{ errorReactivar() }}</div> }
        @if (usuariosFiltrados().length === 0) {
          <p class="aviso">No hay usuarios que coincidan.</p>
        } @else {
          <div class="lista-gestion">
            @for (u of usuariosFiltrados(); track u.idUsuario) {
              <div class="fila-envoltura">
                <button type="button" class="fila-gestion" (click)="irAPersona.emit(u.idPersona)">
                  <span class="col-principal">{{ u.nombrePersona }} {{ u.apellidoPersona }}</span>
                  <span class="col-secundaria">{{ u.username }}</span>
                  <span class="badges-persona">
                    @for (r of u.roles; track r) { <span class="badge badge--info">{{ r }}</span> }
                  </span>
                  <span class="col-secundaria">{{ u.estadoGeneralNombre }}</span>
                  <span class="col-secundaria">{{ u.ultimoAcceso ? fechaHora(u.ultimoAcceso) : 'sin acceso aún' }}</span>
                  <span class="badge" [class.badge--success]="u.activo" [class.badge--danger]="!u.activo">{{ u.activo ? 'Activo' : 'Inactivo' }}</span>
                </button>
                @if (!u.activo) {
                  <app-confirmar-accion etiqueta="Reactivar" [peligrosa]="false"
                                        pregunta="Va a poder volver a iniciar sesión."
                                        textoConfirmar="Sí, reactivar" enCurso="Reactivando…"
                                        [ocupado]="reactivando()"
                                        (confirmado)="reactivarUsuario(u.idUsuario)" />
                }
              </div>
            }
          </div>
        }
      </div>
    }

    @if (tabActiva() === 'estudiantes') {
      <div class="card panel-gestion">
        <div class="barra-filtros">
          <input class="buscador" type="search" placeholder="Buscar por nombre o código…"
                 [ngModel]="busquedaEstudiantes()" (ngModelChange)="busquedaEstudiantes.set($event)" name="busquedaEstudiantes" />
          <label class="toggle-inactivos">
            <input type="checkbox" [ngModel]="mostrarInactivosEstudiantes()" (ngModelChange)="mostrarInactivosEstudiantes.set($event)" name="mostrarInactivosEstudiantes" />
            Mostrar inactivos
          </label>
        </div>
        @if (errorReactivar()) { <div class="alert alert--danger" role="alert">{{ errorReactivar() }}</div> }
        @if (estudiantesFiltrados().length === 0) {
          <p class="aviso">No hay estudiantes que coincidan.</p>
        } @else {
          <div class="lista-gestion">
            @for (e of estudiantesFiltrados(); track e.idEstudiante) {
              <div class="fila-envoltura">
                <button type="button" class="fila-gestion" (click)="irAPersona.emit(e.idPersona)">
                  <span class="col-principal">{{ e.nombrePersona }} {{ e.apellidoPersona }}</span>
                  <span class="col-secundaria">{{ e.codigoEstudiante }}</span>
                  <span class="col-secundaria">{{ e.nombreCategoria }}</span>
                  <span class="col-secundaria">{{ e.fechaIngreso | date:'shortDate' }}</span>
                  <span class="badge" [class.badge--success]="e.activo" [class.badge--danger]="!e.activo">{{ e.activo ? 'Activo' : 'Inactivo' }}</span>
                </button>
                @if (!e.activo) {
                  <app-confirmar-accion etiqueta="Reactivar" [peligrosa]="false"
                                        pregunta="Vuelve a contar para convocatorias y asistencia."
                                        textoConfirmar="Sí, reactivar" enCurso="Reactivando…"
                                        [ocupado]="reactivando()"
                                        (confirmado)="reactivarEstudiante(e.idEstudiante)" />
                }
              </div>
            }
          </div>
        }
      </div>
    }

    @if (tabActiva() === 'entrenadores') {
      <div class="card panel-gestion">
        <div class="barra-filtros">
          <input class="buscador" type="search" placeholder="Buscar por nombre o especialidad…"
                 [ngModel]="busquedaEntrenadores()" (ngModelChange)="busquedaEntrenadores.set($event)" name="busquedaEntrenadores" />
          <label class="toggle-inactivos">
            <input type="checkbox" [ngModel]="mostrarInactivosEntrenadores()" (ngModelChange)="mostrarInactivosEntrenadores.set($event)" name="mostrarInactivosEntrenadores" />
            Mostrar inactivos
          </label>
        </div>
        @if (errorReactivar()) { <div class="alert alert--danger" role="alert">{{ errorReactivar() }}</div> }
        @if (entrenadoresFiltrados().length === 0) {
          <p class="aviso">No hay entrenadores que coincidan.</p>
        } @else {
          <div class="lista-gestion">
            @for (ent of entrenadoresFiltrados(); track ent.idEntrenador) {
              <div class="fila-envoltura">
                <button type="button" class="fila-gestion" (click)="irAPersona.emit(ent.idPersona)">
                  <span class="col-principal">{{ ent.nombre }} {{ ent.apellido }}</span>
                  <span class="col-secundaria">{{ ent.nombreEspecialidad || 'sin especialidad' }}</span>
                  <span class="col-secundaria">{{ ent.experienciaAnios ?? 0 }} años</span>
                  <span class="col-secundaria">{{ ent.username }}</span>
                  <span class="badge" [class.badge--success]="ent.activo" [class.badge--danger]="!ent.activo">{{ ent.activo ? 'Activo' : 'Inactivo' }}</span>
                </button>
                @if (!ent.activo) {
                  <app-confirmar-accion etiqueta="Reactivar" [peligrosa]="false"
                                        pregunta="Vuelve a poder tener horarios y sesiones."
                                        textoConfirmar="Sí, reactivar" enCurso="Reactivando…"
                                        [ocupado]="reactivando()"
                                        (confirmado)="reactivarEntrenador(ent.idEntrenador)" />
                }
              </div>
            }
          </div>
        }
      </div>
    }

    @if (tabActiva() === 'representantes') {
      <div class="card panel-gestion">
        <div class="barra-filtros">
          <input class="buscador" type="search" placeholder="Buscar por nombre o parentesco…"
                 [ngModel]="busquedaRepresentantes()" (ngModelChange)="busquedaRepresentantes.set($event)" name="busquedaRepresentantes" />
          <label class="toggle-inactivos">
            <input type="checkbox" [ngModel]="mostrarInactivosRepresentantes()" (ngModelChange)="mostrarInactivosRepresentantes.set($event)" name="mostrarInactivosRepresentantes" />
            Mostrar inactivos
          </label>
        </div>
        @if (errorReactivar()) { <div class="alert alert--danger" role="alert">{{ errorReactivar() }}</div> }
        @if (representantesFiltrados().length === 0) {
          <p class="aviso">No hay representantes que coincidan.</p>
        } @else {
          <div class="lista-gestion">
            @for (r of representantesFiltrados(); track r.idRepresentante) {
              <div class="fila-envoltura">
                <button type="button" class="fila-gestion" (click)="irAPersona.emit(r.idPersona)">
                  <span class="col-principal">{{ r.nombre }} {{ r.apellido }}</span>
                  <span class="col-secundaria">{{ r.parentesco || 'sin parentesco' }}</span>
                  <span class="col-secundaria">{{ r.telefonoContacto || 'sin teléfono' }}</span>
                  <span class="col-secundaria">{{ r.representados.length }} representado{{ r.representados.length === 1 ? '' : 's' }}</span>
                  <span class="badge" [class.badge--success]="r.activo" [class.badge--danger]="!r.activo">{{ r.activo ? 'Activo' : 'Inactivo' }}</span>
                </button>
                @if (!r.activo) {
                  <app-confirmar-accion etiqueta="Reactivar" [peligrosa]="false"
                                        pregunta="Vuelve a ver los informes de sus representados."
                                        textoConfirmar="Sí, reactivar" enCurso="Reactivando…"
                                        [ocupado]="reactivando()"
                                        (confirmado)="reactivarRepresentante(r.idRepresentante)" />
                }
              </div>
            }
          </div>
        }
      </div>
    }
  `,
  styles: [`
    .panel-gestion { max-width: 1100px; margin: 1.25rem auto 3rem; padding: 1.25rem; display: flex; flex-direction: column; gap: .9rem; }
    .barra-filtros { display: flex; gap: .6rem; flex-wrap: wrap; align-items: center; }
    .barra-filtros .buscador { flex: 1; min-width: 220px; }
    .filtro-rol { padding: .6rem .75rem; border: 1px solid var(--color-border); border-radius: var(--radius-sm); font-size: .85rem; background: transparent; color: var(--color-text); }
    .toggle-inactivos { display: flex; align-items: center; gap: .4rem; font-size: .82rem; color: var(--color-text-muted); white-space: nowrap; }
    .lista-gestion { display: flex; flex-direction: column; gap: .1rem; }
    .fila-gestion {
      display: grid; grid-template-columns: 1.4fr 1fr 1fr 1fr 1fr auto; align-items: center; gap: .75rem;
      padding: .6rem .5rem; border: none; border-bottom: 1px solid var(--color-border-light); background: none;
      cursor: pointer; text-align: left; width: 100%; font-size: .85rem;
    }
    .fila-envoltura { display: flex; align-items: center; gap: .5rem; border-bottom: 1px solid var(--color-border-light); }
    .fila-envoltura:last-child { border-bottom: none; }
    .fila-envoltura .fila-gestion { border-bottom: none; }
    .fila-gestion:last-child { border-bottom: none; }
    .fila-gestion:hover { background: var(--color-border-light); }
    @media (max-width: 800px) { .fila-gestion { grid-template-columns: 1fr 1fr; } }
  `],
})
export class PersonasGestionComponent {
  readonly fechaHora = fechaHoraCorta;

  readonly state = inject(PersonasStateService);
  private readonly servicio = inject(PersonasService);

  readonly reactivando = signal(false);
  readonly errorReactivar = signal('');

  readonly tabActiva = input.required<TabGestion>();
  readonly irAPersona = output<number>();

  readonly roles = ROLES_USUARIO;

  reactivarUsuario(id: number): void { this.correr(this.servicio.reactivarUsuario(id)); }

  reactivarEstudiante(id: number): void { this.correr(this.servicio.reactivarEstudiante(id)); }

  reactivarEntrenador(id: number): void { this.correr(this.servicio.reactivarEntrenador(id)); }

  reactivarRepresentante(id: number): void { this.correr(this.servicio.reactivarRepresentante(id)); }

  private correr(peticion: Observable<unknown>): void {
    this.reactivando.set(true);
    this.errorReactivar.set('');
    peticion.subscribe({
      next: () => { this.reactivando.set(false); this.state.cargarPersonas(true); },
      error: (err) => { this.reactivando.set(false); this.errorReactivar.set(mensajeDeError(err)); },
    });
  }

  readonly busquedaUsuarios = signal('');
  readonly filtroRolUsuarios = signal<RolUsuario | 'TODOS'>('TODOS');
  readonly mostrarInactivosUsuarios = signal(false);

  readonly busquedaEstudiantes = signal('');
  readonly mostrarInactivosEstudiantes = signal(false);

  readonly busquedaEntrenadores = signal('');
  readonly mostrarInactivosEntrenadores = signal(false);

  readonly busquedaRepresentantes = signal('');
  readonly mostrarInactivosRepresentantes = signal(false);

  readonly usuariosFiltrados = computed(() => {
    const q = this.busquedaUsuarios().trim().toLowerCase();
    const rol = this.filtroRolUsuarios();
    const conInactivos = this.mostrarInactivosUsuarios();
    return this.state.usuarios().filter((u) =>
      (conInactivos || u.activo) &&
      (rol === 'TODOS' || u.roles.includes(rol)) &&
      (!q || `${u.nombrePersona} ${u.apellidoPersona}`.toLowerCase().includes(q) || u.username.toLowerCase().includes(q)));
  });

  readonly estudiantesFiltrados = computed(() => {
    const q = this.busquedaEstudiantes().trim().toLowerCase();
    const conInactivos = this.mostrarInactivosEstudiantes();
    return this.state.estudiantes().filter((e) =>
      (conInactivos || e.activo) &&
      (!q || `${e.nombrePersona} ${e.apellidoPersona}`.toLowerCase().includes(q) || e.codigoEstudiante.toLowerCase().includes(q)));
  });

  readonly entrenadoresFiltrados = computed(() => {
    const q = this.busquedaEntrenadores().trim().toLowerCase();
    const conInactivos = this.mostrarInactivosEntrenadores();
    return this.state.entrenadores().filter((ent) =>
      (conInactivos || ent.activo) &&
      (!q || `${ent.nombre} ${ent.apellido}`.toLowerCase().includes(q) || (ent.nombreEspecialidad ?? '').toLowerCase().includes(q)));
  });

  readonly representantesFiltrados = computed(() => {
    const q = this.busquedaRepresentantes().trim().toLowerCase();
    const conInactivos = this.mostrarInactivosRepresentantes();
    return this.state.representantes().filter((r) =>
      (conInactivos || r.activo) &&
      (!q || `${r.nombre} ${r.apellido}`.toLowerCase().includes(q) || (r.parentesco ?? '').toLowerCase().includes(q)));
  });
}
