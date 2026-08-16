import { Component, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PersonasService, ESTADO_GENERAL_ACTIVO } from './personas.service';
import { PersonasStateService } from './personas-state.service';

/**
 * Seccion "Ficha de estudiante" del panel de detalle: alta de la ficha y
 * gestion de sus vinculos con representantes. Uno de los componentes en
 * que se dividio personas-admin.component.ts (R-05, informe de evaluacion
 * de calidad).
 */
@Component({
  selector: 'app-ficha-estudiante',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="bloque bloque--separado">
      <h3 class="subtitulo-seccion">Ficha de estudiante</h3>
      @if (persona()?.estudiante; as e) {
        <p class="resumen-seccion">{{ e.codigoEstudiante }} · {{ e.nombreCategoria }} · {{ e.activo ? 'activo' : 'inactivo' }}</p>

        <h4 class="subtitulo-menor">Representantes</h4>
        @if (state.representantesDelEstudiante().length === 0) {
          <p class="aviso">Este estudiante todavía no tiene representantes asignados.</p>
        } @else {
          <div class="lista-vinculos">
            @for (v of state.representantesDelEstudiante(); track v.idRepresentante) {
              <div class="fila-vinculo">
                <span class="col-principal">{{ v.nombre }} {{ v.apellido }}</span>
                <span class="col-secundaria">{{ v.relacion || 'sin relación' }}</span>
                @if (v.contactoPrincipal) { <span class="badge badge--info">Contacto principal</span> }
                <button class="btn btn--ghost btn--sm" type="button"
                        (click)="desvincularRepresentante(v.idRepresentante, e.idEstudiante)">Desvincular</button>
              </div>
            }
          </div>
        }

        @if (state.representantesDisponibles().length > 0) {
          <div class="fila-2">
            <label class="field" for="v-representante"><span class="field__label">Agregar representante</span>
              <span class="field__control">
                <select id="v-representante" [(ngModel)]="formVinculo.idRepresentante" name="v-representante">
                  <option [ngValue]="null" disabled>Selecciona…</option>
                  @for (r of state.representantesDisponibles(); track r.idRepresentante) {
                    <option [ngValue]="r.idRepresentante">{{ r.nombre }} {{ r.apellido }}</option>
                  }
                </select>
              </span></label>
            <label class="field" for="v-relacion"><span class="field__label">Relación</span>
              <span class="field__control"><input id="v-relacion" [(ngModel)]="formVinculo.relacion" name="v-relacion" placeholder="Madre, padre, tutor…" /></span></label>
          </div>
          <label class="toggle-inactivos">
            <input type="checkbox" [(ngModel)]="formVinculo.contactoPrincipal" name="v-principal" />
            Contacto principal
          </label>
          @if (errorVinculo()) { <div class="alert alert--danger" role="alert">{{ errorVinculo() }}</div> }
          <div class="acciones">
            <button class="btn btn--primary btn--sm" type="button" [disabled]="guardandoVinculo() || formVinculo.idRepresentante === null"
                    (click)="vincularRepresentante(e.idEstudiante)">
              @if (guardandoVinculo()) { <span class="spinner"></span> Vinculando… } @else { Vincular }
            </button>
          </div>
        } @else if (state.representantes().length === 0) {
          <p class="aviso">No hay representantes registrados todavía.</p>
        }
      } @else {
        <div class="fila-2">
          <label class="field" for="e-categoria"><span class="field__label">Categoría</span>
            <span class="field__control">
              <select id="e-categoria" [(ngModel)]="formEstudiante.idCategoria" name="e-categoria">
                <option [ngValue]="null" disabled>Selecciona…</option>
                @for (c of state.categorias(); track c.idCategoria) { <option [ngValue]="c.idCategoria">{{ c.nombre }}</option> }
              </select>
            </span></label>
          <label class="field" for="e-codigo"><span class="field__label">Código</span>
            <span class="field__control"><input id="e-codigo" [(ngModel)]="formEstudiante.codigoEstudiante" name="e-codigo" /></span></label>
        </div>
        <div class="fila-2">
          <label class="field" for="e-ingreso"><span class="field__label">Fecha de ingreso</span>
            <span class="field__control"><input id="e-ingreso" type="date" [(ngModel)]="formEstudiante.fechaIngreso" name="e-ingreso" /></span></label>
        </div>
        @if (errorEstudiante()) { <div class="alert alert--danger" role="alert">{{ errorEstudiante() }}</div> }
        <div class="acciones">
          <button class="btn btn--primary btn--sm" type="button" [disabled]="guardandoEstudiante()" (click)="crearEstudiante()">
            @if (guardandoEstudiante()) { <span class="spinner"></span> Creando… } @else { Crear ficha de estudiante }
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    .subtitulo-menor { font-size: .82rem; color: var(--color-text-muted); margin: .5rem 0 .35rem; }
    .lista-vinculos { display: flex; flex-direction: column; gap: .1rem; margin-bottom: .5rem; }
    .fila-vinculo {
      display: flex; align-items: center; gap: .6rem; padding: .4rem 0;
      border-bottom: 1px solid var(--color-border-light); font-size: .85rem;
    }
    .fila-vinculo:last-child { border-bottom: none; }
    .fila-vinculo .col-principal { flex: 1; font-weight: 600; }
    .toggle-inactivos { display: flex; align-items: center; gap: .4rem; font-size: .82rem; color: var(--color-text-muted); white-space: nowrap; }
  `],
})
export class FichaEstudianteComponent {
  readonly state = inject(PersonasStateService);
  private readonly servicio = inject(PersonasService);

  readonly persona = computed(() => this.state.seleccionada());

  formEstudiante: { idCategoria: number | null; codigoEstudiante: string; fechaIngreso: string } =
    { idCategoria: null, codigoEstudiante: '', fechaIngreso: new Date().toISOString().slice(0, 10) };
  readonly guardandoEstudiante = signal(false);
  readonly errorEstudiante = signal('');

  formVinculo: { idRepresentante: number | null; relacion: string; contactoPrincipal: boolean } =
    { idRepresentante: null, relacion: '', contactoPrincipal: false };
  readonly guardandoVinculo = signal(false);
  readonly errorVinculo = signal('');

  constructor() {
    effect(() => {
      this.state.seleccionada();
      this.formEstudiante = { idCategoria: null, codigoEstudiante: '', fechaIngreso: new Date().toISOString().slice(0, 10) };
      this.formVinculo = { idRepresentante: null, relacion: '', contactoPrincipal: false };
      this.errorEstudiante.set('');
      this.errorVinculo.set('');
    });
  }

  crearEstudiante(): void {
    if (this.formEstudiante.idCategoria === null) return;
    const idPersona = this.persona()!.persona.idPersona;
    this.guardandoEstudiante.set(true);
    this.errorEstudiante.set('');
    this.servicio.crearEstudiante({
      idPersona, idCategoria: this.formEstudiante.idCategoria, idEstadoGeneral: ESTADO_GENERAL_ACTIVO,
      codigoEstudiante: this.formEstudiante.codigoEstudiante, fechaIngreso: this.formEstudiante.fechaIngreso,
      peso: null, altura: null,
    }).subscribe({
      next: () => { this.guardandoEstudiante.set(false); this.state.cargarPersonas(true); },
      error: (err) => { this.guardandoEstudiante.set(false); this.errorEstudiante.set(err?.error?.detail ?? 'Error del servidor'); },
    });
  }

  vincularRepresentante(idEstudiante: number): void {
    const idRepresentante = this.formVinculo.idRepresentante;
    if (idRepresentante === null) return;
    this.guardandoVinculo.set(true);
    this.errorVinculo.set('');
    this.servicio.vincularEstudianteARepresentante(idRepresentante, idEstudiante, {
      relacion: this.formVinculo.relacion || null,
      contactoPrincipal: this.formVinculo.contactoPrincipal,
    }).subscribe({
      next: () => {
        this.guardandoVinculo.set(false);
        this.formVinculo = { idRepresentante: null, relacion: '', contactoPrincipal: false };
        this.state.cargarPersonas(true);
      },
      error: (err) => { this.guardandoVinculo.set(false); this.errorVinculo.set(err?.error?.detail ?? 'Error del servidor'); },
    });
  }

  desvincularRepresentante(idRepresentante: number, idEstudiante: number): void {
    this.servicio.desvincularEstudianteDeRepresentante(idRepresentante, idEstudiante).subscribe({
      next: () => this.state.cargarPersonas(true),
      error: (err) => this.errorVinculo.set(err?.error?.detail ?? 'Error del servidor'),
    });
  }
}
