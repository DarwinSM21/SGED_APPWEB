import { Component, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PersonasService } from './personas.service';
import { PersonasStateService } from './personas-state.service';

/**
 * Seccion "Entrenador" del panel de detalle. Uno de los componentes en
 * que se dividio personas-admin.component.ts (R-05, informe de evaluacion
 * de calidad).
 */
@Component({
  selector: 'app-ficha-entrenador',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="bloque bloque--separado">
      <h3 class="subtitulo-seccion">Entrenador</h3>
      @if (persona()?.entrenador; as ent) {
        <p class="resumen-seccion">{{ ent.nombreEspecialidad || 'sin especialidad' }} · {{ ent.experienciaAnios ?? 0 }} años</p>
      } @else if (persona()?.usuario) {
        <div class="fila-2">
          <label class="field" for="ent-especialidad"><span class="field__label">Especialidad</span>
            <span class="field__control">
              <select id="ent-especialidad" [(ngModel)]="formEntrenador.idEspecialidad" name="ent-especialidad">
                <option [ngValue]="null">Sin especialidad</option>
                @for (esp of state.especialidades(); track esp.idEspecialidad) { <option [ngValue]="esp.idEspecialidad">{{ esp.nombre }}</option> }
              </select>
            </span></label>
          <label class="field" for="ent-experiencia"><span class="field__label">Años de experiencia</span>
            <span class="field__control"><input id="ent-experiencia" type="number" min="0" [(ngModel)]="formEntrenador.experienciaAnios" name="ent-experiencia" /></span></label>
        </div>
        @if (error()) { <div class="alert alert--danger" role="alert">{{ error() }}</div> }
        <div class="acciones">
          <button class="btn btn--primary btn--sm" type="button" [disabled]="guardando()" (click)="crear()">
            @if (guardando()) { <span class="spinner"></span> Registrando… } @else { Registrar como entrenador }
          </button>
        </div>
      } @else {
        <p class="aviso">Primero creá una cuenta de usuario para poder registrarla como entrenador.</p>
      }
    </div>
  `,
})
export class FichaEntrenadorComponent {
  readonly state = inject(PersonasStateService);
  private readonly servicio = inject(PersonasService);

  readonly persona = computed(() => this.state.seleccionada());

  formEntrenador: { idEspecialidad: number | null; experienciaAnios: number | null } = { idEspecialidad: null, experienciaAnios: null };
  readonly guardando = signal(false);
  readonly error = signal('');

  constructor() {
    effect(() => {
      this.state.seleccionada();
      this.formEntrenador = { idEspecialidad: null, experienciaAnios: null };
      this.error.set('');
    });
  }

  crear(): void {
    const actual = this.persona()!;
    if (!actual.usuario) return;
    this.guardando.set(true);
    this.error.set('');
    this.servicio.crearEntrenador({
      idPersona: actual.persona.idPersona, idUsuario: actual.usuario.idUsuario,
      idEspecialidad: this.formEntrenador.idEspecialidad, experienciaAnios: this.formEntrenador.experienciaAnios,
      certificacion: null,
    }).subscribe({
      next: () => { this.guardando.set(false); this.state.cargarPersonas(true); },
      error: (err) => { this.guardando.set(false); this.error.set(err?.error?.detail ?? 'Error del servidor'); },
    });
  }
}
