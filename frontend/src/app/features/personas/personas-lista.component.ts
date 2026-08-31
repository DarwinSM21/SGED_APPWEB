import { Component, computed, inject, signal } from '@angular/core';
import { CargandoComponent } from '../../core/cargando.component';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PersonasStateService } from './personas-state.service';
import { PersonaConEstado } from './personas.models';

@Component({
  selector: 'app-personas-lista',
  standalone: true,
  imports: [CommonModule, FormsModule, CargandoComponent],
  template: `
    <div class="card panel-lista">
      <input class="buscador" type="search" placeholder="Buscar por nombre o cédula…"
             [ngModel]="busqueda()" (ngModelChange)="busqueda.set($event)" name="busqueda" />
      <button class="btn btn--primary btn--block" type="button" (click)="state.nuevaPersona()">+ Nueva persona</button>

      @if (state.cargando()) {
        <app-cargando />
      } @else {
        <div class="lista-personas">
          @for (p of personasFiltradas(); track p.persona.idPersona) {
            <button type="button" class="fila-persona" [class.fila-persona--activa]="state.seleccionada()?.persona?.idPersona === p.persona.idPersona"
                    (click)="state.seleccionar(p)">
              <span class="nombre-persona">{{ p.persona.nombre }} {{ p.persona.apellido }}</span>
              <span class="cedula-persona">{{ p.persona.cedula }}</span>
              <span class="badges-persona">
                @if (rolAparte(p); as rol) { <span class="badge badge--info">{{ rol }}</span> }
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
    @media (max-width: 800px) {
      .panel-lista { position: static; max-height: 45vh; }
    }
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

  rolAparte(p: PersonaConEstado): string | null {
    const rol = p.usuario?.roles[0] ?? null;
    if (!p.usuario) return null;
    if (!rol) return 'sin rol';
    const ficha = p.estudiante ? 'ESTUDIANTE'
      : p.entrenador ? 'ENTRENADOR'
      : p.representante ? 'REPRESENTANTE'
      : null;
    return rol === ficha ? null : rol;
  }

  readonly personasFiltradas = computed(() => {
    const q = this.busqueda().trim().toLowerCase();
    if (!q) return this.state.personas();
    return this.state.personas().filter((p) =>
      `${p.persona.nombre} ${p.persona.apellido}`.toLowerCase().includes(q) || p.persona.cedula.includes(q));
  });
}
