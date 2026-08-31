import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CargandoComponent } from '../../core/cargando.component';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CategoriasService } from './categorias.service';
import { AuthService } from '../../auth/auth.service';
import { Categoria, CategoriaRequest } from './categorias.models';
import { mensajeDeError } from '../../core/mensaje-error';
import { ConfirmarAccionComponent } from '../../core/confirmar-accion.component';

const FORMATO_NOMBRE = /^\s*sub[\s-]?\d{1,2}\s*$/i;

const FORMULARIO_VACIO: CategoriaRequest = {
  nombre: '', edadMin: null, edadMax: null, descripcion: null,
};

@Component({
  selector: 'app-categorias',
  standalone: true,
  imports: [CommonModule, FormsModule, CargandoComponent, ConfirmarAccionComponent],
  template: `
    <div class="contenido">
      <h1 class="titulo-panel">Categorías</h1>
      <p class="subtitulo-pantalla">
        Los grupos por edad a los que se asignan los estudiantes, los horarios y las sesiones.
        @if (!puedeGestionar()) {
          Crearlas y modificarlas corresponde a un administrador.
        }
      </p>

      @if (error()) { <div class="alert alert--danger" role="alert">{{ error() }}</div> }
      @if (exito()) { <div class="alert alert--success" role="status">{{ exito() }}</div> }

      @if (puedeGestionar()) {
      <section class="card formulario">
        <h2 class="titulo-card">{{ editando() ? 'Editar categoría' : 'Nueva categoría' }}</h2>

        <div class="fila">
          <label class="field" for="cat-nombre">
            <span class="field__label">Nombre</span>
            <span class="field__control">
              <input id="cat-nombre" [(ngModel)]="formulario.nombre" name="cat-nombre"
                     placeholder="SUB-14" maxlength="10" inputmode="text"
                     (blur)="normalizarNombre()" />
            </span>
            <span class="field__hint">Formato SUB-14. Se corrige solo al salir del campo.</span>
          </label>

          <label class="field" for="cat-min">
            <span class="field__label">Edad mínima</span>
            <span class="field__control">
              <input id="cat-min" type="number" min="4" max="99"
                     [(ngModel)]="formulario.edadMin" name="cat-min" />
            </span>
          </label>

          <label class="field" for="cat-max">
            <span class="field__label">Edad máxima</span>
            <span class="field__control">
              <input id="cat-max" type="number" min="4" max="99"
                     [(ngModel)]="formulario.edadMax" name="cat-max" />
            </span>
          </label>
        </div>

        <label class="field" for="cat-desc">
          <span class="field__label">Descripción (opcional)</span>
          <span class="field__control">
            <input id="cat-desc" [(ngModel)]="formulario.descripcion" name="cat-desc"
                   placeholder="Iniciación, formativa…" maxlength="255" />
          </span>
        </label>

        <div class="acciones">
          <button class="btn btn--primary" type="button" [disabled]="guardando() || !formularioValido()"
                  (click)="guardar()">
            {{ guardando() ? 'Guardando…' : (editando() ? 'Guardar cambios' : 'Crear categoría') }}
          </button>
          @if (editando()) {
            <button class="btn btn--ghost" type="button" (click)="cancelar()">Cancelar</button>
          }
        </div>
      </section>
      }

      <section class="card lista">
        <div class="lista__cabecera">
          <h2 class="titulo-card">Catálogo</h2>
          <span class="conteo">{{ activas() }} activas · {{ inactivas() }} inactivas</span>
        </div>

        @if (cargando()) {
          <app-cargando />
        } @else if (categorias().length === 0) {
          <p class="aviso">
            {{ puedeGestionar() ? 'Todavía no hay categorías. Crea la primera arriba.'
                                : 'Todavía no hay categorías registradas.' }}
          </p>
        } @else {
          @for (c of categorias(); track c.idCategoria) {
            <div class="fila-categoria" [class.fila-categoria--inactiva]="!c.activo">
              <div class="info">
                <span class="nombre">
                  {{ c.nombre }}
                  @if (!c.activo) { <span class="badge badge--neutral">inactiva</span> }
                </span>
                <span class="detalle">
                  {{ c.edadMin }} a {{ c.edadMax }} años
                  @if (c.descripcion) { · {{ c.descripcion }} }
                </span>
              </div>
              @if (puedeGestionar()) {
              <div class="botones">
                <button class="btn btn--ghost btn--sm" type="button" (click)="editar(c)">Editar</button>
                @if (c.activo) {
                  <app-confirmar-accion etiqueta="Desactivar"
                                        [pregunta]="'¿Desactivar ' + c.nombre + '? Deja de aparecer para asignar estudiantes.'"
                                        textoConfirmar="Sí, desactivar" enCurso="Desactivando…"
                                        [ocupado]="guardando()" (confirmado)="desactivar(c)" />
                } @else {
                  <button class="btn btn--ghost btn--sm" type="button"
                          [disabled]="guardando()" (click)="reactivar(c)">Reactivar</button>
                }
              </div>
              }
            </div>
          }
        }
      </section>
    </div>
  `,
  styles: [`
    .contenido { max-width: 900px; margin: 0 auto; padding: 1.5rem 1.25rem; }
    .subtitulo-pantalla { margin: .2rem 0 1.2rem; font-size: .86rem; color: var(--color-text-muted); }
    .formulario { padding: 1.2rem 1.4rem; margin-bottom: 1.25rem; }
    .titulo-card { font-size: 1rem; margin-bottom: .9rem; }
    .fila { display: grid; grid-template-columns: 2fr 1fr 1fr; gap: .8rem; }
    @media (max-width: 640px) { .fila { grid-template-columns: 1fr; } }
    .acciones { display: flex; gap: .6rem; margin-top: .3rem; }
    .lista { padding: 1.2rem 1.4rem; }
    .lista__cabecera { display: flex; align-items: baseline; justify-content: space-between; gap: 1rem; margin-bottom: .6rem; }
    .conteo { font-size: .78rem; color: var(--color-text-faint); font-variant-numeric: tabular-nums; }
    .aviso { font-size: .86rem; color: var(--color-text-muted); }
    .fila-categoria {
      display: flex; align-items: center; gap: 1rem;
      padding: .7rem 0; border-bottom: 1px solid var(--color-border-light);
    }
    .fila-categoria:last-child { border-bottom: none; }
    .fila-categoria--inactiva .nombre, .fila-categoria--inactiva .detalle { color: var(--color-text-faint); }
    .info { display: flex; flex-direction: column; gap: .15rem; flex: 1; min-width: 0; }
    .nombre { font-weight: 600; display: flex; align-items: center; gap: .5rem; }
    .detalle { font-size: .8rem; color: var(--color-text-muted); }
    .botones { display: flex; gap: .4rem; flex-shrink: 0; }
  `],
})
export class CategoriasComponent implements OnInit {
  private readonly servicio = inject(CategoriasService);
  private readonly authService = inject(AuthService);

  readonly puedeGestionar = computed(() =>
    this.authService.currentUser()?.rol === 'ADMINISTRADOR');

  readonly categorias = signal<Categoria[]>([]);
  readonly cargando = signal(true);
  readonly guardando = signal(false);
  readonly error = signal('');
  readonly exito = signal('');
  readonly editando = signal<Categoria | null>(null);

  formulario: CategoriaRequest = { ...FORMULARIO_VACIO };

  readonly activas = computed(() => this.categorias().filter((c) => c.activo).length);
  readonly inactivas = computed(() => this.categorias().length - this.activas());

  normalizarNombre(): void {
    const digitos = this.formulario.nombre.replace(/\D+/g, '');
    if (digitos) this.formulario.nombre = 'SUB-' + digitos;
  }

  formularioValido(): boolean {
    const f = this.formulario;
    return FORMATO_NOMBRE.test(f.nombre.trim())
      && f.edadMin !== null && f.edadMax !== null
      && f.edadMin >= 4 && f.edadMax > f.edadMin;
  }

  ngOnInit(): void {
    this.cargar();
  }

  private cargar(): void {
    this.cargando.set(true);
    this.servicio.listar().subscribe({
      next: (pagina) => { this.categorias.set(pagina.content); this.cargando.set(false); },
      error: (e) => { this.error.set(mensajeDeError(e, 'No se pudieron cargar las categorías')); this.cargando.set(false); },
    });
  }

  guardar(): void {
    this.guardando.set(true);
    this.error.set('');
    this.exito.set('');

    const enEdicion = this.editando();
    const peticion = enEdicion
      ? this.servicio.editar(enEdicion.idCategoria, this.formulario)
      : this.servicio.crear(this.formulario);

    peticion.subscribe({
      next: (c) => {
        this.guardando.set(false);
        this.exito.set(enEdicion ? `Categoría "${c.nombre}" actualizada` : `Categoría "${c.nombre}" creada`);
        this.cancelar();
        this.cargar();
      },
      error: (e) => {
        this.guardando.set(false);
        this.error.set(mensajeDeError(e, 'No se pudo guardar la categoría'));
      },
    });
  }

  editar(c: Categoria): void {
    this.editando.set(c);
    this.formulario = {
      nombre: c.nombre, edadMin: c.edadMin, edadMax: c.edadMax, descripcion: c.descripcion,
    };
    this.error.set('');
    this.exito.set('');
  }

  cancelar(): void {
    this.editando.set(null);
    this.formulario = { ...FORMULARIO_VACIO };
  }

  desactivar(c: Categoria): void {
    this.guardando.set(true);
    this.error.set('');
    this.servicio.desactivar(c.idCategoria).subscribe({
      next: () => {
        this.guardando.set(false);
        this.exito.set(`"${c.nombre}" quedó inactiva; los estudiantes que ya la tenían no se tocan`);
        this.cargar();
      },
      error: (e) => {
        this.guardando.set(false);
        this.error.set(mensajeDeError(e, 'No se pudo desactivar la categoría'));
      },
    });
  }

  reactivar(c: Categoria): void {
    this.guardando.set(true);
    this.error.set('');
    this.servicio.reactivar(c.idCategoria).subscribe({
      next: () => {
        this.guardando.set(false);
        this.exito.set(`"${c.nombre}" volvió a estar activa`);
        this.cargar();
      },
      error: (e) => {
        this.guardando.set(false);
        this.error.set(mensajeDeError(e, 'No se pudo reactivar la categoría'));
      },
    });
  }
}
