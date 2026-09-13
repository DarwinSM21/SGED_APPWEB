import { Component, OnInit, inject, signal } from '@angular/core';
import { CargandoComponent } from '../../core/cargando.component';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PersonasService, ESTADO_GENERAL_ACTIVO } from './personas.service';
import { CategoriaOpcion, EstudianteResponse } from './personas.models';
import { mensajeDeError as traducirError } from '../../core/mensaje-error';

@Component({
  selector: 'app-personas-recepcion',
  standalone: true,
  imports: [CommonModule, FormsModule, CargandoComponent],
  template: `
    <div class="pantalla">
      <h1 class="titulo-pantalla">{{ editando() ? 'Editar estudiante' : 'Registrar estudiante' }}</h1>

      <form class="card formulario" (ngSubmit)="onSubmit()">
        @if (!editando()) {
          <div class="fila-2">
            <label class="field" for="nombre"><span class="field__label">Nombre</span>
              <span class="field__control"><input id="nombre" [(ngModel)]="name" name="nombre" required /></span></label>
            <label class="field" for="apellido"><span class="field__label">Apellido</span>
              <span class="field__control"><input id="apellido" [(ngModel)]="lastName" name="apellido" required /></span></label>
          </div>
          <div class="fila-2">
            <label class="field" for="cedula"><span class="field__label">Cédula</span>
              <span class="field__control"><input id="cedula" [(ngModel)]="nationalId" name="cedula" required pattern="\\d{10}" inputmode="numeric" maxlength="10" /></span></label>
            <label class="field" for="fechaNacimiento"><span class="field__label">Fecha de nacimiento</span>
              <span class="field__control"><input id="fechaNacimiento" type="date" [(ngModel)]="birthDate" name="fechaNacimiento" required /></span></label>
          </div>
          <div class="fila-2">
            <label class="field" for="correo"><span class="field__label">Correo de contacto</span>
              <span class="field__control"><input id="correo" type="email" [(ngModel)]="email" name="correo" required /></span></label>
            <label class="field" for="telefono"><span class="field__label">Teléfono</span>
              <span class="field__control"><input id="telefono" type="tel" [(ngModel)]="phone" name="telefono" /></span></label>
          </div>
        } @else {
          <p class="aviso-edicion">Editando a <strong>{{ name }} {{ lastName }}</strong> (los datos personales no se modifican aquí).</p>
        }

        <div class="fila-2">
          <label class="field" for="idCategoria"><span class="field__label">Categoría</span>
            <span class="field__control">
              @if (cargandoCategorias()) { <span class="aviso">Cargando…</span> } @else {
                <select id="idCategoria" [(ngModel)]="categoryId" name="idCategoria" required>
                  <option [ngValue]="null" disabled>Selecciona…</option>
                  @for (c of categorias(); track c.categoryId) { <option [ngValue]="c.categoryId">{{ c.name }}</option> }
                </select>
              }
            </span></label>
          <label class="field" for="codigoEstudiante"><span class="field__label">Código de estudiante</span>
            <span class="field__control"><input id="codigoEstudiante" [(ngModel)]="studentCode" name="codigoEstudiante" required maxlength="30" /></span></label>
        </div>

        <div class="fila-2">
          <label class="field" for="fechaIngreso"><span class="field__label">Fecha de ingreso</span>
            <span class="field__control"><input id="fechaIngreso" type="date" [(ngModel)]="enrollmentDate" name="fechaIngreso" required /></span></label>
        </div>

        <div class="fila-2">
          <label class="field" for="peso"><span class="field__label">Peso (kg)</span>
            <span class="field__control"><input id="peso" type="number" step="0.01" min="0.01" [(ngModel)]="weight" name="peso" /></span></label>
          <label class="field" for="altura"><span class="field__label">Altura (m)</span>
            <span class="field__control"><input id="altura" type="number" step="0.01" min="0.01" [(ngModel)]="height" name="altura" /></span></label>
        </div>

        @if (error()) { <div class="alert alert--danger" role="alert">{{ error() }}</div> }
        @if (exito()) { <div class="alert alert--success" role="status">{{ exito() }}</div> }

        <div class="acciones">
          @if (editando()) { <button class="btn btn--ghost" type="button" (click)="cancelarEdicion()">Cancelar edición</button> }
          <button class="btn btn--primary" type="submit" [disabled]="guardando()">
            @if (guardando()) { <span class="spinner"></span> Guardando… } @else { {{ editando() ? 'Guardar cambios' : 'Registrar estudiante' }} }
          </button>
        </div>
      </form>

      <div class="card lista">
        <h2 class="subtitulo">Estudiantes registrados</h2>
        @if (cargandoLista()) {
          <app-cargando />
        } @else {
          @for (e of students(); track e.studentId) {
            <div class="fila-estudiante">
              <div class="fila-estudiante__principal">
                <span class="nombre-estudiante">{{ e.personName }} {{ e.personLastName }}</span>
                <span class="categoria-estudiante">{{ e.categoryName }} · {{ e.studentCode }}</span>
                <button class="btn btn--ghost btn--sm" type="button" (click)="editar(e)">Editar</button>
                <button class="btn btn--ghost btn--sm" type="button" (click)="alternarAccion(e.studentId, 'representante')">+ Representante</button>
                <button class="btn btn--ghost btn--sm" type="button" (click)="alternarAccion(e.studentId, 'acceso')">+ Acceso</button>
              </div>

              @if (accionAbierta() === e.studentId && subaccion() === 'representante') {
                <div class="subformulario">
                  <div class="fila-2">
                    <label class="field" for="rep-nombre-{{e.studentId}}"><span class="field__label">Nombre</span>
                      <span class="field__control"><input [id]="'rep-nombre-'+e.studentId" [(ngModel)]="formRepresentante.name" [name]="'rep-nombre-'+e.studentId" /></span></label>
                    <label class="field" for="rep-apellido-{{e.studentId}}"><span class="field__label">Apellido</span>
                      <span class="field__control"><input [id]="'rep-apellido-'+e.studentId" [(ngModel)]="formRepresentante.lastName" [name]="'rep-apellido-'+e.studentId" /></span></label>
                  </div>
                  <div class="fila-2">
                    <label class="field" for="rep-cedula-{{e.studentId}}"><span class="field__label">Cédula</span>
                      <span class="field__control"><input [id]="'rep-cedula-'+e.studentId" [(ngModel)]="formRepresentante.nationalId" [name]="'rep-cedula-'+e.studentId" pattern="\\d{10}" maxlength="10" /></span></label>
                    <label class="field" for="rep-correo-{{e.studentId}}"><span class="field__label">Correo</span>
                      <span class="field__control"><input [id]="'rep-correo-'+e.studentId" type="email" [(ngModel)]="formRepresentante.email" [name]="'rep-correo-'+e.studentId" /></span></label>
                  </div>
                  <div class="fila-2">
                    <label class="field" for="rep-fecha-{{e.studentId}}"><span class="field__label">Fecha de nacimiento</span>
                      <span class="field__control"><input [id]="'rep-fecha-'+e.studentId" type="date" [(ngModel)]="formRepresentante.birthDate" [name]="'rep-fecha-'+e.studentId" /></span></label>
                    <label class="field" for="rep-parentesco-{{e.studentId}}"><span class="field__label">Parentesco</span>
                      <span class="field__control"><input [id]="'rep-parentesco-'+e.studentId" [(ngModel)]="formRepresentante.relationship" [name]="'rep-parentesco-'+e.studentId" placeholder="Madre, padre, tutor…" /></span></label>
                  </div>
                  <div class="fila-2">
                    <label class="field" for="rep-username-{{e.studentId}}"><span class="field__label">Usuario (correo de acceso)</span>
                      <span class="field__control"><input [id]="'rep-username-'+e.studentId" type="email" [(ngModel)]="formRepresentante.username" [name]="'rep-username-'+e.studentId" /></span></label>
                    <label class="field" for="rep-password-{{e.studentId}}"><span class="field__label">Contraseña</span>
                      <span class="field__control"><input [id]="'rep-password-'+e.studentId" type="password" [(ngModel)]="formRepresentante.password" [name]="'rep-password-'+e.studentId" minlength="6" /></span></label>
                  </div>
                  @if (errorAccion()) { <div class="alert alert--danger" role="alert">{{ errorAccion() }}</div> }
                  <div class="acciones">
                    <button class="btn btn--primary btn--sm" type="button" [disabled]="guardandoAccion()" (click)="crearRepresentante(e.studentId)">
                      @if (guardandoAccion()) { <span class="spinner"></span> Vinculando… } @else { Vincular representante }
                    </button>
                  </div>
                </div>
              }

              @if (accionAbierta() === e.studentId && subaccion() === 'acceso') {
                <div class="subformulario">
                  <div class="fila-2">
                    <label class="field" for="acc-username-{{e.studentId}}"><span class="field__label">Usuario (correo de acceso)</span>
                      <span class="field__control"><input [id]="'acc-username-'+e.studentId" type="email" [(ngModel)]="formAcceso.username" [name]="'acc-username-'+e.studentId" /></span></label>
                    <label class="field" for="acc-password-{{e.studentId}}"><span class="field__label">Contraseña</span>
                      <span class="field__control"><input [id]="'acc-password-'+e.studentId" type="password" [(ngModel)]="formAcceso.password" [name]="'acc-password-'+e.studentId" minlength="6" /></span></label>
                  </div>
                  @if (errorAccion()) { <div class="alert alert--danger" role="alert">{{ errorAccion() }}</div> }
                  <div class="acciones">
                    <button class="btn btn--primary btn--sm" type="button" [disabled]="guardandoAccion()" (click)="habilitarAcceso(e.studentId)">
                      @if (guardandoAccion()) { <span class="spinner"></span> Habilitando… } @else { Habilitar acceso }
                    </button>
                  </div>
                </div>
              }
            </div>
          }
        }
      </div>
    </div>
  `,
  styles: [`
    .pantalla { max-width: 760px; margin: 0 auto; padding: 1.5rem 1.25rem 3rem; display: flex; flex-direction: column; gap: 1.25rem; }
    .titulo-pantalla { font-size: 1.2rem; }
    .subtitulo { font-size: .95rem; margin-bottom: .75rem; }
    .formulario { padding: 1.5rem; display: flex; flex-direction: column; gap: .9rem; }
    .fila-2 { display: grid; grid-template-columns: 1fr 1fr; gap: .85rem; }
    @media (max-width: 480px) { .fila-2 { grid-template-columns: 1fr; } }
    .field__control select { flex: 1; border: none; outline: none; padding: .75rem 0; font-size: .95rem; background: transparent; color: var(--color-text); width: 100%; }
    .aviso-edicion { font-size: .88rem; color: var(--color-text-muted); background: var(--color-neutral-bg); padding: .7rem .85rem; border-radius: var(--radius-sm); }
    .aviso { color: var(--color-text-muted); font-size: .85rem; }
    .acciones { display: flex; justify-content: flex-end; gap: .6rem; }
    .lista { padding: 1.25rem 1.5rem; }
    .fila-estudiante { padding: .6rem 0; border-bottom: 1px solid var(--color-border-light); font-size: .88rem; }
    .fila-estudiante:last-child { border-bottom: none; }
    .fila-estudiante__principal { display: flex; align-items: center; gap: .6rem; flex-wrap: wrap; }
    .name-estudiante { font-weight: 600; flex: 1; min-width: 120px; }
    .category-estudiante { color: var(--color-text-muted); }
    .btn--sm { padding: .35rem .6rem; font-size: .78rem; }
    .subformulario { margin-top: .75rem; padding: .9rem; border: 1px dashed var(--color-border); border-radius: var(--radius-sm); display: flex; flex-direction: column; gap: .7rem; }
  `],
})
export class PersonasRecepcionComponent implements OnInit {
  private readonly servicio = inject(PersonasService);

  name = ''; lastName = ''; nationalId = ''; email = ''; phone = ''; birthDate = '';
  categoryId: number | null = null;
  studentCode = '';
  enrollmentDate = new Date().toISOString().slice(0, 10);
  weight: number | null = null;
  height: number | null = null;

  readonly categorias = signal<CategoriaOpcion[]>([]);
  readonly cargandoCategorias = signal(true);
  readonly students = signal<EstudianteResponse[]>([]);
  readonly cargandoLista = signal(true);

  readonly editando = signal<number | null>(null);
  readonly guardando = signal(false);
  readonly error = signal('');
  readonly exito = signal('');

  readonly accionAbierta = signal<number | null>(null);
  readonly subaccion = signal<'representante' | 'acceso' | null>(null);
  readonly guardandoAccion = signal(false);
  readonly errorAccion = signal('');

  formRepresentante = { name: '', lastName: '', nationalId: '', email: '', birthDate: '', relationship: '', username: '', password: '' };
  formAcceso = { username: '', password: '' };

  ngOnInit(): void {
    this.servicio.categoriasActivas().subscribe({
      next: (categorias) => { this.categorias.set(categorias); this.cargandoCategorias.set(false); },
      error: () => this.cargandoCategorias.set(false),
    });
    this.cargarLista();
    this.servicio.siguienteCodigoEstudiante(new Date().getFullYear()).subscribe({
      next: (codigo) => { if (!this.studentCode) this.studentCode = codigo; },
      error: () => {},
    });
  }

  private cargarLista(): void {
    this.cargandoLista.set(true);
    this.servicio.listarEstudiantes().subscribe({
      next: (pagina) => { this.students.set(pagina.content); this.cargandoLista.set(false); },
      error: () => this.cargandoLista.set(false),
    });
  }

  editar(e: EstudianteResponse): void {
    this.editando.set(e.studentId);
    this.name = e.personName; this.lastName = e.personLastName;
    this.categoryId = e.categoryId; this.studentCode = e.studentCode;
    this.enrollmentDate = e.enrollmentDate; this.weight = e.weight; this.height = e.height;
    this.error.set(''); this.exito.set('');
  }

  cancelarEdicion(): void {
    this.editando.set(null);
    this.limpiarFormulario();
  }

  onSubmit(): void {
    if (this.guardando() || this.categoryId === null) return;
    this.guardando.set(true);
    this.error.set(''); this.exito.set('');

    const idEdicion = this.editando();
    if (idEdicion !== null) {
      const estudianteActual = this.students().find((e) => e.studentId === idEdicion);
      if (!estudianteActual) { this.guardando.set(false); return; }
      this.servicio.editarEstudiante(idEdicion, {
        personId: estudianteActual.personId, categoryId: this.categoryId, generalStatusId: estudianteActual.generalStatusId,
        studentCode: this.studentCode, enrollmentDate: this.enrollmentDate, weight: this.weight, height: this.height,
        positionId: estudianteActual.positionId,
      }).subscribe({
        next: () => { this.editando.set(null); this.finalizarConExito('Estudiante actualizado'); },
        error: (err) => this.manejarError(err),
      });
      return;
    }

    this.servicio.crearPersona({
      name: this.name, lastName: this.lastName, nationalId: this.nationalId,
      email: this.email, phone: this.phone || null, photo: null, birthDate: this.birthDate,
    }).subscribe({
      next: (persona) => {
        this.servicio.crearEstudiante({
          personId: persona.personId, categoryId: this.categoryId!, generalStatusId: ESTADO_GENERAL_ACTIVO,
          studentCode: this.studentCode, enrollmentDate: this.enrollmentDate, weight: this.weight, height: this.height,
          positionId: null,
        }).subscribe({
          next: () => this.finalizarConExito('Estudiante registrado'),
          error: (err) => { this.guardando.set(false); this.error.set('La persona se creó, pero no se pudo registrar como estudiante: ' + this.mensajeDeError(err)); },
        });
      },
      error: (err) => this.manejarError(err),
    });
  }

  alternarAccion(idEstudiante: number, type: 'representante' | 'acceso'): void {
    if (this.accionAbierta() === idEstudiante && this.subaccion() === type) {
      this.accionAbierta.set(null); this.subaccion.set(null);
      return;
    }
    this.accionAbierta.set(idEstudiante);
    this.subaccion.set(type);
    this.errorAccion.set('');
    this.formRepresentante = { name: '', lastName: '', nationalId: '', email: '', birthDate: '', relationship: '', username: '', password: '' };
    this.formAcceso = { username: '', password: '' };
  }

  crearRepresentante(idEstudiante: number): void {
    this.guardandoAccion.set(true);
    this.errorAccion.set('');
    this.servicio.crearPersona({
      name: this.formRepresentante.name, lastName: this.formRepresentante.lastName, nationalId: this.formRepresentante.nationalId,
      email: this.formRepresentante.email, phone: null, photo: null, birthDate: this.formRepresentante.birthDate,
    }).subscribe({
      next: (persona) => {
        this.servicio.crearUsuario({
          personId: persona.personId, generalStatusId: ESTADO_GENERAL_ACTIVO,
          username: this.formRepresentante.username, password: this.formRepresentante.password, role: 'REPRESENTANTE',
        }).subscribe({
          next: (usuario) => {
            this.servicio.crearRepresentante({
              personId: persona.personId, userId: usuario.userId,
              relationship: this.formRepresentante.relationship || null, contactPhone: null,
              initialStudentIds: [idEstudiante],
            }).subscribe({
              next: () => { this.guardandoAccion.set(false); this.accionAbierta.set(null); this.subaccion.set(null); this.exito.set('Representante vinculado'); },
              error: (err) => this.manejarErrorAccion(err),
            });
          },
          error: (err) => this.manejarErrorAccion(err),
        });
      },
      error: (err) => this.manejarErrorAccion(err),
    });
  }

  habilitarAcceso(idEstudiante: number): void {
    this.guardandoAccion.set(true);
    this.errorAccion.set('');
    this.servicio.habilitarAccesoEstudiante(idEstudiante, {
      username: this.formAcceso.username, password: this.formAcceso.password,
    }).subscribe({
      next: () => { this.guardandoAccion.set(false); this.accionAbierta.set(null); this.subaccion.set(null); this.exito.set('Acceso habilitado'); },
      error: (err) => this.manejarErrorAccion(err),
    });
  }

  private manejarErrorAccion(err: any): void {
    this.guardandoAccion.set(false);
    this.errorAccion.set(this.mensajeDeError(err));
  }

  private manejarError(err: any): void {
    this.guardando.set(false);
    this.error.set(this.mensajeDeError(err));
  }

  private mensajeDeError(err: any): string {
    const porDefecto = err?.status === 422 ? 'Revisa los datos: hay campos con formato inválido'
      : err?.status === 409 ? 'Ya existe un registro con esos datos'
      : 'Error del servidor';

    return traducirError(err, porDefecto);
  }

  private finalizarConExito(mensaje: string): void {
    this.guardando.set(false);
    this.exito.set(mensaje);
    this.limpiarFormulario();
    this.cargarLista();
  }

  private limpiarFormulario(): void {
    this.name = ''; this.lastName = ''; this.nationalId = ''; this.email = ''; this.phone = ''; this.birthDate = '';
    this.categoryId = null; this.studentCode = ''; this.enrollmentDate = new Date().toISOString().slice(0, 10);
    this.weight = null; this.height = null;
  }
}
