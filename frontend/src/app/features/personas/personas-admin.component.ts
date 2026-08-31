import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PersonasStateService } from './personas-state.service';
import { PersonasListaComponent } from './personas-lista.component';
import { PersonaDetalleComponent } from './persona-detalle.component';
import { PersonasGestionComponent, TabGestion } from './personas-gestion.component';

type Tab = 'personas' | TabGestion;
const ETIQUETA_TAB: Record<Tab, string> = {
  personas: 'Personas', usuarios: 'Usuarios', estudiantes: 'Estudiantes',
  entrenadores: 'Entrenadores', representantes: 'Representantes',
};

@Component({
  selector: 'app-personas-admin',
  standalone: true,
  imports: [CommonModule, PersonasListaComponent, PersonaDetalleComponent, PersonasGestionComponent],
  providers: [PersonasStateService],
  template: `
    <div class="tabs">
      @for (t of tabs; track t) {
        <button type="button" class="tab" [class.tab--activo]="tabActiva() === t" (click)="tabActiva.set(t)">
          {{ etiquetaTab(t) }}
        </button>
      }
    </div>

    @if (tabActiva() === 'personas') {
      <div class="maestro-detalle">
        <app-personas-lista />
        <app-persona-detalle />
      </div>
    } @else if (tabGestionActiva(); as tg) {
      <app-personas-gestion [tabActiva]="tg" (irAPersona)="irAPersona($event)" />
    }
  `,
  styles: [`
    .tabs { display: flex; gap: .4rem; border-bottom: 1px solid var(--color-border-light); padding: 0 1.25rem; max-width: 1100px; margin: 0 auto; }
    .tab {
      border: none; background: none; padding: .75rem .9rem; font-size: .87rem; font-weight: 600;
      color: var(--color-text-muted); cursor: pointer; border-bottom: 2px solid transparent;
    }
    .tab--activo { color: var(--color-primary-700); border-bottom-color: var(--color-primary-500); }
    .maestro-detalle { display: grid; grid-template-columns: 320px 1fr; gap: 1.25rem; max-width: 1100px; margin: 0 auto; padding: 1.5rem 1.25rem 3rem; align-items: start; }
    @media (max-width: 800px) { .maestro-detalle { grid-template-columns: 1fr; } }
  `],
})
export class PersonasAdminComponent implements OnInit {
  private readonly state = inject(PersonasStateService);

  readonly tabs: Tab[] = ['personas', 'usuarios', 'estudiantes', 'entrenadores', 'representantes'];
  readonly tabActiva = signal<Tab>('personas');
  readonly tabGestionActiva = computed<TabGestion | null>(() => {
    const t = this.tabActiva();
    return t === 'personas' ? null : t;
  });
  etiquetaTab(t: Tab): string { return ETIQUETA_TAB[t]; }

  ngOnInit(): void {
    this.state.cargarDatosIniciales();
  }

  irAPersona(idPersona: number): void {
    const p = this.state.buscarPorIdPersona(idPersona);
    if (p) { this.tabActiva.set('personas'); this.state.seleccionar(p); }
  }
}
