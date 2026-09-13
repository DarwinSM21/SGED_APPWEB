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
            @for (u of usuariosFiltrados(); track u.userId) {
              <div class="fila-envoltura">
                <button type="button" class="fila-gestion" (click)="irAPersona.emit(u.personId)">
                  <span class="col-principal">{{ u.personName }} {{ u.personLastName }}</span>
                  <span class="col-secundaria">{{ u.username }}</span>
                  <span class="badges-persona">
                    @for (r of u.roles; track r) { <span class="badge badge--info">{{ r }}</span> }
                  </span>
                  <span class="col-secundaria">{{ u.generalStatusName }}</span>
                  <span class="col-secundaria">{{ u.lastAccess ? fechaHora(u.lastAccess) : 'sin acceso aún' }}</span>
                  <span class="badge" [class.badge--success]="u.active" [class.badge--danger]="!u.active">{{ u.active ? 'Activo' : 'Inactivo' }}</span>
                </button>
                @if (!u.active) {
                  <app-confirmar-accion etiqueta="Reactivar" [peligrosa]="false"
                                        pregunta="Va a poder volver a iniciar sesión."
                                        textoConfirmar="Sí, reactivar" enCurso="Reactivando…"
                                        [ocupado]="reactivando()"
                                        (confirmado)="reactivarUsuario(u.userId)" />
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
            @for (e of estudiantesFiltrados(); track e.studentId) {
              <div class="fila-envoltura">
                <button type="button" class="fila-gestion" (click)="irAPersona.emit(e.personId)">
                  <span class="col-principal">{{ e.personName }} {{ e.personLastName }}</span>
                  <span class="col-secundaria">{{ e.studentCode }}</span>
                  <span class="col-secundaria">{{ e.categoryName }}</span>
                  <span class="col-secundaria">{{ e.enrollmentDate | date:'shortDate' }}</span>
                  <span class="badge" [class.badge--success]="e.active" [class.badge--danger]="!e.active">{{ e.active ? 'Activo' : 'Inactivo' }}</span>
                </button>
                @if (!e.active) {
                  <app-confirmar-accion etiqueta="Reactivar" [peligrosa]="false"
                                        pregunta="Vuelve a contar para convocatorias y asistencia."
                                        textoConfirmar="Sí, reactivar" enCurso="Reactivando…"
                                        [ocupado]="reactivando()"
                                        (confirmado)="reactivarEstudiante(e.studentId)" />
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
            @for (ent of entrenadoresFiltrados(); track ent.coachId) {
              <div class="fila-envoltura">
                <button type="button" class="fila-gestion" (click)="irAPersona.emit(ent.personId)">
                  <span class="col-principal">{{ ent.name }} {{ ent.lastName }}</span>
                  <span class="col-secundaria">{{ ent.specialtyName || 'sin especialidad' }}</span>
                  <span class="col-secundaria">{{ ent.yearsOfExperience ?? 0 }} años</span>
                  <span class="col-secundaria">{{ ent.username }}</span>
                  <span class="badge" [class.badge--success]="ent.active" [class.badge--danger]="!ent.active">{{ ent.active ? 'Activo' : 'Inactivo' }}</span>
                </button>
                @if (!ent.active) {
                  <app-confirmar-accion etiqueta="Reactivar" [peligrosa]="false"
                                        pregunta="Vuelve a poder tener horarios y sesiones."
                                        textoConfirmar="Sí, reactivar" enCurso="Reactivando…"
                                        [ocupado]="reactivando()"
                                        (confirmado)="reactivarEntrenador(ent.coachId)" />
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
            @for (r of representantesFiltrados(); track r.guardianId) {
              <div class="fila-envoltura">
                <button type="button" class="fila-gestion" (click)="irAPersona.emit(r.personId)">
                  <span class="col-principal">{{ r.name }} {{ r.lastName }}</span>
                  <span class="col-secundaria">{{ r.relationship || 'sin parentesco' }}</span>
                  <span class="col-secundaria">{{ r.contactPhone || 'sin teléfono' }}</span>
                  <span class="col-secundaria">{{ r.wards.length }} representado{{ r.wards.length === 1 ? '' : 's' }}</span>
                  <span class="badge" [class.badge--success]="r.active" [class.badge--danger]="!r.active">{{ r.active ? 'Activo' : 'Inactivo' }}</span>
                </button>
                @if (!r.active) {
                  <app-confirmar-accion etiqueta="Reactivar" [peligrosa]="false"
                                        pregunta="Vuelve a ver los informes de sus representados."
                                        textoConfirmar="Sí, reactivar" enCurso="Reactivando…"
                                        [ocupado]="reactivando()"
                                        (confirmado)="reactivarRepresentante(r.guardianId)" />
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
      (conInactivos || u.active) &&
      (rol === 'TODOS' || u.roles.includes(rol)) &&
      (!q || `${u.personName} ${u.personLastName}`.toLowerCase().includes(q) || u.username.toLowerCase().includes(q)));
  });

  readonly estudiantesFiltrados = computed(() => {
    const q = this.busquedaEstudiantes().trim().toLowerCase();
    const conInactivos = this.mostrarInactivosEstudiantes();
    return this.state.students().filter((e) =>
      (conInactivos || e.active) &&
      (!q || `${e.personName} ${e.personLastName}`.toLowerCase().includes(q) || e.studentCode.toLowerCase().includes(q)));
  });

  readonly entrenadoresFiltrados = computed(() => {
    const q = this.busquedaEntrenadores().trim().toLowerCase();
    const conInactivos = this.mostrarInactivosEntrenadores();
    return this.state.entrenadores().filter((ent) =>
      (conInactivos || ent.active) &&
      (!q || `${ent.name} ${ent.lastName}`.toLowerCase().includes(q) || (ent.specialtyName ?? '').toLowerCase().includes(q)));
  });

  readonly representantesFiltrados = computed(() => {
    const q = this.busquedaRepresentantes().trim().toLowerCase();
    const conInactivos = this.mostrarInactivosRepresentantes();
    return this.state.representantes().filter((r) =>
      (conInactivos || r.active) &&
      (!q || `${r.name} ${r.lastName}`.toLowerCase().includes(q) || (r.relationship ?? '').toLowerCase().includes(q)));
  });
}
