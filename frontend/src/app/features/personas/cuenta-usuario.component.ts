import { Component, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PersonasService, ESTADO_GENERAL_ACTIVO } from './personas.service';
import { PersonasStateService } from './personas-state.service';
import { RolUsuario, ROLES_USUARIO, UsuarioResponse } from './personas.models';
import { mensajeDeError } from '../../core/mensaje-error';

/**
 * Seccion "Cuenta de usuario" del panel de detalle: crear, editar y
 * desactivar la cuenta de la persona seleccionada. Uno de los componentes
 * en que se dividio personas-admin.component.ts (R-05, informe de
 * evaluacion de calidad).
 */
@Component({
  selector: 'app-cuenta-usuario',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="bloque bloque--separado">
      <h3 class="subtitulo-seccion">Cuenta de usuario</h3>
      @if (persona()?.usuario; as u) {
        @if (!editando()) {
          <p class="resumen-seccion">{{ u.username }} · {{ u.roles.join(', ') || 'sin rol' }} · {{ u.activo ? 'activo' : 'inactivo' }}</p>
          <div class="acciones">
            <button class="btn btn--ghost btn--sm" type="button" (click)="iniciarEdicion(u)">Editar cuenta</button>
            @if (u.activo) {
              <button class="btn btn--ghost btn--sm" type="button" (click)="desactivar(u.idUsuario)">Desactivar cuenta</button>
            }
          </div>
        } @else {
          <div class="fila-2">
            <label class="field" for="u-username-editar"><span class="field__label">Usuario</span>
              <span class="field__control"><input id="u-username-editar" type="email" [(ngModel)]="formUsuario.username" name="u-username-editar" /></span></label>
            <label class="field" for="u-password-editar"><span class="field__label">Contraseña</span>
              <span class="field__control"><input id="u-password-editar" type="password" [(ngModel)]="formUsuario.password" name="u-password-editar" minlength="6" placeholder="Dejar en blanco para no cambiarla" /></span></label>
          </div>
          <label class="field" for="u-rol-editar"><span class="field__label">Rol</span>
            <span class="field__control">
              <select id="u-rol-editar" [(ngModel)]="formUsuario.rol" name="u-rol-editar">
                @for (r of roles; track r) { <option [value]="r">{{ r }}</option> }
              </select>
            </span>
          </label>
          @if (error()) { <div class="alert alert--danger" role="alert">{{ error() }}</div> }
          <div class="acciones">
            <button class="btn btn--ghost btn--sm" type="button" [disabled]="guardando()" (click)="cancelarEdicion()">Cancelar</button>
            <button class="btn btn--primary btn--sm" type="button" [disabled]="guardando()" (click)="guardarEdicion(u.idUsuario)">
              @if (guardando()) { <span class="spinner"></span> Guardando… } @else { Guardar }
            </button>
          </div>
        }
      } @else {
        <div class="fila-2">
          <label class="field" for="u-username"><span class="field__label">Usuario</span>
            <span class="field__control"><input id="u-username" type="email" [(ngModel)]="formUsuario.username" name="u-username" /></span></label>
          <label class="field" for="u-password"><span class="field__label">Contraseña</span>
            <span class="field__control"><input id="u-password" type="password" [(ngModel)]="formUsuario.password" name="u-password" minlength="6" /></span></label>
        </div>
        <label class="field" for="u-rol"><span class="field__label">Rol</span>
          <span class="field__control">
            <select id="u-rol" [(ngModel)]="formUsuario.rol" name="u-rol">
              @for (r of roles; track r) { <option [value]="r">{{ r }}</option> }
            </select>
          </span>
        </label>
        @if (error()) { <div class="alert alert--danger" role="alert">{{ error() }}</div> }
        <div class="acciones">
          <button class="btn btn--primary btn--sm" type="button" [disabled]="guardando()" (click)="crear()">
            @if (guardando()) { <span class="spinner"></span> Creando… } @else { Crear cuenta }
          </button>
        </div>
      }
    </div>
  `,
})
export class CuentaUsuarioComponent {
  readonly state = inject(PersonasStateService);
  private readonly servicio = inject(PersonasService);

  readonly roles = ROLES_USUARIO;
  readonly persona = computed(() => this.state.seleccionada());

  formUsuario: { username: string; password: string; rol: RolUsuario } = { username: '', password: '', rol: 'ENTRENADOR' };
  readonly guardando = signal(false);
  readonly error = signal('');
  readonly editando = signal(false);

  constructor() {
    // Al cambiar de persona seleccionada, el formulario de edicion vuelve
    // a su estado inicial: sin esto arrastraria los datos de la cuenta
    // anterior si el administrador entra directo a "Editar cuenta".
    effect(() => {
      this.state.seleccionada();
      this.formUsuario = { username: '', password: '', rol: 'ENTRENADOR' };
      this.error.set('');
      this.editando.set(false);
    });
  }

  crear(): void {
    const idPersona = this.persona()!.persona.idPersona;
    this.guardando.set(true);
    this.error.set('');
    this.servicio.crearUsuario({
      idPersona, idEstadoGeneral: ESTADO_GENERAL_ACTIVO,
      username: this.formUsuario.username, password: this.formUsuario.password, rol: this.formUsuario.rol,
    }).subscribe({
      next: () => { this.guardando.set(false); this.state.cargarPersonas(true); },
      error: (err) => this.manejarError(err),
    });
  }

  desactivar(idUsuario: number): void {
    this.servicio.desactivarUsuario(idUsuario).subscribe({ next: () => this.state.cargarPersonas(true) });
  }

  iniciarEdicion(u: UsuarioResponse): void {
    this.formUsuario = { username: u.username, password: '', rol: (u.roles[0] as RolUsuario) ?? 'ENTRENADOR' };
    this.error.set('');
    this.editando.set(true);
  }

  cancelarEdicion(): void {
    this.editando.set(false);
    this.error.set('');
  }

  guardarEdicion(idUsuario: number): void {
    const idPersona = this.persona()!.persona.idPersona;
    const u = this.persona()!.usuario!;
    this.guardando.set(true);
    this.error.set('');
    this.servicio.editarUsuario(idUsuario, {
      idPersona, idEstadoGeneral: u.idEstadoGeneral,
      username: this.formUsuario.username, password: this.formUsuario.password || null, rol: this.formUsuario.rol,
    }).subscribe({
      next: () => { this.guardando.set(false); this.editando.set(false); this.state.cargarPersonas(true); },
      error: (err) => this.manejarError(err),
    });
  }

  private manejarError(err: any): void {
    this.guardando.set(false);
    this.error.set(mensajeDeError(err));
  }
}
