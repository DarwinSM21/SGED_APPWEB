import { Component, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PersonasService } from './personas.service';
import { PersonasStateService } from './personas-state.service';

/**
 * Seccion "Representante" del panel de detalle. Uno de los componentes en
 * que se dividio personas-admin.component.ts (R-05, informe de evaluacion
 * de calidad).
 */
@Component({
  selector: 'app-ficha-representante',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="bloque bloque--separado">
      <h3 class="subtitulo-seccion">Representante</h3>
      @if (persona()?.representante; as rep) {
        <p class="resumen-seccion">{{ rep.parentesco || 'sin parentesco' }} · {{ rep.representados.length }} representado(s)</p>
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
})
export class FichaRepresentanteComponent {
  readonly state = inject(PersonasStateService);
  private readonly servicio = inject(PersonasService);

  readonly persona = computed(() => this.state.seleccionada());

  formRepresentante: { parentesco: string; telefonoContacto: string } = { parentesco: '', telefonoContacto: '' };
  readonly guardando = signal(false);
  readonly error = signal('');

  constructor() {
    effect(() => {
      this.state.seleccionada();
      this.formRepresentante = { parentesco: '', telefonoContacto: '' };
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
      error: (err) => { this.guardando.set(false); this.error.set(err?.error?.detail ?? 'Error del servidor'); },
    });
  }
}
