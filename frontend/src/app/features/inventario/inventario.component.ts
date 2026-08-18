import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../auth/auth.service';
import { InventarioService } from './inventario.service';
import {
  ArticuloResponse, AsignacionResponse, EstadoAsignacion, MovimientoResponse,
  PersonaOpcion, TipoArticulo, TipoDestinatario, TipoMovimiento,
} from './inventario.models';
import { mensajeDeError as traducirError } from '../../core/mensaje-error';

type Tab = 'articulos' | 'movimientos' | 'asignaciones';

const ETIQUETA_TIPO_ARTICULO: Record<TipoArticulo, string> = {
  UNIFORME: 'Uniforme', BALON: 'Balón', IMPLEMENTO: 'Implemento', OTRO: 'Otro',
};

/**
 * Módulo Inventario (RF-27 a RF-30): catálogo de artículos, movimientos
 * de stock y asignaciones a estudiantes/entrenadores. Un solo componente
 * con pestañas -mismo patrón de PagosComponent- porque los 3 conceptos
 * comparten el mismo catálogo de artículos y no justifican rutas propias.
 *
 * ENTRENADOR solo gestiona catálogo ni stock (ver AsignacionController):
 * la pestaña de Asignaciones es la única visible para ese rol, aunque el
 * formulario igual necesita leer el catálogo activo para elegir artículo.
 */
@Component({
  selector: 'app-inventario',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="pantalla">
      <h1 class="titulo-pantalla">Inventario</h1>

      <div class="tabs">
        @for (t of tabsVisibles(); track t) {
          <button type="button" class="tab" [class.tab--activo]="tabActiva() === t" (click)="tabActiva.set(t)">
            {{ etiquetaTab(t) }}
          </button>
        }
      </div>

      @if (tabActiva() === 'articulos') {
        <div class="card">
          @if (puedeGestionarCatalogo()) {
            <h2 class="subtitulo">{{ idArticuloEditando() === null ? 'Nuevo artículo' : 'Editar artículo' }}</h2>
            <div class="fila-2">
              <label class="field" for="af-nombre">
                <span class="field__label">Nombre</span>
                <span class="field__control"><input id="af-nombre" [(ngModel)]="formArticulo.nombre" name="af-nombre" /></span>
              </label>
              <label class="field" for="af-tipo">
                <span class="field__label">Tipo</span>
                <span class="field__control">
                  <select id="af-tipo" [(ngModel)]="formArticulo.tipo" name="af-tipo">
                    <option value="UNIFORME">Uniforme</option>
                    <option value="BALON">Balón</option>
                    <option value="IMPLEMENTO">Implemento</option>
                    <option value="OTRO">Otro</option>
                  </select>
                </span>
              </label>
            </div>
            <div class="fila-2">
              <label class="field" for="af-talla">
                <span class="field__label">Talla (opcional)</span>
                <span class="field__control"><input id="af-talla" [(ngModel)]="formArticulo.talla" name="af-talla" /></span>
              </label>
              <label class="field" for="af-stockMinimo">
                <span class="field__label">Stock mínimo (umbral de alerta, no la cantidad inicial)</span>
                <span class="field__control"><input id="af-stockMinimo" type="number" min="0" [(ngModel)]="formArticulo.stockMinimo" name="af-stockMinimo" /></span>
              </label>
            </div>
            <label class="field" for="af-descripcion">
              <span class="field__label">Descripción (opcional)</span>
              <span class="field__control"><input id="af-descripcion" [(ngModel)]="formArticulo.descripcion" name="af-descripcion" /></span>
            </label>
            <div class="acciones">
              @if (idArticuloEditando() !== null) {
                <button class="btn btn--ghost" type="button" (click)="cancelarEdicionArticulo()">Cancelar</button>
              }
              <button class="btn btn--primary" type="button" [disabled]="guardandoArticulo() || !formArticulo.nombre" (click)="guardarArticulo()">
                @if (guardandoArticulo()) { <span class="spinner"></span> Guardando… } @else { {{ idArticuloEditando() === null ? 'Registrar' : 'Guardar cambios' }} }
              </button>
            </div>
            @if (errorArticulo()) { <div class="alert alert--danger" role="alert">{{ errorArticulo() }}</div> }

            @if (stockBajoTotal() > 0) {
              <div class="alert alert--warning" role="status">{{ stockBajoTotal() }} artículo(s) con stock igual o por debajo del mínimo.</div>
            }
          }

          <h2 class="subtitulo">Catálogo</h2>
          @if (cargandoArticulos()) {
            <p class="aviso">Cargando…</p>
          } @else if (articulos().length === 0) {
            <p class="aviso">Sin artículos registrados todavía.</p>
          } @else {
            <div class="tabla">
              @for (a of articulos(); track a.idArticulo) {
                <div class="fila-articulo" [class.fila-articulo--bajo]="a.stockActual <= a.stockMinimo">
                  <span class="badge">{{ etiquetaTipo(a.tipo) }}</span>
                  <span class="nombre-articulo">{{ a.nombre }}@if (a.talla) { · {{ a.talla }} }</span>
                  <span class="stock-articulo">{{ a.stockActual }} {{ a.unidadMedida }}</span>
                  @if (puedeGestionarCatalogo()) {
                    <button class="btn btn--ghost btn--pequeno" type="button" (click)="editarArticulo(a)">Editar</button>
                    <button class="btn btn--ghost btn--pequeno" type="button" (click)="eliminarArticulo(a)">Baja</button>
                  }
                </div>
              }
            </div>
          }
        </div>
      }

      @if (tabActiva() === 'movimientos') {
        <div class="card">
          <h2 class="subtitulo">Registrar movimiento</h2>
          <div class="fila-2">
            <label class="field" for="mf-articulo">
              <span class="field__label">Artículo</span>
              <span class="field__control">
                <select id="mf-articulo" [(ngModel)]="formMovimiento.idArticulo" name="mf-articulo">
                  <option [ngValue]="null" disabled>Selecciona…</option>
                  @for (a of articulos(); track a.idArticulo) {
                    <option [ngValue]="a.idArticulo">{{ a.nombre }} (stock: {{ a.stockActual }})</option>
                  }
                </select>
              </span>
            </label>
            <label class="field" for="mf-tipo">
              <span class="field__label">Tipo</span>
              <span class="field__control">
                <select id="mf-tipo" [(ngModel)]="formMovimiento.tipoMovimiento" name="mf-tipo">
                  <option value="ENTRADA">Entrada</option>
                  <option value="SALIDA">Salida</option>
                  <option value="AJUSTE">Ajuste</option>
                </select>
              </span>
            </label>
          </div>
          <div class="fila-2">
            <label class="field" for="mf-cantidad">
              <span class="field__label">Cantidad</span>
              <span class="field__control"><input id="mf-cantidad" type="number" min="1" [(ngModel)]="formMovimiento.cantidad" name="mf-cantidad" /></span>
            </label>
            <label class="field" for="mf-motivo">
              <span class="field__label">Motivo (opcional)</span>
              <span class="field__control"><input id="mf-motivo" [(ngModel)]="formMovimiento.motivo" name="mf-motivo" /></span>
            </label>
          </div>
          <div class="acciones">
            <button class="btn btn--primary" type="button" [disabled]="guardandoMovimiento() || !formMovimiento.idArticulo || !formMovimiento.cantidad" (click)="registrarMovimiento()">
              @if (guardandoMovimiento()) { <span class="spinner"></span> Guardando… } @else { Registrar movimiento }
            </button>
          </div>
          @if (errorMovimiento()) { <div class="alert alert--danger" role="alert">{{ errorMovimiento() }}</div> }

          <h2 class="subtitulo">Historial</h2>
          @if (cargandoMovimientos()) {
            <p class="aviso">Cargando…</p>
          } @else if (movimientos().length === 0) {
            <p class="aviso">Sin movimientos registrados todavía.</p>
          } @else {
            @for (m of movimientos(); track m.idMovimiento) {
              <div class="fila-movimiento">
                <span class="badge" [class.badge--success]="m.tipoMovimiento === 'ENTRADA'" [class.badge--danger]="m.tipoMovimiento === 'SALIDA'" [class.badge--info]="m.tipoMovimiento === 'AJUSTE'">
                  {{ m.tipoMovimiento }}
                </span>
                <span class="nombre-articulo">{{ m.articulo }}</span>
                <span class="cantidad-movimiento">{{ m.cantidad }}</span>
                <span class="meta-movimiento">{{ m.registradoPor }} · {{ m.fechaMovimiento | date: 'short' }}</span>
              </div>
            }
          }
        </div>
      }

      @if (tabActiva() === 'asignaciones') {
        <div class="card">
          <h2 class="subtitulo">Nueva asignación</h2>
          <div class="fila-2">
            <label class="field" for="asf-articulo">
              <span class="field__label">Artículo</span>
              <span class="field__control">
                <select id="asf-articulo" [(ngModel)]="formAsignacion.idArticulo" name="asf-articulo">
                  <option [ngValue]="null" disabled>Selecciona…</option>
                  @for (a of articulos(); track a.idArticulo) {
                    <option [ngValue]="a.idArticulo">{{ a.nombre }} (stock: {{ a.stockActual }})</option>
                  }
                </select>
              </span>
            </label>
            <label class="field" for="asf-cantidad">
              <span class="field__label">Cantidad</span>
              <span class="field__control"><input id="asf-cantidad" type="number" min="1" [(ngModel)]="formAsignacion.cantidad" name="asf-cantidad" /></span>
            </label>
          </div>
          <div class="tabs tabs--secundario">
            <button type="button" class="tab" [class.tab--activo]="formAsignacion.tipoDestinatario === 'ESTUDIANTE'" (click)="cambiarTipoDestinatario('ESTUDIANTE')">Estudiante</button>
            <button type="button" class="tab" [class.tab--activo]="formAsignacion.tipoDestinatario === 'ENTRENADOR'" (click)="cambiarTipoDestinatario('ENTRENADOR')">Entrenador</button>
          </div>
          <label class="field" for="asf-destinatario">
            <span class="field__label">{{ formAsignacion.tipoDestinatario === 'ESTUDIANTE' ? 'Estudiante' : 'Entrenador' }}</span>
            <span class="field__control">
              <select id="asf-destinatario" [(ngModel)]="formAsignacion.idDestinatario" name="asf-destinatario">
                <option [ngValue]="null" disabled>Selecciona…</option>
                @for (p of opcionesDestinatario(); track p.id) {
                  <option [ngValue]="p.id">{{ p.nombreCompleto }}</option>
                }
              </select>
            </span>
          </label>
          <label class="field" for="asf-observaciones">
            <span class="field__label">Observaciones (opcional)</span>
            <span class="field__control"><input id="asf-observaciones" [(ngModel)]="formAsignacion.observaciones" name="asf-observaciones" /></span>
          </label>
          <div class="acciones">
            <button class="btn btn--primary" type="button"
                    [disabled]="guardandoAsignacion() || !formAsignacion.idArticulo || !formAsignacion.idDestinatario"
                    (click)="crearAsignacion()">
              @if (guardandoAsignacion()) { <span class="spinner"></span> Guardando… } @else { Asignar }
            </button>
          </div>
          @if (errorAsignacion()) { <div class="alert alert--danger" role="alert">{{ errorAsignacion() }}</div> }

          <h2 class="subtitulo">Asignaciones</h2>
          @if (cargandoAsignaciones()) {
            <p class="aviso">Cargando…</p>
          } @else if (asignaciones().length === 0) {
            <p class="aviso">Sin asignaciones registradas todavía.</p>
          } @else {
            @for (a of asignaciones(); track a.idAsignacion) {
              <div class="fila-asignacion">
                <span class="badge" [class.badge--info]="a.estado === 'ASIGNADO'" [class.badge--success]="a.estado === 'DEVUELTO'" [class.badge--danger]="a.estado === 'PERDIDO'">
                  {{ a.estado }}
                </span>
                <span class="nombre-articulo">{{ a.articulo }} × {{ a.cantidad }}</span>
                <span class="destinatario-asignacion">{{ a.estudiante ?? a.entrenador }}</span>
                <span class="meta-movimiento">{{ a.fechaAsignacion }}</span>
                @if (a.estado === 'ASIGNADO') {
                  <button class="btn btn--ghost btn--pequeno" type="button" (click)="devolver(a, 'DEVUELTO')">Devuelto</button>
                  <button class="btn btn--ghost btn--pequeno" type="button" (click)="devolver(a, 'PERDIDO')">Perdido</button>
                }
              </div>
            }
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .pantalla { max-width: 820px; margin: 0 auto; padding: 1.5rem 1.25rem 3rem; display: flex; flex-direction: column; gap: 1.25rem; }
    .titulo-pantalla { font-size: 1.2rem; }
    .subtitulo { font-size: .95rem; margin: 1rem 0 .75rem; }
    .card { padding: 1.5rem; display: flex; flex-direction: column; gap: .9rem; }

    .fila-2 { display: grid; grid-template-columns: 1fr 1fr; gap: .85rem; }
    @media (max-width: 480px) { .fila-2 { grid-template-columns: 1fr; } }

    .field__control select, .field__control input { flex: 1; border: none; outline: none; padding: .75rem 0; font-size: .95rem; background: transparent; color: var(--color-text); width: 100%; }
    .aviso { color: var(--color-text-muted); font-size: .85rem; }

    .tabs { display: flex; gap: .4rem; border-bottom: 1px solid var(--color-border-light); padding-bottom: .1rem; }
    .tabs--secundario { border-bottom: none; padding-bottom: 0; }
    .tab {
      border: none; background: none; padding: .55rem .9rem; font-size: .87rem; font-weight: 600;
      color: var(--color-text-muted); cursor: pointer; border-bottom: 2px solid transparent;
    }
    .tab--activo { color: var(--color-primary-700); border-bottom-color: var(--color-primary-500); }

    .acciones { display: flex; justify-content: flex-end; gap: .6rem; }
    .btn--pequeno { padding: .35rem .6rem; font-size: .78rem; }

    .fila-articulo, .fila-movimiento, .fila-asignacion {
      display: flex; align-items: center; gap: .75rem; padding: .55rem 0; border-bottom: 1px solid var(--color-border-light); font-size: .88rem;
    }
    .fila-articulo:last-child, .fila-movimiento:last-child, .fila-asignacion:last-child { border-bottom: none; }
    .fila-articulo--bajo .stock-articulo { color: var(--color-danger-600, #c0392b); font-weight: 700; }
    .nombre-articulo, .destinatario-asignacion { flex: 1; }
    .stock-articulo, .cantidad-movimiento { font-weight: 600; }
    .meta-movimiento { color: var(--color-text-faint); font-size: .8rem; }

    .alert--warning { background: #fff8e1; color: #8a6100; border: 1px solid #f0d98c; padding: .6rem .9rem; border-radius: var(--radius-sm); font-size: .85rem; }
    .badge--danger { background: #fdecea; color: #c0392b; }
  `],
})
export class InventarioComponent implements OnInit {

  private readonly auth = inject(AuthService);
  private readonly servicio = inject(InventarioService);

  readonly rol = computed(() => this.auth.currentUser()?.rol ?? '');
  readonly puedeGestionarCatalogo = computed(() => this.rol() === 'ADMINISTRADOR' || this.rol() === 'RECEPCIONISTA');

  readonly tabsVisibles = computed<Tab[]>(() =>
    this.rol() === 'ENTRENADOR' ? ['asignaciones'] : ['articulos', 'movimientos', 'asignaciones']);
  readonly tabActiva = signal<Tab>('articulos');

  readonly articulos = signal<ArticuloResponse[]>([]);
  readonly cargandoArticulos = signal(true);
  readonly stockBajoTotal = signal(0);

  readonly idArticuloEditando = signal<number | null>(null);
  formArticulo = { nombre: '', tipo: 'IMPLEMENTO' as TipoArticulo, talla: '', descripcion: '', stockMinimo: 0, unidadMedida: 'unidad' };
  readonly guardandoArticulo = signal(false);
  readonly errorArticulo = signal('');

  readonly movimientos = signal<MovimientoResponse[]>([]);
  readonly cargandoMovimientos = signal(true);
  formMovimiento: { idArticulo: number | null; tipoMovimiento: TipoMovimiento; cantidad: number | null; motivo: string } =
    { idArticulo: null, tipoMovimiento: 'ENTRADA', cantidad: null, motivo: '' };
  readonly guardandoMovimiento = signal(false);
  readonly errorMovimiento = signal('');

  readonly asignaciones = signal<AsignacionResponse[]>([]);
  readonly cargandoAsignaciones = signal(true);
  readonly estudiantesOpcion = signal<PersonaOpcion[]>([]);
  readonly entrenadoresOpcion = signal<PersonaOpcion[]>([]);
  readonly opcionesDestinatario = computed(() =>
    this.formAsignacion.tipoDestinatario === 'ESTUDIANTE' ? this.estudiantesOpcion() : this.entrenadoresOpcion());
  formAsignacion: { idArticulo: number | null; cantidad: number | null; tipoDestinatario: TipoDestinatario; idDestinatario: number | null; observaciones: string } =
    { idArticulo: null, cantidad: 1, tipoDestinatario: 'ESTUDIANTE', idDestinatario: null, observaciones: '' };
  readonly guardandoAsignacion = signal(false);
  readonly errorAsignacion = signal('');

  ngOnInit(): void {
    this.tabActiva.set(this.tabsVisibles()[0]);

    this.servicio.listarArticulosActivos().subscribe({
      next: (a) => { this.articulos.set(a); this.cargandoArticulos.set(false); },
      error: () => this.cargandoArticulos.set(false),
    });

    this.servicio.listarMovimientos().subscribe({
      next: (m) => { this.movimientos.set(m); this.cargandoMovimientos.set(false); },
      error: () => this.cargandoMovimientos.set(false),
    });

    this.servicio.listarAsignaciones().subscribe({
      next: (a) => { this.asignaciones.set(a); this.cargandoAsignaciones.set(false); },
      error: () => this.cargandoAsignaciones.set(false),
    });

    this.servicio.listarEstudiantesOpcion().subscribe({ next: (e) => this.estudiantesOpcion.set(e) });
    this.servicio.listarEntrenadoresOpcion().subscribe({ next: (e) => this.entrenadoresOpcion.set(e) });

    if (this.puedeGestionarCatalogo()) {
      this.servicio.stockBajo().subscribe({ next: (r) => this.stockBajoTotal.set(r.total) });
    }
  }

  etiquetaTab(t: Tab): string {
    return t === 'articulos' ? 'Artículos' : t === 'movimientos' ? 'Movimientos' : 'Asignaciones';
  }

  etiquetaTipo(t: TipoArticulo): string {
    return ETIQUETA_TIPO_ARTICULO[t];
  }

  cambiarTipoDestinatario(tipo: TipoDestinatario): void {
    this.formAsignacion.tipoDestinatario = tipo;
    this.formAsignacion.idDestinatario = null;
  }

  editarArticulo(a: ArticuloResponse): void {
    this.idArticuloEditando.set(a.idArticulo);
    this.formArticulo = { nombre: a.nombre, tipo: a.tipo, talla: a.talla ?? '', descripcion: a.descripcion ?? '', stockMinimo: a.stockMinimo, unidadMedida: a.unidadMedida };
    this.errorArticulo.set('');
  }

  cancelarEdicionArticulo(): void {
    this.idArticuloEditando.set(null);
    this.formArticulo = { nombre: '', tipo: 'IMPLEMENTO', talla: '', descripcion: '', stockMinimo: 0, unidadMedida: 'unidad' };
  }

  guardarArticulo(): void {
    if (!this.formArticulo.nombre) return;
    this.guardandoArticulo.set(true);
    this.errorArticulo.set('');

    const request = {
      nombre: this.formArticulo.nombre,
      tipo: this.formArticulo.tipo,
      talla: this.formArticulo.talla || null,
      descripcion: this.formArticulo.descripcion || null,
      stockMinimo: this.formArticulo.stockMinimo,
      unidadMedida: this.formArticulo.unidadMedida || null,
    };

    const idEditando = this.idArticuloEditando();
    const peticion = idEditando === null ? this.servicio.crearArticulo(request) : this.servicio.editarArticulo(idEditando, request);

    peticion.subscribe({
      next: (articulo) => {
        this.guardandoArticulo.set(false);
        const lista = this.articulos().filter((a) => a.idArticulo !== articulo.idArticulo);
        this.articulos.set([...lista, articulo].sort((a, b) => a.nombre.localeCompare(b.nombre)));
        this.cancelarEdicionArticulo();
      },
      error: (err) => { this.guardandoArticulo.set(false); this.errorArticulo.set(this.mensajeDeError(err)); },
    });
  }

  eliminarArticulo(a: ArticuloResponse): void {
    this.servicio.eliminarArticulo(a.idArticulo).subscribe({
      next: () => this.articulos.set(this.articulos().filter((x) => x.idArticulo !== a.idArticulo)),
    });
  }

  registrarMovimiento(): void {
    const { idArticulo, tipoMovimiento, cantidad, motivo } = this.formMovimiento;
    if (idArticulo === null || !cantidad) return;
    this.guardandoMovimiento.set(true);
    this.errorMovimiento.set('');

    this.servicio.registrarMovimiento({ idArticulo, tipoMovimiento, cantidad, motivo: motivo || null }).subscribe({
      next: (movimiento) => {
        this.guardandoMovimiento.set(false);
        this.movimientos.set([movimiento, ...this.movimientos()]);
        this.actualizarStockLocal(idArticulo, tipoMovimiento === 'SALIDA' ? -cantidad : cantidad);
        this.formMovimiento = { idArticulo: null, tipoMovimiento: 'ENTRADA', cantidad: null, motivo: '' };
      },
      error: (err) => { this.guardandoMovimiento.set(false); this.errorMovimiento.set(this.mensajeDeError(err)); },
    });
  }

  crearAsignacion(): void {
    const { idArticulo, cantidad, tipoDestinatario, idDestinatario, observaciones } = this.formAsignacion;
    if (idArticulo === null || !cantidad || idDestinatario === null) return;
    this.guardandoAsignacion.set(true);
    this.errorAsignacion.set('');

    this.servicio.crearAsignacion({
      idArticulo, cantidad, tipoDestinatario,
      idEstudiante: tipoDestinatario === 'ESTUDIANTE' ? idDestinatario : null,
      idEntrenador: tipoDestinatario === 'ENTRENADOR' ? idDestinatario : null,
      fechaDevolucionEsperada: null,
      observaciones: observaciones || null,
    }).subscribe({
      next: (asignacion) => {
        this.guardandoAsignacion.set(false);
        this.asignaciones.set([asignacion, ...this.asignaciones()]);
        this.actualizarStockLocal(idArticulo, -cantidad);
        this.formAsignacion = { idArticulo: null, cantidad: 1, tipoDestinatario, idDestinatario: null, observaciones: '' };
      },
      error: (err) => { this.guardandoAsignacion.set(false); this.errorAsignacion.set(this.mensajeDeError(err)); },
    });
  }

  devolver(a: AsignacionResponse, estado: EstadoAsignacion & ('DEVUELTO' | 'PERDIDO')): void {
    this.errorAsignacion.set('');
    this.servicio.devolverAsignacion(a.idAsignacion, { estado, observaciones: null }).subscribe({
      next: (actualizada) => {
        this.asignaciones.set(this.asignaciones().map((x) => x.idAsignacion === actualizada.idAsignacion ? actualizada : x));
        if (estado === 'DEVUELTO') this.actualizarStockLocal(a.idArticulo, a.cantidad);
      },
      error: (err) => this.errorAsignacion.set(this.mensajeDeError(err)),
    });
  }

  private actualizarStockLocal(idArticulo: number, delta: number): void {
    this.articulos.set(this.articulos().map((art) =>
      art.idArticulo === idArticulo ? { ...art, stockActual: art.stockActual + delta } : art));
  }

  private mensajeDeError(err: unknown): string {
    return traducirError(err);
  }
}
