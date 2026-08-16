import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PersonasStateService } from './personas-state.service';

/**
 * Panel izquierdo del maestro-detalle: busqueda + lista de personas +
 * boton de alta. Uno de los tres componentes hijos en que se dividio
 * personas-admin.component.ts (R-05, informe de evaluacion de calidad).
 */
@Component({
  selector: 'app-personas-lista',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="card panel-lista">
      <input class="buscador" type="search" placeholder="Buscar por nombre o cédula…"
             [ngModel]="busqueda()" (ngModelChange)="busqueda.set($event)" name="busqueda" />
      <button class="btn btn--primary btn--block" type="button" (click)="state.nuevaPersona()">+ Nueva persona</button>

      @if (state.cargando()) {
        <p class="aviso">Cargando…</p>
      } @else {
        <div class="lista-personas">
          @for (p of personasFiltradas(); track p.persona.idPersona) {
            <button type="button" class="fila-persona" [class.fila-persona--activa]="state.seleccionada()?.persona?.idPersona === p.persona.idPersona"
                    (click)="state.seleccionar(p)">
              <span class="nombre-persona">{{ p.persona.nombre }} {{ p.persona.apellido }}</span>
              <span class="cedula-persona">{{ p.persona.cedula }}</span>
              <span class="badges-persona">
                @if (p.usuario) { <span class="badge badge--info">{{ p.usuario.roles.length ? p.usuario.roles[0] : 'sin rol' }}</span> }
                @if (p.estudiante) { <span class="badge badge--success">Estudiante</span> }
                @if (p.entrenador) { <span class="badge badge--success">Entrenador</span> }
                @if (p.representante) { <span class="badge badge--success">Representante</span> }
              </span>
            </button>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .panel-lista { padding: 1rem; display: flex; flex-direction: column; gap: .75rem; position: sticky; top: 1rem; max-height: calc(100vh - 2rem); overflow-y: auto; }
    .lista-personas { display: flex; flex-direction: column; gap: .2rem; }
    .fila-persona {
      display: flex; flex-direction: column; align-items: flex-start; gap: .2rem; padding: .55rem .6rem;
      border: none; background: none; border-radius: var(--radius-sm); cursor: pointer; text-align: left; width: 100%;
    }
    .fila-persona:hover { background: var(--color-border-light); }
    .fila-persona--activa { background: var(--color-primary-50); }
    .nombre-persona { font-weight: 600; font-size: .88rem; }
    .cedula-persona { font-size: .78rem; color: var(--color-text-faint); }
  `],
})
export class PersonasListaComponent {
  readonly state = inject(PersonasStateService);

  readonly busqueda = signal('');

  readonly personasFiltradas = computed(() => {
    const q = this.busqueda().trim().toLowerCase();
    if (!q) return this.state.personas();
    return this.state.personas().filter((p) =>
      `${p.persona.nombre} ${p.persona.apellido}`.toLowerCase().includes(q) || p.persona.cedula.includes(q));
  });
}
