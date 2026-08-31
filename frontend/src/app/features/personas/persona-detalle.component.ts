import { Component, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PersonasService } from './personas.service';
import { PersonasStateService } from './personas-state.service';
import { PersonaResponse } from './personas.models';
import { CuentaUsuarioComponent } from './cuenta-usuario.component';
import { FichaEstudianteComponent } from './ficha-estudiante.component';
import { FichaEntrenadorComponent } from './ficha-entrenador.component';
import { FichaRepresentanteComponent } from './ficha-representante.component';
import { mensajeDeError } from '../../core/mensaje-error';

type FormularioPersona = {
  nombre: string; apellido: string; cedula: string; correo: string; telefono: string; fechaNacimiento: string;
};

const PERSONA_VACIA: FormularioPersona = { nombre: '', apellido: '', cedula: '', correo: '', telefono: '', fechaNacimiento: '' };

@Component({
  selector: 'app-persona-detalle',
  standalone: true,
  imports: [CommonModule, FormsModule, CuentaUsuarioComponent, FichaEstudianteComponent, FichaEntrenadorComponent, FichaRepresentanteComponent],
  template: `
    <div class="card panel-detalle">
      @if (!state.mostrandoDetalle()) {
        <p class="aviso">Seleccioná una persona de la lista, o creá una nueva.</p>
      } @else {
        <h2 class="subtitulo">{{ state.esNueva() ? 'Nueva persona' : formPersona.nombre + ' ' + formPersona.apellido }}</h2>

        <form class="bloque" (ngSubmit)="guardarPersona()">
          <div class="fila-2">
            <label class="field" for="p-nombre"><span class="field__label">Nombre</span>
              <span class="field__control"><input id="p-nombre" [(ngModel)]="formPersona.nombre" name="p-nombre" required /></span></label>
            <label class="field" for="p-apellido"><span class="field__label">Apellido</span>
              <span class="field__control"><input id="p-apellido" [(ngModel)]="formPersona.apellido" name="p-apellido" required /></span></label>
          </div>
          <div class="fila-2">
            <label class="field" for="p-cedula"><span class="field__label">Cédula</span>
              <span class="field__control"><input id="p-cedula" [(ngModel)]="formPersona.cedula" name="p-cedula" required pattern="\\d{10}" maxlength="10" /></span></label>
            <label class="field" for="p-fecha"><span class="field__label">Fecha de nacimiento</span>
              <span class="field__control"><input id="p-fecha" type="date" [(ngModel)]="formPersona.fechaNacimiento" name="p-fecha" required /></span></label>
          </div>
          <div class="fila-2">
            <label class="field" for="p-correo"><span class="field__label">Correo</span>
              <span class="field__control"><input id="p-correo" type="email" [(ngModel)]="formPersona.correo" name="p-correo" required /></span></label>
            <label class="field" for="p-telefono"><span class="field__label">Teléfono</span>
              <span class="field__control"><input id="p-telefono" [(ngModel)]="formPersona.telefono" name="p-telefono" /></span></label>
          </div>
          @if (errorPersona()) { <div class="alert alert--danger" role="alert">{{ errorPersona() }}</div> }
          <div class="acciones">
            <button class="btn btn--primary" type="submit" [disabled]="guardandoPersona()">
              @if (guardandoPersona()) { <span class="spinner"></span> Guardando… } @else { {{ state.esNueva() ? 'Crear persona' : 'Guardar datos' }} }
            </button>
          </div>
        </form>

        @if (state.esNueva()) {
          <p class="aviso">
            Guardá primero los datos de la persona. La cuenta de usuario y la ficha se habilitan después,
            y cuál de las tres fichas aparece lo decide el rol que le des a la cuenta.
          </p>
        } @else {
          <app-cuenta-usuario />

          @switch (fichaQueCorresponde()) {
            @case ('ESTUDIANTE') { <app-ficha-estudiante /> }
            @case ('ENTRENADOR') { <app-ficha-entrenador /> }
            @case ('REPRESENTANTE') { <app-ficha-representante /> }
            @default {
              <div class="bloque bloque--separado">
                <h3 class="subtitulo-seccion">Ficha</h3>
                <p class="aviso">
                  Una cuenta con rol <strong>{{ rolCuenta() }}</strong> no lleva ficha deportiva.
                  Solo llevan ficha el estudiante, el entrenador y el representante.
                </p>
              </div>
            }
          }

          @if (notaDeFicha(); as nota) { <p class="nota-ficha">{{ nota }}</p> }
        }
      }
    </div>
  `,
  styles: [`
    .panel-detalle { padding: 1.5rem; display: flex; flex-direction: column; gap: 1rem; }
    .subtitulo { font-size: 1rem; }
    .nota-ficha { font-size: 0.85rem; color: var(--texto-suave, #64748b); margin: 0; }
  `],
})
export class PersonaDetalleComponent {
  readonly state = inject(PersonasStateService);
  private readonly servicio = inject(PersonasService);

  formPersona: FormularioPersona = { ...PERSONA_VACIA };
  readonly guardandoPersona = signal(false);
  readonly errorPersona = signal('');

  readonly rolCuenta = computed(() => this.state.seleccionada()?.usuario?.roles[0] ?? null);

  readonly fichaExistente = computed(() => {
    const p = this.state.seleccionada();
    if (!p) return null;
    if (p.estudiante) return 'ESTUDIANTE';
    if (p.entrenador) return 'ENTRENADOR';
    if (p.representante) return 'REPRESENTANTE';
    return null;
  });

  readonly fichaQueCorresponde = computed(() => {
    const ya = this.fichaExistente();
    if (ya) return ya;
    const rol = this.rolCuenta();
    if (rol === 'ESTUDIANTE' || rol === 'ENTRENADOR' || rol === 'REPRESENTANTE') return rol;
    if (rol === null) return 'ESTUDIANTE';
    return null;
  });

  readonly notaDeFicha = computed(() => {
    const p = this.state.seleccionada();
    if (!p || this.fichaExistente()) return null;
    if (this.rolCuenta() === null) {
      return 'Todavía no tiene cuenta. Sin cuenta solo se puede llevar ficha de estudiante, porque un menor '
        + 'puede estar matriculado sin iniciar sesión nunca. Para entrenador o representante, creale antes '
        + 'la cuenta con ese rol.';
    }
    return null;
  });

  constructor() {
    effect(() => {
      const seleccionada = this.state.seleccionada();
      const esNueva = this.state.esNueva();
      if (seleccionada) {
        this.formPersona = {
          nombre: seleccionada.persona.nombre, apellido: seleccionada.persona.apellido, cedula: seleccionada.persona.cedula,
          correo: seleccionada.persona.correo, telefono: seleccionada.persona.telefono ?? '', fechaNacimiento: seleccionada.persona.fechaNacimiento,
        };
      } else if (esNueva) {
        this.formPersona = { ...PERSONA_VACIA };
      }
      this.errorPersona.set('');
    });
  }

  guardarPersona(): void {
    this.guardandoPersona.set(true);
    this.errorPersona.set('');
    const request = {
      nombre: this.formPersona.nombre, apellido: this.formPersona.apellido, cedula: this.formPersona.cedula,
      correo: this.formPersona.correo, telefono: this.formPersona.telefono || null, foto: null,
      fechaNacimiento: this.formPersona.fechaNacimiento,
    };

    if (this.state.esNueva()) {
      this.servicio.crearPersona(request).subscribe({
        next: (creada: PersonaResponse) => {
          this.guardandoPersona.set(false);
          this.state.esNueva.set(false);
          this.state.cargarPersonas();
          this.state.seleccionar({ persona: creada, usuario: null, estudiante: null, entrenador: null, representante: null });
        },
        error: (err) => this.manejarError(err),
      });
      return;
    }

    const idPersona = this.state.seleccionada()!.persona.idPersona;
    this.servicio.editarPersona(idPersona, request).subscribe({
      next: () => { this.guardandoPersona.set(false); this.state.cargarPersonas(true); },
      error: (err) => this.manejarError(err),
    });
  }

  private manejarError(err: unknown): void {
    this.guardandoPersona.set(false);
    this.errorPersona.set(mensajeDeError(err));
  }
}
