import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PersonasService, ESTADO_GENERAL_ACTIVO } from './personas.service';
import {
  CategoriaOpcion, EntrenadorResponse, EspecialidadOpcion, EstudianteResponse, PersonaConEstado, PersonaResponse,
  RepresentanteResponse, RolUsuario, ROLES_USUARIO, UsuarioResponse,
} from './personas.models';

type FormularioPersona = {
  nombre: string; apellido: string; cedula: string; correo: string; telefono: string; fechaNacimiento: string;
};

const PERSONA_VACIA: FormularioPersona = { nombre: '', apellido: '', cedula: '', correo: '', telefono: '', fechaNacimiento: '' };

type Tab = 'personas' | 'usuarios' | 'estudiantes' | 'entrenadores' | 'representantes';
const ETIQUETA_TAB: Record<Tab, string> = {
  personas: 'Personas', usuarios: 'Usuarios', estudiantes: 'Estudiantes',
  entrenadores: 'Entrenadores', representantes: 'Representantes',
};

/**
 * Vista de ADMINISTRADOR: maestro-detalle sobre Persona, la raiz real del
 * dato (Usuario/Estudiante/Entrenador/Representante cuelgan de ella por
 * FK). Sin endpoint agregador nuevo: PersonasService cruza en el cliente
 * las 5 listas que ya existen -- ver personas.service.ts.
 */
@Component({
  selector: 'app-personas-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  template: `
    <div class="tabs">
      @for (t of tabs; track t) {
        <button type="button" class="tab" [class.tab--activo]="tabActiva() === t" (click)="tabActiva.set(t)">
          {{ etiquetaTab(t) }}
        </button>
      }
    </div>

    @if (tabActiva() === 'personas') {
    <div class="maestro-detalle">
      <div class="card panel-lista">
        <input class="buscador" type="search" placeholder="Buscar por nombre o cédula…"
               [ngModel]="busqueda()" (ngModelChange)="busqueda.set($event)" name="busqueda" />
        <button class="btn btn--primary btn--block" type="button" (click)="nuevaPersona()">+ Nueva persona</button>

        @if (cargando()) {
          <p class="aviso">Cargando…</p>
        } @else {
          <div class="lista-personas">
            @for (p of personasFiltradas(); track p.persona.idPersona) {
              <button type="button" class="fila-persona" [class.fila-persona--activa]="seleccionada()?.persona?.idPersona === p.persona.idPersona"
                      (click)="seleccionar(p)">
                <span class="nombre-persona">{{ p.persona.nombre }} {{ p.persona.apellido }}</span>
                <span class="cedula-persona">{{ p.persona.cedula }}</span>
                <span class="badges-persona">
                  @if (p.usuario) { <span class="badge badge--info">{{ p.usuario.roles.length ? p.usuario.roles[0] : 'sin rol' }}</span> }
                  @if (p.estudiante) { <span class="badge badge--success">Estudiante</span> }
                  @if (p.entrenador) { <span class="badge badge--success">Entrenador</span> }
                  @if (p.representante) { <span class="badge badge--success">Representante</span> }
                </span>
              </button>
            }
          </div>
        }
      </div>

      <div class="card panel-detalle">
        @if (!mostrandoDetalle()) {
          <p class="aviso">Seleccioná una persona de la lista, o creá una nueva.</p>
        } @else {
          <h2 class="subtitulo">{{ esNueva() ? 'Nueva persona' : formPersona.nombre + ' ' + formPersona.apellido }}</h2>

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
                @if (guardandoPersona()) { <span class="spinner"></span> Guardando… } @else { {{ esNueva() ? 'Crear persona' : 'Guardar datos' }} }
              </button>
            </div>
          </form>

          @if (!esNueva()) {
            <!-- Usuario -->
            <div class="bloque bloque--separado">
              <h3 class="subtitulo-seccion">Cuenta de usuario</h3>
              @if (seleccionada()?.usuario; as u) {
                @if (!editandoUsuario()) {
                  <p class="resumen-seccion">{{ u.username }} · {{ u.roles.join(', ') || 'sin rol' }} · {{ u.activo ? 'activo' : 'inactivo' }}</p>
                  <div class="acciones">
                    <button class="btn btn--ghost btn--sm" type="button" (click)="iniciarEdicionUsuario(u)">Editar cuenta</button>
                    @if (u.activo) {
                      <button class="btn btn--ghost btn--sm" type="button" (click)="desactivarUsuario(u.idUsuario)">Desactivar cuenta</button>
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
                  @if (errorUsuario()) { <div class="alert alert--danger" role="alert">{{ errorUsuario() }}</div> }
                  <div class="acciones">
                    <button class="btn btn--ghost btn--sm" type="button" [disabled]="guardandoUsuario()" (click)="cancelarEdicionUsuario()">Cancelar</button>
                    <button class="btn btn--primary btn--sm" type="button" [disabled]="guardandoUsuario()" (click)="guardarEdicionUsuario(u.idUsuario)">
                      @if (guardandoUsuario()) { <span class="spinner"></span> Guardando… } @else { Guardar }
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
                @if (errorUsuario()) { <div class="alert alert--danger" role="alert">{{ errorUsuario() }}</div> }
                <div class="acciones">
                  <button class="btn btn--primary btn--sm" type="button" [disabled]="guardandoUsuario()" (click)="crearUsuario()">
                    @if (guardandoUsuario()) { <span class="spinner"></span> Creando… } @else { Crear cuenta }
                  </button>
                </div>
              }
            </div>

            <!-- Estudiante -->
            <div class="bloque bloque--separado">
              <h3 class="subtitulo-seccion">Ficha de estudiante</h3>
              @if (seleccionada()?.estudiante; as e) {
                <p class="resumen-seccion">{{ e.codigoEstudiante }} · {{ e.nombreCategoria }} · {{ e.activo ? 'activo' : 'inactivo' }}</p>

                <h4 class="subtitulo-menor">Representantes</h4>
                @if (representantesDelEstudiante().length === 0) {
                  <p class="aviso">Este estudiante todavía no tiene representantes asignados.</p>
                } @else {
                  <div class="lista-vinculos">
                    @for (v of representantesDelEstudiante(); track v.idRepresentante) {
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

                @if (representantesDisponibles().length > 0) {
                  <div class="fila-2">
                    <label class="field" for="v-representante"><span class="field__label">Agregar representante</span>
                      <span class="field__control">
                        <select id="v-representante" [(ngModel)]="formVinculo.idRepresentante" name="v-representante">
                          <option [ngValue]="null" disabled>Selecciona…</option>
                          @for (r of representantesDisponibles(); track r.idRepresentante) {
                            <option [ngValue]="r.idRepresentante">{{ r.nombre }} {{ r.apellido }}</option>
                          }
                        </select>
                      </span></label>
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
                } @else if (representantes().length === 0) {
                  <p class="aviso">No hay representantes registrados todavía.</p>
                }
              } @else {
                <div class="fila-2">
                  <label class="field" for="e-categoria"><span class="field__label">Categoría</span>
                    <span class="field__control">
                      <select id="e-categoria" [(ngModel)]="formEstudiante.idCategoria" name="e-categoria">
                        <option [ngValue]="null" disabled>Selecciona…</option>
                        @for (c of categorias(); track c.idCategoria) { <option [ngValue]="c.idCategoria">{{ c.nombre }}</option> }
                      </select>
                    </span></label>
                  <label class="field" for="e-codigo"><span class="field__label">Código</span>
                    <span class="field__control"><input id="e-codigo" [(ngModel)]="formEstudiante.codigoEstudiante" name="e-codigo" /></span></label>
                </div>
                <div class="fila-2">
                  <label class="field" for="e-ingreso"><span class="field__label">Fecha de ingreso</span>
                    <span class="field__control"><input id="e-ingreso" type="date" [(ngModel)]="formEstudiante.fechaIngreso" name="e-ingreso" /></span></label>
                </div>
                @if (errorEstudiante()) { <div class="alert alert--danger" role="alert">{{ errorEstudiante() }}</div> }
                <div class="acciones">
                  <button class="btn btn--primary btn--sm" type="button" [disabled]="guardandoEstudiante()" (click)="crearEstudiante()">
                    @if (guardandoEstudiante()) { <span class="spinner"></span> Creando… } @else { Crear ficha de estudiante }
                  </button>
                </div>
              }
            </div>

            <!-- Entrenador -->
            <div class="bloque bloque--separado">
              <h3 class="subtitulo-seccion">Entrenador</h3>
              @if (seleccionada()?.entrenador; as ent) {
                <p class="resumen-seccion">{{ ent.nombreEspecialidad || 'sin especialidad' }} · {{ ent.experienciaAnios ?? 0 }} años</p>
              } @else if (seleccionada()?.usuario; as u) {
                <div class="fila-2">
                  <label class="field" for="ent-especialidad"><span class="field__label">Especialidad</span>
                    <span class="field__control">
                      <select id="ent-especialidad" [(ngModel)]="formEntrenador.idEspecialidad" name="ent-especialidad">
                        <option [ngValue]="null">Sin especialidad</option>
                        @for (esp of especialidades(); track esp.idEspecialidad) { <option [ngValue]="esp.idEspecialidad">{{ esp.nombre }}</option> }
                      </select>
                    </span></label>
                  <label class="field" for="ent-experiencia"><span class="field__label">Años de experiencia</span>
                    <span class="field__control"><input id="ent-experiencia" type="number" min="0" [(ngModel)]="formEntrenador.experienciaAnios" name="ent-experiencia" /></span></label>
                </div>
                @if (errorEntrenador()) { <div class="alert alert--danger" role="alert">{{ errorEntrenador() }}</div> }
                <div class="acciones">
                  <button class="btn btn--primary btn--sm" type="button" [disabled]="guardandoEntrenador()" (click)="crearEntrenador()">
                    @if (guardandoEntrenador()) { <span class="spinner"></span> Registrando… } @else { Registrar como entrenador }
                  </button>
                </div>
              } @else {
                <p class="aviso">Primero creá una cuenta de usuario para poder registrarla como entrenador.</p>
              }
            </div>

            <!-- Representante -->
            <div class="bloque bloque--separado">
              <h3 class="subtitulo-seccion">Representante</h3>
              @if (seleccionada()?.representante; as rep) {
                <p class="resumen-seccion">{{ rep.parentesco || 'sin parentesco' }} · {{ rep.representados.length }} representado(s)</p>
              } @else if (seleccionada()?.usuario; as u) {
                <div class="fila-2">
                  <label class="field" for="rep-parentesco"><span class="field__label">Parentesco</span>
                    <span class="field__control"><input id="rep-parentesco" [(ngModel)]="formRepresentante.parentesco" name="rep-parentesco" placeholder="Madre, padre, tutor…" /></span></label>
                  <label class="field" for="rep-telefono"><span class="field__label">Teléfono de contacto</span>
                    <span class="field__control"><input id="rep-telefono" [(ngModel)]="formRepresentante.telefonoContacto" name="rep-telefono" /></span></label>
                </div>
                @if (errorRepresentante()) { <div class="alert alert--danger" role="alert">{{ errorRepresentante() }}</div> }
                <div class="acciones">
                  <button class="btn btn--primary btn--sm" type="button" [disabled]="guardandoRepresentante()" (click)="crearRepresentante()">
                    @if (guardandoRepresentante()) { <span class="spinner"></span> Registrando… } @else { Registrar como representante }
                  </button>
                </div>
              } @else {
                <p class="aviso">Primero creá una cuenta de usuario para poder registrarla como representante.</p>
              }
            </div>
          }
        }
      </div>
    </div>
    }

    @if (tabActiva() === 'usuarios') {
      <div class="card panel-gestion">
        <div class="barra-filtros">
          <input class="buscador" type="search" placeholder="Buscar por nombre o usuario…"
                 [ngModel]="busquedaUsuarios()" (ngModelChange)="busquedaUsuarios.set($event)" name="busquedaUsuarios" />
          <select class="filtro-rol" [ngModel]="filtroRolUsuarios()" (ngModelChange)="filtroRolUsuarios.set($event)" name="filtroRolUsuarios">
            <option value="TODOS">Todos los roles</option>
            @for (r of roles; track r) { <option [value]="r">{{ r }}</option> }
          </select>
          <label class="toggle-inactivos">
            <input type="checkbox" [ngModel]="mostrarInactivosUsuarios()" (ngModelChange)="mostrarInactivosUsuarios.set($event)" name="mostrarInactivosUsuarios" />
            Mostrar inactivos
          </label>
        </div>
        @if (usuariosFiltrados().length === 0) {
          <p class="aviso">No hay usuarios que coincidan.</p>
        } @else {
          <div class="lista-gestion">
            @for (u of usuariosFiltrados(); track u.idUsuario) {
              <button type="button" class="fila-gestion" (click)="irAPersona(u.idPersona)">
                <span class="col-principal">{{ u.nombrePersona }} {{ u.apellidoPersona }}</span>
                <span class="col-secundaria">{{ u.username }}</span>
                <span class="badges-persona">
                  @for (r of u.roles; track r) { <span class="badge badge--info">{{ r }}</span> }
                </span>
                <span class="col-secundaria">{{ u.estadoGeneralNombre }}</span>
                <span class="col-secundaria">{{ u.ultimoAcceso ? (u.ultimoAcceso | date:'short') : 'sin acceso aún' }}</span>
                <span class="badge" [class.badge--success]="u.activo" [class.badge--danger]="!u.activo">{{ u.activo ? 'Activo' : 'Inactivo' }}</span>
              </button>
            }
          </div>
        }
      </div>
    }

    @if (tabActiva() === 'estudiantes') {
      <div class="card panel-gestion">
        <div class="barra-filtros">
          <input class="buscador" type="search" placeholder="Buscar por nombre o código…"
                 [ngModel]="busquedaEstudiantes()" (ngModelChange)="busquedaEstudiantes.set($event)" name="busquedaEstudiantes" />
          <label class="toggle-inactivos">
            <input type="checkbox" [ngModel]="mostrarInactivosEstudiantes()" (ngModelChange)="mostrarInactivosEstudiantes.set($event)" name="mostrarInactivosEstudiantes" />
            Mostrar inactivos
          </label>
        </div>
        @if (estudiantesFiltrados().length === 0) {
          <p class="aviso">No hay estudiantes que coincidan.</p>
        } @else {
          <div class="lista-gestion">
            @for (e of estudiantesFiltrados(); track e.idEstudiante) {
              <button type="button" class="fila-gestion" (click)="irAPersona(e.idPersona)">
                <span class="col-principal">{{ e.nombrePersona }} {{ e.apellidoPersona }}</span>
                <span class="col-secundaria">{{ e.codigoEstudiante }}</span>
                <span class="col-secundaria">{{ e.nombreCategoria }}</span>
                <span class="col-secundaria">{{ e.fechaIngreso | date:'shortDate' }}</span>
                <span class="badge" [class.badge--success]="e.activo" [class.badge--danger]="!e.activo">{{ e.activo ? 'Activo' : 'Inactivo' }}</span>
              </button>
            }
          </div>
        }
      </div>
    }

    @if (tabActiva() === 'entrenadores') {
      <div class="card panel-gestion">
        <div class="barra-filtros">
          <input class="buscador" type="search" placeholder="Buscar por nombre o especialidad…"
                 [ngModel]="busquedaEntrenadores()" (ngModelChange)="busquedaEntrenadores.set($event)" name="busquedaEntrenadores" />
          <label class="toggle-inactivos">
            <input type="checkbox" [ngModel]="mostrarInactivosEntrenadores()" (ngModelChange)="mostrarInactivosEntrenadores.set($event)" name="mostrarInactivosEntrenadores" />
            Mostrar inactivos
          </label>
        </div>
        @if (entrenadoresFiltrados().length === 0) {
          <p class="aviso">No hay entrenadores que coincidan.</p>
        } @else {
          <div class="lista-gestion">
            @for (ent of entrenadoresFiltrados(); track ent.idEntrenador) {
              <button type="button" class="fila-gestion" (click)="irAPersona(ent.idPersona)">
                <span class="col-principal">{{ ent.nombre }} {{ ent.apellido }}</span>
                <span class="col-secundaria">{{ ent.nombreEspecialidad || 'sin especialidad' }}</span>
                <span class="col-secundaria">{{ ent.experienciaAnios ?? 0 }} años</span>
                <span class="col-secundaria">{{ ent.username }}</span>
                <span class="badge" [class.badge--success]="ent.activo" [class.badge--danger]="!ent.activo">{{ ent.activo ? 'Activo' : 'Inactivo' }}</span>
              </button>
            }
          </div>
        }
      </div>
    }

    @if (tabActiva() === 'representantes') {
      <div class="card panel-gestion">
        <div class="barra-filtros">
          <input class="buscador" type="search" placeholder="Buscar por nombre o parentesco…"
                 [ngModel]="busquedaRepresentantes()" (ngModelChange)="busquedaRepresentantes.set($event)" name="busquedaRepresentantes" />
          <label class="toggle-inactivos">
            <input type="checkbox" [ngModel]="mostrarInactivosRepresentantes()" (ngModelChange)="mostrarInactivosRepresentantes.set($event)" name="mostrarInactivosRepresentantes" />
            Mostrar inactivos
          </label>
        </div>
        @if (representantesFiltrados().length === 0) {
          <p class="aviso">No hay representantes que coincidan.</p>
        } @else {
          <div class="lista-gestion">
            @for (r of representantesFiltrados(); track r.idRepresentante) {
              <button type="button" class="fila-gestion" (click)="irAPersona(r.idPersona)">
                <span class="col-principal">{{ r.nombre }} {{ r.apellido }}</span>
                <span class="col-secundaria">{{ r.parentesco || 'sin parentesco' }}</span>
                <span class="col-secundaria">{{ r.telefonoContacto || 'sin teléfono' }}</span>
                <span class="col-secundaria">{{ r.representados.length }} representado{{ r.representados.length === 1 ? '' : 's' }}</span>
                <span class="badge" [class.badge--success]="r.activo" [class.badge--danger]="!r.activo">{{ r.activo ? 'Activo' : 'Inactivo' }}</span>
              </button>
            }
          </div>
        }
      </div>
    }
  `,
  styles: [`
    .tabs { display: flex; gap: .4rem; border-bottom: 1px solid var(--color-border-light); padding: 0 1.25rem; max-width: 1100px; margin: 0 auto; }
    .tab {
      border: none; background: none; padding: .75rem .9rem; font-size: .87rem; font-weight: 600;
      color: var(--color-text-muted); cursor: pointer; border-bottom: 2px solid transparent;
    }
    .tab--activo { color: var(--color-primary-700); border-bottom-color: var(--color-primary-500); }

    .panel-gestion { max-width: 1100px; margin: 1.25rem auto 3rem; padding: 1.25rem; display: flex; flex-direction: column; gap: .9rem; }
    .barra-filtros { display: flex; gap: .6rem; flex-wrap: wrap; align-items: center; }
    .barra-filtros .buscador { flex: 1; min-width: 220px; }
    .filtro-rol { padding: .6rem .75rem; border: 1px solid var(--color-border); border-radius: var(--radius-sm); font-size: .85rem; background: transparent; color: var(--color-text); }
    .toggle-inactivos { display: flex; align-items: center; gap: .4rem; font-size: .82rem; color: var(--color-text-muted); white-space: nowrap; }

    .lista-gestion { display: flex; flex-direction: column; gap: .1rem; }
    .fila-gestion {
      display: grid; grid-template-columns: 1.4fr 1fr 1fr 1fr 1fr auto; align-items: center; gap: .75rem;
      padding: .6rem .5rem; border: none; border-bottom: 1px solid var(--color-border-light); background: none;
      cursor: pointer; text-align: left; width: 100%; font-size: .85rem;
    }
    .fila-gestion:last-child { border-bottom: none; }
    .fila-gestion:hover { background: var(--color-border-light); }
    .col-principal { font-weight: 600; }
    .col-secundaria { color: var(--color-text-muted); }
    @media (max-width: 800px) { .fila-gestion { grid-template-columns: 1fr 1fr; } }

    .maestro-detalle { display: grid; grid-template-columns: 320px 1fr; gap: 1.25rem; max-width: 1100px; margin: 0 auto; padding: 1.5rem 1.25rem 3rem; align-items: start; }
    @media (max-width: 800px) { .maestro-detalle { grid-template-columns: 1fr; } }

    .panel-lista { padding: 1rem; display: flex; flex-direction: column; gap: .75rem; position: sticky; top: 1rem; max-height: calc(100vh - 2rem); overflow-y: auto; }
    .buscador { width: 100%; padding: .6rem .75rem; border: 1px solid var(--color-border); border-radius: var(--radius-sm); font-size: .88rem; background: transparent; color: var(--color-text); }

    .lista-personas { display: flex; flex-direction: column; gap: .2rem; }
    .fila-persona {
      display: flex; flex-direction: column; align-items: flex-start; gap: .2rem; padding: .55rem .6rem;
      border: none; background: none; border-radius: var(--radius-sm); cursor: pointer; text-align: left; width: 100%;
    }
    .fila-persona:hover { background: var(--color-border-light); }
    .fila-persona--activa { background: var(--color-primary-50); }
    .nombre-persona { font-weight: 600; font-size: .88rem; }
    .cedula-persona { font-size: .78rem; color: var(--color-text-faint); }
    .badges-persona { display: flex; gap: .3rem; flex-wrap: wrap; margin-top: .15rem; }

    .panel-detalle { padding: 1.5rem; display: flex; flex-direction: column; gap: 1rem; }
    .subtitulo { font-size: 1rem; }
    .subtitulo-seccion { font-size: .88rem; margin-bottom: .5rem; }
    .subtitulo-menor { font-size: .82rem; color: var(--color-text-muted); margin: .5rem 0 .35rem; }

    .lista-vinculos { display: flex; flex-direction: column; gap: .1rem; margin-bottom: .5rem; }
    .fila-vinculo {
      display: flex; align-items: center; gap: .6rem; padding: .4rem 0;
      border-bottom: 1px solid var(--color-border-light); font-size: .85rem;
    }
    .fila-vinculo:last-child { border-bottom: none; }
    .fila-vinculo .col-principal { flex: 1; font-weight: 600; }
    .aviso { color: var(--color-text-muted); font-size: .85rem; }
    .resumen-seccion { font-size: .85rem; color: var(--color-text-muted); margin-bottom: .5rem; }

    .bloque { display: flex; flex-direction: column; gap: .75rem; }
    .bloque--separado { border-top: 1px solid var(--color-border-light); padding-top: 1rem; }
    .fila-2 { display: grid; grid-template-columns: 1fr 1fr; gap: .75rem; }
    @media (max-width: 480px) { .fila-2 { grid-template-columns: 1fr; } }

    .field__control select { flex: 1; border: none; outline: none; padding: .7rem 0; font-size: .9rem; background: transparent; color: var(--color-text); width: 100%; }
    .acciones { display: flex; justify-content: flex-end; gap: .5rem; }
    .btn--sm { padding: .5rem .9rem; font-size: .85rem; }
  `],
})
export class PersonasAdminComponent implements OnInit {

  private readonly servicio = inject(PersonasService);

  readonly roles = ROLES_USUARIO;
  readonly tabs: Tab[] = ['personas', 'usuarios', 'estudiantes', 'entrenadores', 'representantes'];
  readonly tabActiva = signal<Tab>('personas');
  etiquetaTab(t: Tab): string { return ETIQUETA_TAB[t]; }

  readonly busquedaUsuarios = signal('');
  readonly filtroRolUsuarios = signal<RolUsuario | 'TODOS'>('TODOS');
  readonly mostrarInactivosUsuarios = signal(false);

  readonly busquedaEstudiantes = signal('');
  readonly mostrarInactivosEstudiantes = signal(false);

  readonly busquedaEntrenadores = signal('');
  readonly mostrarInactivosEntrenadores = signal(false);

  readonly busquedaRepresentantes = signal('');
  readonly mostrarInactivosRepresentantes = signal(false);

  // Cada lista se carga y falla de forma independiente (ver
  // personas.service.ts): una que tarde o falle no bloquea a las demas,
  // solo deja esa columna de estado vacia hasta el proximo refresco.
  readonly personasBase = signal<PersonaResponse[]>([]);
  readonly usuarios = signal<UsuarioResponse[]>([]);
  readonly estudiantes = signal<EstudianteResponse[]>([]);
  readonly entrenadores = signal<EntrenadorResponse[]>([]);
  readonly representantes = signal<RepresentanteResponse[]>([]);
  readonly cargando = signal(true);
  readonly busqueda = signal('');

  readonly personas = computed<PersonaConEstado[]>(() => {
    const usuarios = this.usuarios(), estudiantes = this.estudiantes(),
      entrenadores = this.entrenadores(), representantes = this.representantes();
    return this.personasBase().map((persona) => ({
      persona,
      usuario: usuarios.find((u) => u.idPersona === persona.idPersona) ?? null,
      estudiante: estudiantes.find((e) => e.idPersona === persona.idPersona) ?? null,
      entrenador: entrenadores.find((e) => e.idPersona === persona.idPersona) ?? null,
      representante: representantes.find((r) => r.idPersona === persona.idPersona) ?? null,
    }));
  });

  readonly personasFiltradas = computed(() => {
    const q = this.busqueda().trim().toLowerCase();
    if (!q) return this.personas();
    return this.personas().filter((p) =>
      `${p.persona.nombre} ${p.persona.apellido}`.toLowerCase().includes(q) || p.persona.cedula.includes(q));
  });

  readonly usuariosFiltrados = computed(() => {
    const q = this.busquedaUsuarios().trim().toLowerCase();
    const rol = this.filtroRolUsuarios();
    const conInactivos = this.mostrarInactivosUsuarios();
    return this.usuarios().filter((u) =>
      (conInactivos || u.activo) &&
      (rol === 'TODOS' || u.roles.includes(rol)) &&
      (!q || `${u.nombrePersona} ${u.apellidoPersona}`.toLowerCase().includes(q) || u.username.toLowerCase().includes(q)));
  });

  readonly estudiantesFiltrados = computed(() => {
    const q = this.busquedaEstudiantes().trim().toLowerCase();
    const conInactivos = this.mostrarInactivosEstudiantes();
    return this.estudiantes().filter((e) =>
      (conInactivos || e.activo) &&
      (!q || `${e.nombrePersona} ${e.apellidoPersona}`.toLowerCase().includes(q) || e.codigoEstudiante.toLowerCase().includes(q)));
  });

  readonly entrenadoresFiltrados = computed(() => {
    const q = this.busquedaEntrenadores().trim().toLowerCase();
    const conInactivos = this.mostrarInactivosEntrenadores();
    return this.entrenadores().filter((ent) =>
      (conInactivos || ent.activo) &&
      (!q || `${ent.nombre} ${ent.apellido}`.toLowerCase().includes(q) || (ent.nombreEspecialidad ?? '').toLowerCase().includes(q)));
  });

  readonly representantesFiltrados = computed(() => {
    const q = this.busquedaRepresentantes().trim().toLowerCase();
    const conInactivos = this.mostrarInactivosRepresentantes();
    return this.representantes().filter((r) =>
      (conInactivos || r.activo) &&
      (!q || `${r.nombre} ${r.apellido}`.toLowerCase().includes(q) || (r.parentesco ?? '').toLowerCase().includes(q)));
  });

  readonly seleccionada = signal<PersonaConEstado | null>(null);
  readonly esNueva = signal(false);
  readonly mostrandoDetalle = computed(() => this.seleccionada() !== null || this.esNueva());

  readonly categorias = signal<CategoriaOpcion[]>([]);
  readonly especialidades = signal<EspecialidadOpcion[]>([]);

  formPersona: FormularioPersona = { ...PERSONA_VACIA };
  readonly guardandoPersona = signal(false);
  readonly errorPersona = signal('');

  formUsuario: { username: string; password: string; rol: RolUsuario } = { username: '', password: '', rol: 'ENTRENADOR' };
  readonly guardandoUsuario = signal(false);
  readonly errorUsuario = signal('');
  readonly editandoUsuario = signal(false);

  formEstudiante: { idCategoria: number | null; codigoEstudiante: string; fechaIngreso: string } =
    { idCategoria: null, codigoEstudiante: '', fechaIngreso: new Date().toISOString().slice(0, 10) };
  readonly guardandoEstudiante = signal(false);
  readonly errorEstudiante = signal('');

  formEntrenador: { idEspecialidad: number | null; experienciaAnios: number | null } = { idEspecialidad: null, experienciaAnios: null };
  readonly guardandoEntrenador = signal(false);
  readonly errorEntrenador = signal('');

  formRepresentante: { parentesco: string; telefonoContacto: string } = { parentesco: '', telefonoContacto: '' };
  readonly guardandoRepresentante = signal(false);
  readonly errorRepresentante = signal('');

  formVinculo: { idRepresentante: number | null; relacion: string; contactoPrincipal: boolean } =
    { idRepresentante: null, relacion: '', contactoPrincipal: false };
  readonly guardandoVinculo = signal(false);
  readonly errorVinculo = signal('');

  /**
   * Los representantes del estudiante seleccionado, cruzados en el cliente
   * desde representados[] -- mismo criterio que el resto de la pantalla, sin
   * endpoint agregador nuevo. `relacion`/`contactoPrincipal` viven en el
   * vinculo, asi que se leen de la fila de representados que apunta a este
   * estudiante, no del representante.
   */
  readonly representantesDelEstudiante = computed(() => {
    const idEstudiante = this.seleccionada()?.estudiante?.idEstudiante;
    if (idEstudiante === undefined) return [];
    return this.representantes().flatMap((r) => {
      const vinculo = r.representados.find((e) => e.idEstudiante === idEstudiante);
      return vinculo
        ? [{ idRepresentante: r.idRepresentante, nombre: r.nombre, apellido: r.apellido,
             relacion: vinculo.relacion, contactoPrincipal: vinculo.contactoPrincipal }]
        : [];
    });
  });

  readonly representantesDisponibles = computed(() => {
    const yaVinculados = new Set(this.representantesDelEstudiante().map((v) => v.idRepresentante));
    return this.representantes().filter((r) => r.activo && !yaVinculados.has(r.idRepresentante));
  });

  ngOnInit(): void {
    this.cargarPersonas();
    this.servicio.categoriasActivas().subscribe({ next: (c) => this.categorias.set(c) });
    this.servicio.especialidadesActivas().subscribe({ next: (e) => this.especialidades.set(e) });
  }

  private cargarPersonas(mantenerSeleccion = false): void {
    this.cargando.set(true);
    const idSeleccionado = this.seleccionada()?.persona?.idPersona ?? null;

    this.servicio.listarPersonas().subscribe({
      next: (pagina) => { this.personasBase.set(pagina.content); this.cargando.set(false); this.reaplicarSeleccion(mantenerSeleccion, idSeleccionado); },
      error: () => this.cargando.set(false),
    });
    this.servicio.listarUsuarios().subscribe({
      next: (pagina) => { this.usuarios.set(pagina.content); this.reaplicarSeleccion(mantenerSeleccion, idSeleccionado); },
      error: () => {},
    });
    this.servicio.listarEstudiantes().subscribe({
      next: (pagina) => { this.estudiantes.set(pagina.content); this.reaplicarSeleccion(mantenerSeleccion, idSeleccionado); },
      error: () => {},
    });
    this.servicio.listarEntrenadores().subscribe({
      next: (pagina) => { this.entrenadores.set(pagina.content); this.reaplicarSeleccion(mantenerSeleccion, idSeleccionado); },
      error: () => {},
    });
    this.servicio.listarRepresentantes().subscribe({
      next: (pagina) => { this.representantes.set(pagina.content); this.reaplicarSeleccion(mantenerSeleccion, idSeleccionado); },
      error: () => {},
    });
  }

  /** La seleccion actual debe reflejar los datos mas frescos a medida que cada lista llega, no solo al final. */
  private reaplicarSeleccion(mantenerSeleccion: boolean, idSeleccionado: number | null): void {
    if (!mantenerSeleccion || idSeleccionado === null) return;
    const actualizada = this.personas().find((p) => p.persona.idPersona === idSeleccionado);
    if (actualizada) this.seleccionada.set(actualizada);
  }

  seleccionar(p: PersonaConEstado): void {
    this.esNueva.set(false);
    this.seleccionada.set(p);
    this.formPersona = {
      nombre: p.persona.nombre, apellido: p.persona.apellido, cedula: p.persona.cedula,
      correo: p.persona.correo, telefono: p.persona.telefono ?? '', fechaNacimiento: p.persona.fechaNacimiento,
    };
    this.resetFormulariosSecundarios();
  }

  irAPersona(idPersona: number): void {
    const p = this.personas().find((x) => x.persona.idPersona === idPersona);
    if (p) { this.tabActiva.set('personas'); this.seleccionar(p); }
  }

  nuevaPersona(): void {
    this.seleccionada.set(null);
    this.esNueva.set(true);
    this.formPersona = { ...PERSONA_VACIA };
    this.errorPersona.set('');
  }

  private resetFormulariosSecundarios(): void {
    this.formUsuario = { username: '', password: '', rol: 'ENTRENADOR' };
    this.formEstudiante = { idCategoria: null, codigoEstudiante: '', fechaIngreso: new Date().toISOString().slice(0, 10) };
    this.formEntrenador = { idEspecialidad: null, experienciaAnios: null };
    this.formRepresentante = { parentesco: '', telefonoContacto: '' };
    this.formVinculo = { idRepresentante: null, relacion: '', contactoPrincipal: false };
    this.errorUsuario.set(''); this.errorEstudiante.set(''); this.errorEntrenador.set(''); this.errorRepresentante.set('');
    this.errorVinculo.set('');
    this.editandoUsuario.set(false);
  }

  guardarPersona(): void {
    this.guardandoPersona.set(true);
    this.errorPersona.set('');
    const request = {
      nombre: this.formPersona.nombre, apellido: this.formPersona.apellido, cedula: this.formPersona.cedula,
      correo: this.formPersona.correo, telefono: this.formPersona.telefono || null, foto: null,
      fechaNacimiento: this.formPersona.fechaNacimiento,
    };

    if (this.esNueva()) {
      this.servicio.crearPersona(request).subscribe({
        next: (creada: PersonaResponse) => {
          this.guardandoPersona.set(false);
          this.esNueva.set(false);
          this.cargarPersonas();
          this.seleccionar({ persona: creada, usuario: null, estudiante: null, entrenador: null, representante: null });
        },
        error: (err) => this.manejarError(err, this.errorPersona, this.guardandoPersona),
      });
      return;
    }

    const idPersona = this.seleccionada()!.persona.idPersona;
    this.servicio.editarPersona(idPersona, request).subscribe({
      next: () => { this.guardandoPersona.set(false); this.cargarPersonas(true); },
      error: (err) => this.manejarError(err, this.errorPersona, this.guardandoPersona),
    });
  }

  crearUsuario(): void {
    const idPersona = this.seleccionada()!.persona.idPersona;
    this.guardandoUsuario.set(true);
    this.errorUsuario.set('');
    this.servicio.crearUsuario({
      idPersona, idEstadoGeneral: ESTADO_GENERAL_ACTIVO,
      username: this.formUsuario.username, password: this.formUsuario.password, rol: this.formUsuario.rol,
    }).subscribe({
      next: () => { this.guardandoUsuario.set(false); this.cargarPersonas(true); },
      error: (err) => this.manejarError(err, this.errorUsuario, this.guardandoUsuario),
    });
  }

  desactivarUsuario(idUsuario: number): void {
    this.servicio.desactivarUsuario(idUsuario).subscribe({ next: () => this.cargarPersonas(true) });
  }

  iniciarEdicionUsuario(u: UsuarioResponse): void {
    this.formUsuario = { username: u.username, password: '', rol: (u.roles[0] as RolUsuario) ?? 'ENTRENADOR' };
    this.errorUsuario.set('');
    this.editandoUsuario.set(true);
  }

  cancelarEdicionUsuario(): void {
    this.editandoUsuario.set(false);
    this.errorUsuario.set('');
  }

  guardarEdicionUsuario(idUsuario: number): void {
    const idPersona = this.seleccionada()!.persona.idPersona;
    const u = this.seleccionada()!.usuario!;
    this.guardandoUsuario.set(true);
    this.errorUsuario.set('');
    this.servicio.editarUsuario(idUsuario, {
      idPersona, idEstadoGeneral: u.idEstadoGeneral,
      username: this.formUsuario.username, password: this.formUsuario.password || null, rol: this.formUsuario.rol,
    }).subscribe({
      next: () => { this.guardandoUsuario.set(false); this.editandoUsuario.set(false); this.cargarPersonas(true); },
      error: (err) => this.manejarError(err, this.errorUsuario, this.guardandoUsuario),
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
        this.cargarPersonas(true);
      },
      error: (err) => this.manejarError(err, this.errorVinculo, this.guardandoVinculo),
    });
  }

  desvincularRepresentante(idRepresentante: number, idEstudiante: number): void {
    this.servicio.desvincularEstudianteDeRepresentante(idRepresentante, idEstudiante).subscribe({
      next: () => this.cargarPersonas(true),
      error: (err) => this.manejarError(err, this.errorVinculo, this.guardandoVinculo),
    });
  }

  crearEstudiante(): void {
    if (this.formEstudiante.idCategoria === null) return;
    const idPersona = this.seleccionada()!.persona.idPersona;
    this.guardandoEstudiante.set(true);
    this.errorEstudiante.set('');
    this.servicio.crearEstudiante({
      idPersona, idCategoria: this.formEstudiante.idCategoria, idEstadoGeneral: ESTADO_GENERAL_ACTIVO,
      codigoEstudiante: this.formEstudiante.codigoEstudiante, fechaIngreso: this.formEstudiante.fechaIngreso,
      peso: null, altura: null,
    }).subscribe({
      next: () => { this.guardandoEstudiante.set(false); this.cargarPersonas(true); },
      error: (err) => this.manejarError(err, this.errorEstudiante, this.guardandoEstudiante),
    });
  }

  crearEntrenador(): void {
    const actual = this.seleccionada()!;
    if (!actual.usuario) return;
    this.guardandoEntrenador.set(true);
    this.errorEntrenador.set('');
    this.servicio.crearEntrenador({
      idPersona: actual.persona.idPersona, idUsuario: actual.usuario.idUsuario,
      idEspecialidad: this.formEntrenador.idEspecialidad, experienciaAnios: this.formEntrenador.experienciaAnios,
      certificacion: null,
    }).subscribe({
      next: () => { this.guardandoEntrenador.set(false); this.cargarPersonas(true); },
      error: (err) => this.manejarError(err, this.errorEntrenador, this.guardandoEntrenador),
    });
  }

  crearRepresentante(): void {
    const actual = this.seleccionada()!;
    if (!actual.usuario) return;
    this.guardandoRepresentante.set(true);
    this.errorRepresentante.set('');
    this.servicio.crearRepresentante({
      idPersona: actual.persona.idPersona, idUsuario: actual.usuario.idUsuario,
      parentesco: this.formRepresentante.parentesco || null, telefonoContacto: this.formRepresentante.telefonoContacto || null,
      idsEstudiantesIniciales: [],
    }).subscribe({
      next: () => { this.guardandoRepresentante.set(false); this.cargarPersonas(true); },
      error: (err) => this.manejarError(err, this.errorRepresentante, this.guardandoRepresentante),
    });
  }

  private manejarError(err: any, errorSignal: ReturnType<typeof signal<string>>, guardandoSignal: ReturnType<typeof signal<boolean>>): void {
    guardandoSignal.set(false);
    errorSignal.set(err?.error?.detail ?? 'Error del servidor');
  }
}
