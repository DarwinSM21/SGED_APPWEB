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
        <p class="resumen-seccion">{{ rep.parentesco || 'sin parentesco' }} · {{ rep.representados.length }} representado(s)</p>

        @if (rep.representados.length) {
          <ul class="vinculos">
            @for (r of rep.representados; track r.idEstudiante) {
              <li class="vinculo">
                <div class="vinculo__quien">
                  <span class="vinculo__nombre">{{ r.nombreCompleto }}</span>
                  <span class="vinculo__meta">
                    {{ r.categoria }}
                    @if (r.relacion) { · {{ r.relacion }} }
                    @if (r.contactoPrincipal) { <span class="badge badge--info">contacto principal</span> }
                  </span>
                </div>
                <app-confirmar-accion etiqueta="Quitar"
                                      [pregunta]="'¿Quitarle el acceso a ' + r.nombreCompleto + '?'"
                                      textoConfirmar="Sí, quitar" enCurso="Quitando…"
                                      [ocupado]="guardando()"
                                      (confirmado)="desvincular(rep.idRepresentante, r.idEstudiante)" />
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
                <select id="vin-estudiante" [(ngModel)]="formVinculo.idEstudiante" name="vin-estudiante">
                  <option [ngValue]="null">Elegí un estudiante…</option>
                  @for (e of sinVincular(); track e.idEstudiante) {
                    <option [ngValue]="e.idEstudiante">{{ e.apellidoPersona }} {{ e.nombrePersona }} · {{ e.nombreCategoria }}</option>
                  }
                </select>
              </span></label>
            <label class="field" for="vin-relacion"><span class="field__label">Relación</span>
              <span class="field__control"><input id="vin-relacion" [(ngModel)]="formVinculo.relacion" name="vin-relacion" placeholder="Madre, padre, tutor…" /></span></label>
          </div>
          <label class="check" for="vin-principal">
            <input id="vin-principal" type="checkbox" [(ngModel)]="formVinculo.contactoPrincipal" name="vin-principal" />
            <span>Es el contacto principal de este estudiante</span>
          </label>
          @if (error()) { <div class="alert alert--danger" role="alert">{{ error() }}</div> }
          <div class="acciones">
            <button class="btn btn--primary btn--sm" type="button"
                    [disabled]="guardando() || formVinculo.idEstudiante === null"
                    (click)="vincular(rep.idRepresentante)">
              @if (guardando()) { <span class="spinner"></span> Vinculando… } @else { Vincular }
            </button>
          </div>
        } @else {
          @if (error()) { <div class="alert alert--danger" role="alert">{{ error() }}</div> }
          <p class="aviso">No quedan estudiantes activos sin vincular a este representante.</p>
        }

      } @else if (persona()?.usuario) {
        <div class="fila-2">
          <label class="field" for="rep-parentesco"><span class="field__label">Parentesco</span>
            <span class="field__control"><input id="rep-parentesco" [(ngModel)]="formRepresentante.parentesco" name="rep-parentesco" placeholder="Madre, padre, tutor…" /></span></label>
          <label class="field" for="rep-telefono"><span class="field__label">Teléfono de contacto</span>
            <span class="field__control"><input id="rep-telefono" [(ngModel)]="formRepresentante.telefonoContacto" name="rep-telefono" /></span></label>
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
    const yaEstan = new Set(rep.representados.map((r) => r.idEstudiante));
    return this.state.estudiantes()
      .filter((e) => e.activo && !yaEstan.has(e.idEstudiante))
      .sort((a, b) => (a.apellidoPersona + ' ' + a.nombrePersona)
        .localeCompare(b.apellidoPersona + ' ' + b.nombrePersona, 'es'));
  });

  formRepresentante: { parentesco: string; telefonoContacto: string } = { parentesco: '', telefonoContacto: '' };
  formVinculo: { idEstudiante: number | null; relacion: string; contactoPrincipal: boolean } =
    { idEstudiante: null, relacion: '', contactoPrincipal: false };

  readonly guardando = signal(false);
  readonly error = signal('');

  constructor() {
    effect(() => {
      this.state.seleccionada();
      this.formRepresentante = { parentesco: '', telefonoContacto: '' };
      this.formVinculo = { idEstudiante: null, relacion: '', contactoPrincipal: false };
      this.error.set('');
    });
  }

  crear(): void {
    const actual = this.persona()!;
    if (!actual.usuario) return;
    this.guardando.set(true);
    this.error.set('');
    this.servicio.crearRepresentante({
      idPersona: actual.persona.idPersona, idUsuario: actual.usuario.idUsuario,
      parentesco: this.formRepresentante.parentesco || null, telefonoContacto: this.formRepresentante.telefonoContacto || null,
      idsEstudiantesIniciales: [],
    }).subscribe({
      next: () => { this.guardando.set(false); this.state.cargarPersonas(true); },
      error: (err) => { this.guardando.set(false); this.error.set(mensajeDeError(err)); },
    });
  }

  vincular(idRepresentante: number): void {
    const idEstudiante = this.formVinculo.idEstudiante;
    if (idEstudiante === null) return;
    this.guardando.set(true);
    this.error.set('');
    this.servicio.vincularEstudianteARepresentante(idRepresentante, idEstudiante, {
      relacion: this.formVinculo.relacion || null,
      contactoPrincipal: this.formVinculo.contactoPrincipal,
    }).subscribe({
      next: () => {
        this.guardando.set(false);
        this.formVinculo = { idEstudiante: null, relacion: '', contactoPrincipal: false };
        this.state.cargarPersonas(true);
      },
      error: (err) => { this.guardando.set(false); this.error.set(mensajeDeError(err)); },
    });
  }

  desvincular(idRepresentante: number, idEstudiante: number): void {
    this.guardando.set(true);
    this.error.set('');
    this.servicio.desvincularEstudianteDeRepresentante(idRepresentante, idEstudiante).subscribe({
      next: () => { this.guardando.set(false); this.state.cargarPersonas(true); },
      error: (err) => { this.guardando.set(false); this.error.set(mensajeDeError(err)); },
    });
  }
}
