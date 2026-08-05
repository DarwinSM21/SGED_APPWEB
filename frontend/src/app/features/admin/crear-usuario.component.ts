import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../auth/auth.service';
import { AdminService } from './admin.service';
import { EstudianteOpcion } from './admin.models';

const ROLES = ['USER', 'ENTRENADOR', 'RECEPCIONISTA', 'REPRESENTANTE', 'ESTUDIANTE', 'ADMINISTRADOR'] as const;

/**
 * Alta de cuentas por un administrador: mismo criterio ya usado para
 * Entrenador ("el admin lo crea"), ahora tambien para Recepcionista,
 * Representante y Estudiante.
 *
 * ESTUDIANTE es un caso distinto a los demas: el estudiante YA existe en el
 * sistema (con su propia Persona), asi que en vez de /api/auth/registro
 * (que siempre crea una Persona nueva) se usa
 * POST /api/estudiantes/{id}/acceso, que solo agrega la cuenta sobre la
 * Persona existente. Por eso los campos de identidad (nombre, cedula, etc.)
 * se ocultan para ese rol: no aplican, ya estan cargados.
 */
@Component({
  selector: 'app-crear-usuario',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="pantalla">
      <h1 class="titulo-pantalla">Crear usuario</h1>

      <form class="card formulario" (ngSubmit)="onSubmit()">
        @if (rol !== 'ESTUDIANTE') {
          <div class="fila-2">
            <label class="field" for="nombre">
              <span class="field__label">Nombre</span>
              <span class="field__control">
                <input id="nombre" type="text" [(ngModel)]="nombre" name="nombre" required />
              </span>
            </label>
            <label class="field" for="apellido">
              <span class="field__label">Apellido</span>
              <span class="field__control">
                <input id="apellido" type="text" [(ngModel)]="apellido" name="apellido" required />
              </span>
            </label>
          </div>

          <div class="fila-2">
            <label class="field" for="cedula">
              <span class="field__label">Cédula</span>
              <span class="field__control">
                <input id="cedula" type="text" [(ngModel)]="cedula" name="cedula"
                       required pattern="\\d{10}" inputmode="numeric" maxlength="10" />
              </span>
            </label>
            <label class="field" for="fechaNacimiento">
              <span class="field__label">Fecha de nacimiento</span>
              <span class="field__control">
                <input id="fechaNacimiento" type="date" [(ngModel)]="fechaNacimiento" name="fechaNacimiento" required />
              </span>
            </label>
          </div>

          <label class="field" for="correo">
            <span class="field__label">Correo de contacto</span>
            <span class="field__control">
              <input id="correo" type="email" [(ngModel)]="correo" name="correo" required />
            </span>
          </label>
        }

        <div class="fila-2">
          <label class="field" for="username">
            <span class="field__label">Usuario (correo de acceso)</span>
            <span class="field__control">
              <input id="username" type="email" [(ngModel)]="username" name="username" required />
            </span>
          </label>
          <label class="field" for="password">
            <span class="field__label">Contraseña</span>
            <span class="field__control">
              <input id="password" type="password" [(ngModel)]="password" name="password" required minlength="6" />
            </span>
          </label>
        </div>

        <label class="field" for="rol">
          <span class="field__label">Rol</span>
          <span class="field__control">
            <select id="rol" [ngModel]="rol" (ngModelChange)="onRolChange($event)" name="rol">
              @for (r of roles; track r) {
                <option [value]="r">{{ r }}</option>
              }
            </select>
          </span>
        </label>

        @if (rol === 'REPRESENTANTE') {
          <div class="bloque-representante">
            <div class="fila-2">
              <label class="field" for="parentesco">
                <span class="field__label">Parentesco</span>
                <span class="field__control">
                  <input id="parentesco" type="text" [(ngModel)]="parentesco" name="parentesco" placeholder="Madre, padre, tutor…" />
                </span>
              </label>
              <label class="field" for="telefonoContacto">
                <span class="field__label">Teléfono de contacto</span>
                <span class="field__control">
                  <input id="telefonoContacto" type="tel" [(ngModel)]="telefonoContacto" name="telefonoContacto" />
                </span>
              </label>
            </div>

            <span class="field__label">Representados</span>
            @if (cargandoEstudiantes()) {
              <p class="aviso">Cargando estudiantes…</p>
            } @else {
              <div class="lista-estudiantes">
                @for (e of estudiantes(); track e.idEstudiante) {
                  <label class="opcion-estudiante">
                    <input type="checkbox" [checked]="idsSeleccionados().has(e.idEstudiante)"
                           (change)="alternarEstudiante(e.idEstudiante)" />
                    {{ e.nombreCompleto }} · {{ e.categoria }}
                  </label>
                }
              </div>
            }
          </div>
        }

        @if (rol === 'ESTUDIANTE') {
          <div class="bloque-representante">
            <label class="field" for="idEstudianteSeleccionado">
              <span class="field__label">Estudiante</span>
              <span class="field__control">
                @if (cargandoEstudiantes()) {
                  <span class="aviso">Cargando estudiantes…</span>
                } @else {
                  <select id="idEstudianteSeleccionado" [(ngModel)]="idEstudianteSeleccionado" name="idEstudianteSeleccionado" required>
                    <option value="" disabled>Selecciona un estudiante…</option>
                    @for (e of estudiantes(); track e.idEstudiante) {
                      <option [value]="e.idEstudiante">{{ e.nombreCompleto }} · {{ e.categoria }}</option>
                    }
                  </select>
                }
              </span>
              <span class="field__hint">Si el estudiante ya tiene una cuenta, el servidor lo rechaza al guardar.</span>
            </label>
          </div>
        }

        @if (error()) {
          <div class="alert alert--danger" role="alert">{{ error() }}</div>
        }
        @if (exito()) {
          <div class="alert alert--success" role="status">{{ exito() }}</div>
        }

        <button class="btn btn--primary btn--block" type="submit" [disabled]="guardando()">
          @if (guardando()) { <span class="spinner"></span> Creando… } @else { Crear usuario }
        </button>
      </form>
    </div>
  `,
  styles: [`
    .pantalla { max-width: 620px; margin: 0 auto; padding: 1.5rem 1.25rem 3rem; }
    .titulo-pantalla { font-size: 1.2rem; margin-bottom: 1.1rem; }

    .formulario { padding: 1.5rem; }
    .fila-2 { display: grid; grid-template-columns: 1fr 1fr; gap: .85rem; }
    @media (max-width: 480px) { .fila-2 { grid-template-columns: 1fr; } }

    .field__control select {
      flex: 1; border: none; outline: none; padding: .75rem 0; font-size: .95rem;
      background: transparent; color: var(--color-text); width: 100%;
    }

    .bloque-representante {
      border-top: 1px dashed var(--color-border); margin-top: .5rem; padding-top: 1.1rem;
    }

    .aviso { color: var(--color-text-muted); font-size: .85rem; }

    .lista-estudiantes {
      max-height: 220px; overflow-y: auto; border: 1px solid var(--color-border);
      border-radius: var(--radius-sm); padding: .5rem .75rem; margin-bottom: 1rem;
    }
    .opcion-estudiante {
      display: flex; align-items: center; gap: .55rem; padding: .4rem 0; font-size: .87rem; cursor: pointer;
    }
  `]
})
export class CrearUsuarioComponent implements OnInit {

  private readonly authService = inject(AuthService);
  private readonly adminService = inject(AdminService);

  readonly roles = ROLES;

  nombre = '';
  apellido = '';
  cedula = '';
  correo = '';
  fechaNacimiento = '';
  username = '';
  password = '';
  parentesco = '';
  telefonoContacto = '';
  idEstudianteSeleccionado = '';

  /**
   * Plain property, no signal: es el destino de un binding de ngModel
   * ([ngModel]="rol" + (ngModelChange)="onRolChange($event)"), y ese evento
   * de plantilla ya dispara deteccion de cambios por si mismo en esta app
   * zoneless -a diferencia de un callback de HttpClient.subscribe(), que no
   * pasa por el sistema de eventos de Angular y si necesita un signal-.
   */
  rol = 'USER';

  readonly guardando = signal(false);
  readonly error = signal('');
  readonly exito = signal('');

  readonly estudiantes = signal<EstudianteOpcion[]>([]);
  readonly cargandoEstudiantes = signal(false);
  readonly idsSeleccionados = signal<Set<number>>(new Set());

  private estudiantesCargados = false;

  ngOnInit(): void {}

  onRolChange(nuevoRol: string): void {
    this.rol = nuevoRol;
    if ((nuevoRol === 'REPRESENTANTE' || nuevoRol === 'ESTUDIANTE') && !this.estudiantesCargados) {
      this.cargarEstudiantes();
    }
  }

  private cargarEstudiantes(): void {
    this.estudiantesCargados = true;
    this.cargandoEstudiantes.set(true);
    this.adminService.listarEstudiantes().subscribe({
      next: (estudiantes) => {
        this.estudiantes.set(estudiantes);
        this.cargandoEstudiantes.set(false);
      },
      error: () => {
        this.cargandoEstudiantes.set(false);
      },
    });
  }

  alternarEstudiante(idEstudiante: number): void {
    const actuales = new Set(this.idsSeleccionados());
    if (actuales.has(idEstudiante)) {
      actuales.delete(idEstudiante);
    } else {
      actuales.add(idEstudiante);
    }
    this.idsSeleccionados.set(actuales);
  }

  onSubmit(): void {
    if (this.guardando()) return;
    this.guardando.set(true);
    this.error.set('');
    this.exito.set('');

    if (this.rol === 'ESTUDIANTE') {
      const idEstudiante = Number(this.idEstudianteSeleccionado);
      this.adminService.habilitarAccesoEstudiante(idEstudiante, {
        username: this.username,
        password: this.password,
      }).subscribe({
        next: () => this.finalizarConExito(),
        error: (err) => {
          this.guardando.set(false);
          this.error.set(this.mensajeDeErrorAcceso(err.status));
        },
      });
      return;
    }

    this.authService.register({
      nombre: this.nombre,
      apellido: this.apellido,
      cedula: this.cedula,
      correo: this.correo,
      fechaNacimiento: this.fechaNacimiento,
      username: this.username,
      password: this.password,
      rol: this.rol,
    }).subscribe({
      next: (cuenta) => {
        if (this.rol === 'REPRESENTANTE' && cuenta.idPersona && cuenta.idUsuario) {
          this.adminService.crearRepresentante({
            idPersona: cuenta.idPersona,
            idUsuario: cuenta.idUsuario,
            parentesco: this.parentesco || null,
            telefonoContacto: this.telefonoContacto || null,
            idsEstudiantesIniciales: Array.from(this.idsSeleccionados()),
          }).subscribe({
            next: () => this.finalizarConExito(),
            error: () => {
              this.guardando.set(false);
              this.error.set('La cuenta se creó, pero no se pudo vincular con los representados. Vincúlalos manualmente.');
            },
          });
          return;
        }
        this.finalizarConExito();
      },
      error: (err) => {
        this.guardando.set(false);
        this.error.set(this.mensajeDeError(err.status));
      },
    });
  }

  private finalizarConExito(): void {
    this.guardando.set(false);
    this.exito.set(`Cuenta creada: ${this.username} (${this.rol})`);
    this.nombre = '';
    this.apellido = '';
    this.cedula = '';
    this.correo = '';
    this.fechaNacimiento = '';
    this.username = '';
    this.password = '';
    this.parentesco = '';
    this.telefonoContacto = '';
    this.idEstudianteSeleccionado = '';
    this.rol = 'USER';
    this.idsSeleccionados.set(new Set());
  }

  private mensajeDeError(status: number): string {
    switch (status) {
      case 409: return 'Ya existe una cuenta con ese usuario, cédula o correo';
      case 422: return 'Revisa los datos: la cédula debe tener 10 dígitos y la fecha de nacimiento ser pasada';
      case 400: return 'Ese rol no existe';
      default: return 'Error del servidor';
    }
  }

  private mensajeDeErrorAcceso(status: number): string {
    switch (status) {
      case 400: return 'Ese estudiante ya tiene una cuenta, o el usuario ya está en uso';
      case 404: return 'No se encontró el estudiante seleccionado';
      case 422: return 'Revisa el usuario y la contraseña (mínimo 6 caracteres)';
      default: return 'Error del servidor';
    }
  }
}
