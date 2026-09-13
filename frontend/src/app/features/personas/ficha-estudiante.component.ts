import { Component, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PersonasService, ESTADO_GENERAL_ACTIVO } from './personas.service';
import { PersonasStateService } from './personas-state.service';
import { mensajeDeError } from '../../core/mensaje-error';
import { BuscadorOpcionesComponent, OpcionBuscable } from '../../core/buscador-opciones.component';
import { ConfirmarAccionComponent } from '../../core/confirmar-accion.component';

@Component({
  selector: 'app-ficha-estudiante',
  standalone: true,
  imports: [CommonModule, FormsModule, BuscadorOpcionesComponent, ConfirmarAccionComponent],
  template: `
    <div class="bloque bloque--separado">
      <h3 class="subtitulo-seccion">Ficha de estudiante</h3>
      @if (persona()?.student; as e) {
        @if (!editandoEstudiante()) {
          <p class="resumen-seccion">
            {{ e.studentCode }} · {{ e.categoryName }}
            @if (e.positionAbbreviation) { · {{ e.positionAbbreviation }} }
            · {{ e.active ? 'activo' : 'inactivo' }}
          </p>
          <div class="acciones">
            <button class="btn btn--ghost btn--sm" type="button" (click)="iniciarEdicionEstudiante(e)">Editar ficha</button>
          </div>
        } @else {
          <div class="fila-2">
            <label class="field" for="e-categoria-editar"><span class="field__label">Categoría</span>
              <span class="field__control">
                <select id="e-categoria-editar" [(ngModel)]="formEstudiante.categoryId" name="e-categoria-editar">
                  <option [ngValue]="null" disabled>Selecciona…</option>
                  @for (c of state.categorias(); track c.categoryId) { <option [ngValue]="c.categoryId">{{ c.name }}</option> }
                </select>
              </span></label>
            <label class="field" for="e-codigo-editar"><span class="field__label">Código</span>
              <span class="field__control"><input id="e-codigo-editar" [(ngModel)]="formEstudiante.studentCode" name="e-codigo-editar" /></span></label>
          </div>
          <div class="fila-2">
            <label class="field" for="e-ingreso-editar"><span class="field__label">Fecha de ingreso</span>
              <span class="field__control"><input id="e-ingreso-editar" type="date" [(ngModel)]="formEstudiante.enrollmentDate" name="e-ingreso-editar" /></span></label>
            <label class="field" for="e-posicion-editar"><span class="field__label">Posición</span>
              <span class="field__control">
                <select id="e-posicion-editar" [(ngModel)]="formEstudiante.positionId" name="e-posicion-editar">
                  <option [ngValue]="null">Sin posición</option>
                  @for (p of state.posiciones(); track p.positionId) { <option [ngValue]="p.positionId">{{ p.name }} ({{ p.abbreviation }})</option> }
                </select>
              </span></label>
          </div>
          @if (errorEstudiante()) { <div class="alert alert--danger" role="alert">{{ errorEstudiante() }}</div> }
          <div class="acciones">
            <button class="btn btn--ghost btn--sm" type="button" [disabled]="guardandoEstudiante()" (click)="cancelarEdicionEstudiante()">Cancelar</button>
            <button class="btn btn--primary btn--sm" type="button" [disabled]="guardandoEstudiante()" (click)="guardarEdicionEstudiante(e.studentId)">
              @if (guardandoEstudiante()) { <span class="spinner"></span> Guardando… } @else { Guardar }
            </button>
          </div>
        }

        <h4 class="subtitulo-menor">Representantes</h4>
        @if (state.representantesDelEstudiante().length === 0) {
          <p class="aviso">Este estudiante todavía no tiene representantes asignados.</p>
        } @else {
          <div class="lista-vinculos">
            @for (v of state.representantesDelEstudiante(); track v.guardianId) {
              <div class="fila-vinculo">
                <span class="col-principal">{{ v.name }} {{ v.lastName }}</span>
                <span class="col-secundaria">{{ v.relationship || 'sin relación' }}</span>
                @if (v.primaryContact) { <span class="badge badge--info">Contacto principal</span> }
                <app-confirmar-accion etiqueta="Desvincular"
                                      [pregunta]="'¿Quitarle el acceso a ' + v.name + ' ' + v.lastName + '?'"
                                      textoConfirmar="Sí, quitar" enCurso="Quitando…"
                                      [ocupado]="guardandoVinculo()"
                                      (confirmado)="desvincularRepresentante(v.guardianId, e.studentId)" />
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
              [textoSeleccionado]="nombreRepresentante(formVinculo.guardianId)"
              (seleccionada)="formVinculo.guardianId = $event.id"
              (limpiada)="formVinculo.guardianId = null" />
            <label class="field" for="v-relacion"><span class="field__label">Relación</span>
              <span class="field__control"><input id="v-relacion" [(ngModel)]="formVinculo.relationship" name="v-relacion" placeholder="Madre, padre, tutor…" /></span></label>
          </div>
          <label class="toggle-inactivos">
            <input type="checkbox" [(ngModel)]="formVinculo.primaryContact" name="v-principal" />
            Contacto principal
          </label>
          @if (errorVinculo()) { <div class="alert alert--danger" role="alert">{{ errorVinculo() }}</div> }
          <div class="acciones">
            <button class="btn btn--primary btn--sm" type="button" [disabled]="guardandoVinculo() || formVinculo.guardianId === null"
                    (click)="vincularRepresentante(e.studentId)">
              @if (guardandoVinculo()) { <span class="spinner"></span> Vinculando… } @else { Vincular }
            </button>
          </div>
        } @else if (representantesActivos() === 0) {
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
              <select id="e-categoria" [(ngModel)]="formEstudiante.categoryId" name="e-categoria">
                <option [ngValue]="null" disabled>Selecciona…</option>
                @for (c of state.categorias(); track c.categoryId) { <option [ngValue]="c.categoryId">{{ c.name }}</option> }
              </select>
            </span></label>
          <label class="field" for="e-codigo"><span class="field__label">Código</span>
            <span class="field__control">
              <input id="e-codigo" [(ngModel)]="formEstudiante.studentCode" name="e-codigo" readonly
                     [placeholder]="pidiendoCodigo() ? 'Generando…' : 'Se genera al guardar'" />
            </span>
            <span class="field__hint">Lo genera el sistema, no hace falta escribirlo.</span></label>
        </div>
        <div class="fila-2">
          <label class="field" for="e-ingreso"><span class="field__label">Fecha de ingreso</span>
            <span class="field__control"><input id="e-ingreso" type="date" [(ngModel)]="formEstudiante.enrollmentDate" name="e-ingreso" /></span></label>
          <label class="field" for="e-posicion"><span class="field__label">Posición (opcional)</span>
            <span class="field__control">
              <select id="e-posicion" [(ngModel)]="formEstudiante.positionId" name="e-posicion">
                <option [ngValue]="null">Sin posición todavía</option>
                @for (p of state.posiciones(); track p.positionId) { <option [ngValue]="p.positionId">{{ p.name }} ({{ p.abbreviation }})</option> }
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
  readonly opcionesRepresentantes = computed<OpcionBuscable[]>(() =>
    this.state.representantesDisponibles().map((r) => ({
      id: r.guardianId,
      titulo: r.name + ' ' + r.lastName,
    })));

  nombreRepresentante(id: number | null): string | null {
    if (id === null) return null;
    const r = this.state.representantesDisponibles().find((x) => x.guardianId === id);
    return r ? r.name + ' ' + r.lastName : null;
  }

  readonly state = inject(PersonasStateService);
  private readonly servicio = inject(PersonasService);

  readonly persona = computed(() => this.state.seleccionada());

  readonly representantesActivos = computed(() =>
    this.state.representantes().filter((r) => r.active).length);

  readonly rolIncoherente = computed(() => {
    const usuario = this.persona()?.user;
    if (!usuario || usuario.roles.includes('ESTUDIANTE')) return null;
    return usuario.roles[0] ?? null;
  });

  formEstudiante: { categoryId: number | null; studentCode: string; enrollmentDate: string; positionId: number | null } =
    { categoryId: null, studentCode: '', enrollmentDate: new Date().toISOString().slice(0, 10), positionId: null };
  readonly guardandoEstudiante = signal(false);
  readonly errorEstudiante = signal('');
  readonly editandoEstudiante = signal(false);
  readonly pidiendoCodigo = signal(false);
  private pesoAlturaEditando: { weight: number | null; height: number | null } = { weight: null, height: null };

  formVinculo: { guardianId: number | null; relationship: string; primaryContact: boolean } =
    { guardianId: null, relationship: '', primaryContact: false };
  readonly guardandoVinculo = signal(false);
  readonly errorVinculo = signal('');

  constructor() {
    effect(() => {
      const seleccionada = this.state.seleccionada();
      this.formEstudiante = { categoryId: null, studentCode: '', enrollmentDate: new Date().toISOString().slice(0, 10), positionId: null };
      this.formVinculo = { guardianId: null, relationship: '', primaryContact: false };
      this.errorEstudiante.set('');
      this.errorVinculo.set('');
      this.editandoEstudiante.set(false);

      if (seleccionada && !seleccionada.student && !this.rolIncoherente()) {
        this.pedirCodigoSugerido();
      }
    });
  }

  private pedirCodigoSugerido(): void {
    this.pidiendoCodigo.set(true);
    this.servicio.siguienteCodigoEstudiante(new Date().getFullYear()).subscribe({
      next: (codigo) => { this.pidiendoCodigo.set(false); this.formEstudiante.studentCode = codigo.trim(); },
      error: () => { this.pidiendoCodigo.set(false); },
    });
  }

  iniciarEdicionEstudiante(e: { categoryId: number; studentCode: string; enrollmentDate: string; positionId: number | null; weight: number | null; height: number | null }): void {
    this.formEstudiante = {
      categoryId: e.categoryId, studentCode: e.studentCode,
      enrollmentDate: e.enrollmentDate, positionId: e.positionId,
    };
    this.pesoAlturaEditando = { weight: e.weight, height: e.height };
    this.errorEstudiante.set('');
    this.editandoEstudiante.set(true);
  }

  cancelarEdicionEstudiante(): void {
    this.editandoEstudiante.set(false);
    this.errorEstudiante.set('');
  }

  guardarEdicionEstudiante(idEstudiante: number): void {
    if (this.formEstudiante.categoryId === null) return;
    this.guardandoEstudiante.set(true);
    this.errorEstudiante.set('');
    this.servicio.editarEstudiante(idEstudiante, {
      personId: this.persona()!.persona.personId, categoryId: this.formEstudiante.categoryId, generalStatusId: ESTADO_GENERAL_ACTIVO,
      studentCode: this.formEstudiante.studentCode, enrollmentDate: this.formEstudiante.enrollmentDate,
      weight: this.pesoAlturaEditando.weight, height: this.pesoAlturaEditando.height, positionId: this.formEstudiante.positionId,
    }).subscribe({
      next: () => { this.guardandoEstudiante.set(false); this.editandoEstudiante.set(false); this.state.cargarPersonas(true); },
      error: (err) => { this.guardandoEstudiante.set(false); this.errorEstudiante.set(mensajeDeError(err)); },
    });
  }

  crearEstudiante(): void {
    if (this.formEstudiante.categoryId === null) return;
    const personId = this.persona()!.persona.personId;
    this.guardandoEstudiante.set(true);
    this.errorEstudiante.set('');
    this.servicio.crearEstudiante({
      personId, categoryId: this.formEstudiante.categoryId, generalStatusId: ESTADO_GENERAL_ACTIVO,
      studentCode: this.formEstudiante.studentCode, enrollmentDate: this.formEstudiante.enrollmentDate,
      weight: null, height: null, positionId: this.formEstudiante.positionId,
    }).subscribe({
      next: () => { this.guardandoEstudiante.set(false); this.state.cargarPersonas(true); },
      error: (err) => { this.guardandoEstudiante.set(false); this.errorEstudiante.set(mensajeDeError(err)); },
    });
  }

  vincularRepresentante(idEstudiante: number): void {
    const idRepresentante = this.formVinculo.guardianId;
    if (idRepresentante === null) return;
    this.guardandoVinculo.set(true);
    this.errorVinculo.set('');
    this.servicio.vincularEstudianteARepresentante(idRepresentante, idEstudiante, {
      relationship: this.formVinculo.relationship || null,
      primaryContact: this.formVinculo.primaryContact,
    }).subscribe({
      next: () => {
        this.guardandoVinculo.set(false);
        this.formVinculo = { guardianId: null, relationship: '', primaryContact: false };
        this.state.cargarPersonas(true);
      },
      error: (err) => { this.guardandoVinculo.set(false); this.errorVinculo.set(mensajeDeError(err)); },
    });
  }

  desvincularRepresentante(idRepresentante: number, studentId: number): void {
    this.servicio.desvincularEstudianteDeRepresentante(idRepresentante, studentId).subscribe({
      next: () => this.state.cargarPersonas(true),
      error: (err) => this.errorVinculo.set(mensajeDeError(err)),
    });
  }
}
