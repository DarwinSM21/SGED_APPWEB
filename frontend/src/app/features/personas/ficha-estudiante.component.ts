import { Component, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PersonasService, ESTADO_GENERAL_ACTIVO } from './personas.service';
import { PersonasStateService } from './personas-state.service';
import { mensajeDeError } from '../../core/mensaje-error';
import { BuscadorOpcionesComponent, OpcionBuscable } from '../../core/buscador-opciones.component';

/**
 * Seccion "Ficha de estudiante" del panel de detalle: alta de la ficha y
 * gestion de sus vinculos con representantes. Uno de los componentes en
 * que se dividio personas-admin.component.ts (R-05, informe de evaluacion
 * de calidad).
 */
@Component({
  selector: 'app-ficha-estudiante',
  standalone: true,
  imports: [CommonModule, FormsModule, BuscadorOpcionesComponent],
  template: `
    <div class="bloque bloque--separado">
      <h3 class="subtitulo-seccion">Ficha de estudiante</h3>
      @if (persona()?.estudiante; as e) {
        @if (!editandoEstudiante()) {
          <p class="resumen-seccion">
            {{ e.codigoEstudiante }} · {{ e.nombreCategoria }}
            @if (e.abreviaturaPosicion) { · {{ e.abreviaturaPosicion }} }
            · {{ e.activo ? 'activo' : 'inactivo' }}
          </p>
          <div class="acciones">
            <button class="btn btn--ghost btn--sm" type="button" (click)="iniciarEdicionEstudiante(e)">Editar ficha</button>
          </div>
        } @else {
          <div class="fila-2">
            <label class="field" for="e-categoria-editar"><span class="field__label">Categoría</span>
              <span class="field__control">
                <select id="e-categoria-editar" [(ngModel)]="formEstudiante.idCategoria" name="e-categoria-editar">
                  <option [ngValue]="null" disabled>Selecciona…</option>
                  @for (c of state.categorias(); track c.idCategoria) { <option [ngValue]="c.idCategoria">{{ c.nombre }}</option> }
                </select>
              </span></label>
            <label class="field" for="e-codigo-editar"><span class="field__label">Código</span>
              <span class="field__control"><input id="e-codigo-editar" [(ngModel)]="formEstudiante.codigoEstudiante" name="e-codigo-editar" /></span></label>
          </div>
          <div class="fila-2">
            <label class="field" for="e-ingreso-editar"><span class="field__label">Fecha de ingreso</span>
              <span class="field__control"><input id="e-ingreso-editar" type="date" [(ngModel)]="formEstudiante.fechaIngreso" name="e-ingreso-editar" /></span></label>
            <label class="field" for="e-posicion-editar"><span class="field__label">Posición</span>
              <span class="field__control">
                <select id="e-posicion-editar" [(ngModel)]="formEstudiante.idPosicion" name="e-posicion-editar">
                  <option [ngValue]="null">Sin posición</option>
                  @for (p of state.posiciones(); track p.idPosicion) { <option [ngValue]="p.idPosicion">{{ p.nombre }} ({{ p.abreviatura }})</option> }
                </select>
              </span></label>
          </div>
          @if (errorEstudiante()) { <div class="alert alert--danger" role="alert">{{ errorEstudiante() }}</div> }
          <div class="acciones">
            <button class="btn btn--ghost btn--sm" type="button" [disabled]="guardandoEstudiante()" (click)="cancelarEdicionEstudiante()">Cancelar</button>
            <button class="btn btn--primary btn--sm" type="button" [disabled]="guardandoEstudiante()" (click)="guardarEdicionEstudiante(e.idEstudiante)">
              @if (guardandoEstudiante()) { <span class="spinner"></span> Guardando… } @else { Guardar }
            </button>
          </div>
        }

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
            <app-buscador-opciones
              etiqueta="Agregar representante"
              marcador="Busca por nombre o apellido…"
              [opciones]="opcionesRepresentantes()"
              [textoSeleccionado]="nombreRepresentante(formVinculo.idRepresentante)"
              (seleccionada)="formVinculo.idRepresentante = $event.id"
              (limpiada)="formVinculo.idRepresentante = null" />
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
      } @else if (rolIncoherente(); as rol) {
        <p class="aviso">
          Esta persona ya tiene una cuenta con rol <strong>{{ rol }}</strong>. Una cuenta solo puede tener un rol
          coherente con su ficha, así que acá no se le puede crear una ficha de estudiante.
        </p>
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
          <label class="field" for="e-posicion"><span class="field__label">Posición (opcional)</span>
            <span class="field__control">
              <select id="e-posicion" [(ngModel)]="formEstudiante.idPosicion" name="e-posicion">
                <option [ngValue]="null">Sin posición todavía</option>
                @for (p of state.posiciones(); track p.idPosicion) { <option [ngValue]="p.idPosicion">{{ p.nombre }} ({{ p.abreviatura }})</option> }
              </select>
            </span></label>
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

  /**
   * La lista de representantes crece con las familias de la escuela, asi que
   * es la que antes deja de servir como desplegable. Categoria y posicion se
   * quedan como select a proposito: son listas cortas y cerradas -tres
   * categorias, diez posiciones de futbol- donde un buscador solo agrega
   * pasos.
   */
  readonly opcionesRepresentantes = computed<OpcionBuscable[]>(() =>
    this.state.representantesDisponibles().map((r) => ({
      id: r.idRepresentante,
      titulo: r.nombre + ' ' + r.apellido,
    })));

  nombreRepresentante(id: number | null): string | null {
    if (id === null) return null;
    const r = this.state.representantesDisponibles().find((x) => x.idRepresentante === id);
    return r ? r.nombre + ' ' + r.apellido : null;
  }

  readonly state = inject(PersonasStateService);
  private readonly servicio = inject(PersonasService);

  readonly persona = computed(() => this.state.seleccionada());

  /**
   * El backend ya rechaza crear la ficha si la cuenta tiene otro rol
   * (EstudianteAccesoService.validarCoherenciaConFichaEstudiante); esto
   * solo evita mostrarle el formulario a alguien que de todas formas va a
   * chocar con ese error al enviarlo.
   */
  readonly rolIncoherente = computed(() => {
    const usuario = this.persona()?.usuario;
    if (!usuario || usuario.roles.includes('ESTUDIANTE')) return null;
    return usuario.roles[0] ?? null;
  });

  formEstudiante: { idCategoria: number | null; codigoEstudiante: string; fechaIngreso: string; idPosicion: number | null } =
    { idCategoria: null, codigoEstudiante: '', fechaIngreso: new Date().toISOString().slice(0, 10), idPosicion: null };
  readonly guardandoEstudiante = signal(false);
  readonly errorEstudiante = signal('');
  readonly editandoEstudiante = signal(false);
  /** Peso/altura no se editan en esta pantalla (no forman parte del pedido), pero
   *  igual hay que reenviarlos tal cual venian: EstudianteService.editar los
   *  sobreescribe con lo que llegue en el request, así que mandar null los borraría. */
  private pesoAlturaEditando: { peso: number | null; altura: number | null } = { peso: null, altura: null };

  formVinculo: { idRepresentante: number | null; relacion: string; contactoPrincipal: boolean } =
    { idRepresentante: null, relacion: '', contactoPrincipal: false };
  readonly guardandoVinculo = signal(false);
  readonly errorVinculo = signal('');

  constructor() {
    effect(() => {
      this.state.seleccionada();
      this.formEstudiante = { idCategoria: null, codigoEstudiante: '', fechaIngreso: new Date().toISOString().slice(0, 10), idPosicion: null };
      this.formVinculo = { idRepresentante: null, relacion: '', contactoPrincipal: false };
      this.errorEstudiante.set('');
      this.errorVinculo.set('');
      this.editandoEstudiante.set(false);
    });
  }

  iniciarEdicionEstudiante(e: { idCategoria: number; codigoEstudiante: string; fechaIngreso: string; idPosicion: number | null; peso: number | null; altura: number | null }): void {
    this.formEstudiante = {
      idCategoria: e.idCategoria, codigoEstudiante: e.codigoEstudiante,
      fechaIngreso: e.fechaIngreso, idPosicion: e.idPosicion,
    };
    this.pesoAlturaEditando = { peso: e.peso, altura: e.altura };
    this.errorEstudiante.set('');
    this.editandoEstudiante.set(true);
  }

  cancelarEdicionEstudiante(): void {
    this.editandoEstudiante.set(false);
    this.errorEstudiante.set('');
  }

  guardarEdicionEstudiante(idEstudiante: number): void {
    if (this.formEstudiante.idCategoria === null) return;
    this.guardandoEstudiante.set(true);
    this.errorEstudiante.set('');
    this.servicio.editarEstudiante(idEstudiante, {
      idPersona: this.persona()!.persona.idPersona, idCategoria: this.formEstudiante.idCategoria, idEstadoGeneral: ESTADO_GENERAL_ACTIVO,
      codigoEstudiante: this.formEstudiante.codigoEstudiante, fechaIngreso: this.formEstudiante.fechaIngreso,
      peso: this.pesoAlturaEditando.peso, altura: this.pesoAlturaEditando.altura, idPosicion: this.formEstudiante.idPosicion,
    }).subscribe({
      next: () => { this.guardandoEstudiante.set(false); this.editandoEstudiante.set(false); this.state.cargarPersonas(true); },
      error: (err) => { this.guardandoEstudiante.set(false); this.errorEstudiante.set(mensajeDeError(err)); },
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
      peso: null, altura: null, idPosicion: this.formEstudiante.idPosicion,
    }).subscribe({
      next: () => { this.guardandoEstudiante.set(false); this.state.cargarPersonas(true); },
      error: (err) => { this.guardandoEstudiante.set(false); this.errorEstudiante.set(mensajeDeError(err)); },
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
      error: (err) => { this.guardandoVinculo.set(false); this.errorVinculo.set(mensajeDeError(err)); },
    });
  }

  desvincularRepresentante(idRepresentante: number, idEstudiante: number): void {
    this.servicio.desvincularEstudianteDeRepresentante(idRepresentante, idEstudiante).subscribe({
      next: () => this.state.cargarPersonas(true),
      error: (err) => this.errorVinculo.set(mensajeDeError(err)),
    });
  }
}
