import { Component, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PersonasService } from './personas.service';
import { PersonasStateService } from './personas-state.service';
import { mensajeDeError } from '../../core/mensaje-error';
import { ConfirmarAccionComponent } from '../../core/confirmar-accion.component';

@Component({
  selector: 'app-ficha-representante',
  standalone: true,
  imports: [CommonModule, FormsModule, ConfirmarAccionComponent],
  template: `
    <div class="bloque bloque--separado">
      <h3 class="subtitulo-seccion">Representante</h3>
      @if (persona()?.representante; as rep) {
        <p class="resumen-seccion">{{ rep.relationship || 'sin parentesco' }} · {{ rep.wards.length }} representado(s)</p>

        @if (rep.wards.length) {
          <ul class="vinculos">
            @for (r of rep.wards; track r.studentId) {
              <li class="vinculo">
                <div class="vinculo__quien">
                  <span class="vinculo__nombre">{{ r.fullName }}</span>
                  <span class="vinculo__meta">
                    {{ r.category }}
                    @if (r.relationship) { · {{ r.relationship }} }
                    @if (r.primaryContact) { <span class="badge badge--info">contacto principal</span> }
                  </span>
                </div>
                <app-confirmar-accion etiqueta="Quitar"
                                      [pregunta]="'¿Quitarle el acceso a ' + r.fullName + '?'"
                                      textoConfirmar="Sí, quitar" enCurso="Quitando…"
                                      [ocupado]="guardando()"
                                      (confirmado)="desvincular(rep.guardianId, r.studentId)" />
              </li>
            }
          </ul>
        } @else {
          <p class="aviso">Todavía no tiene ningún representado. Vinculá al menos uno para que pueda ver informes.</p>
        }

        @if (sinVincular().length) {
          <div class="fila-2">
            <label class="field" for="vin-estudiante"><span class="field__label">Agregar representado</span>
              <span class="field__control">
                <select id="vin-estudiante" [(ngModel)]="formVinculo.studentId" name="vin-estudiante">
                  <option [ngValue]="null">Elegí un estudiante…</option>
                  @for (e of sinVincular(); track e.studentId) {
                    <option [ngValue]="e.studentId">{{ e.personLastName }} {{ e.personName }} · {{ e.categoryName }}</option>
                  }
                </select>
              </span></label>
            <label class="field" for="vin-relacion"><span class="field__label">Relación</span>
              <span class="field__control"><input id="vin-relacion" [(ngModel)]="formVinculo.relationship" name="vin-relacion" placeholder="Madre, padre, tutor…" /></span></label>
          </div>
          <label class="check" for="vin-principal">
            <input id="vin-principal" type="checkbox" [(ngModel)]="formVinculo.primaryContact" name="vin-principal" />
            <span>Es el contacto principal de este estudiante</span>
          </label>
          @if (error()) { <div class="alert alert--danger" role="alert">{{ error() }}</div> }
          <div class="acciones">
            <button class="btn btn--primary btn--sm" type="button"
                    [disabled]="guardando() || formVinculo.studentId === null"
                    (click)="vincular(rep.guardianId)">
              @if (guardando()) { <span class="spinner"></span> Vinculando… } @else { Vincular }
            </button>
          </div>
        } @else {
          @if (error()) { <div class="alert alert--danger" role="alert">{{ error() }}</div> }
          <p class="aviso">No quedan estudiantes activos sin vincular a este representante.</p>
        }

      } @else if (persona()?.user) {
        <div class="fila-2">
          <label class="field" for="rep-parentesco"><span class="field__label">Parentesco</span>
            <span class="field__control"><input id="rep-parentesco" [(ngModel)]="formRepresentante.relationship" name="rep-parentesco" placeholder="Madre, padre, tutor…" /></span></label>
          <label class="field" for="rep-telefono"><span class="field__label">Teléfono de contacto</span>
            <span class="field__control"><input id="rep-telefono" [(ngModel)]="formRepresentante.contactPhone" name="rep-telefono" /></span></label>
        </div>
        @if (error()) { <div class="alert alert--danger" role="alert">{{ error() }}</div> }
        <div class="acciones">
          <button class="btn btn--primary btn--sm" type="button" [disabled]="guardando()" (click)="crear()">
            @if (guardando()) { <span class="spinner"></span> Registrando… } @else { Registrar como representante }
          </button>
        </div>
      } @else {
        <p class="aviso">Primero creá una cuenta de usuario para poder registrarla como representante.</p>
      }
    </div>
  `,
  styles: [`
    .vinculos { list-style: none; margin: 0 0 1rem; padding: 0; display: flex; flex-direction: column; gap: 0.5rem; }
    .vinculo { display: flex; align-items: center; justify-content: space-between; gap: 1rem;
               padding: 0.6rem 0.75rem; border: 1px solid var(--borde, #d7dde5); border-radius: 0.5rem; }
    .vinculo__quien { display: flex; flex-direction: column; gap: 0.15rem; min-width: 0; }
    .vinculo__nombre { font-weight: 600; }
    .vinculo__meta { font-size: 0.85rem; color: var(--texto-suave, #64748b); display: flex; align-items: center; gap: 0.4rem; flex-wrap: wrap; }
    .check { display: flex; align-items: center; gap: 0.5rem; margin: 0.5rem 0 0.75rem; font-size: 0.9rem; }
  `],
})
export class FichaRepresentanteComponent {
  readonly state = inject(PersonasStateService);
  private readonly servicio = inject(PersonasService);

  readonly persona = computed(() => this.state.seleccionada());

  readonly sinVincular = computed(() => {
    const rep = this.persona()?.representante;
    if (!rep) return [];
    const yaEstan = new Set(rep.wards.map((r) => r.studentId));
    return this.state.students()
      .filter((e) => e.active && !yaEstan.has(e.studentId))
      .sort((a, b) => (a.personLastName + ' ' + a.personName)
        .localeCompare(b.personLastName + ' ' + b.personName, 'es'));
  });

  formRepresentante: { relationship: string; contactPhone: string } = { relationship: '', contactPhone: '' };
  formVinculo: { studentId: number | null; relationship: string; primaryContact: boolean } =
    { studentId: null, relationship: '', primaryContact: false };

  readonly guardando = signal(false);
  readonly error = signal('');

  constructor() {
    effect(() => {
      this.state.seleccionada();
      this.formRepresentante = { relationship: '', contactPhone: '' };
      this.formVinculo = { studentId: null, relationship: '', primaryContact: false };
      this.error.set('');
    });
  }

  crear(): void {
    const actual = this.persona()!;
    if (!actual.user) return;
    this.guardando.set(true);
    this.error.set('');
    this.servicio.crearRepresentante({
      personId: actual.persona.personId, userId: actual.user.userId,
      relationship: this.formRepresentante.relationship || null, contactPhone: this.formRepresentante.contactPhone || null,
      initialStudentIds: [],
    }).subscribe({
      next: () => { this.guardando.set(false); this.state.cargarPersonas(true); },
      error: (err) => { this.guardando.set(false); this.error.set(mensajeDeError(err)); },
    });
  }

  vincular(idRepresentante: number): void {
    const idEstudiante = this.formVinculo.studentId;
    if (idEstudiante === null) return;
    this.guardando.set(true);
    this.error.set('');
    this.servicio.vincularEstudianteARepresentante(idRepresentante, idEstudiante, {
      relationship: this.formVinculo.relationship || null,
      primaryContact: this.formVinculo.primaryContact,
    }).subscribe({
      next: () => {
        this.guardando.set(false);
        this.formVinculo = { studentId: null, relationship: '', primaryContact: false };
        this.state.cargarPersonas(true);
      },
      error: (err) => { this.guardando.set(false); this.error.set(mensajeDeError(err)); },
    });
  }

  desvincular(idRepresentante: number, studentId: number): void {
    this.guardando.set(true);
    this.error.set('');
    this.servicio.desvincularEstudianteDeRepresentante(idRepresentante, studentId).subscribe({
      next: () => { this.guardando.set(false); this.state.cargarPersonas(true); },
      error: (err) => { this.guardando.set(false); this.error.set(mensajeDeError(err)); },
    });
  }
}
