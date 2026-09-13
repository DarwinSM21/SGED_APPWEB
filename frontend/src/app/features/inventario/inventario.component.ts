import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CargandoComponent } from '../../core/cargando.component';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../auth/auth.service';
import { InventarioService } from './inventario.service';
import {
  ArticuloResponse, AsignacionResponse, EstadoAsignacion, MovimientoResponse,
  PersonaOpcion, TipoArticulo, TipoDestinatario, TipoMovimiento,
} from './inventario.models';
import { mensajeDeError as traducirError } from '../../core/mensaje-error';
import { fechaHoraCorta } from '../../core/formato-fecha';
import { ConfirmarAccionComponent } from '../../core/confirmar-accion.component';

type Tab = 'articulos' | 'movimientos' | 'asignaciones';

const ETIQUETA_TIPO_ARTICULO: Record<TipoArticulo, string> = {
  UNIFORME: 'Uniforme', BALON: 'Balón', IMPLEMENTO: 'Implemento', OTRO: 'Otro',
};

@Component({
  selector: 'app-inventario',
  standalone: true,
  imports: [CommonModule, FormsModule, CargandoComponent, ConfirmarAccionComponent],
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
                <span class="field__control"><input id="af-nombre" [(ngModel)]="formArticulo.name" name="af-nombre" /></span>
              </label>
              <label class="field" for="af-tipo">
                <span class="field__label">Tipo</span>
                <span class="field__control">
                  <select id="af-tipo" [(ngModel)]="formArticulo.type" name="af-tipo">
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
                <span class="field__control"><input id="af-talla" [(ngModel)]="formArticulo.size" name="af-talla" /></span>
              </label>
              <label class="field" for="af-stockMinimo">
                <span class="field__label">Stock mínimo (umbral de alerta, no la cantidad inicial)</span>
                <span class="field__control"><input id="af-stockMinimo" type="number" min="0" [(ngModel)]="formArticulo.minimumStock" name="af-stockMinimo" /></span>
              </label>
            </div>
            <label class="field" for="af-descripcion">
              <span class="field__label">Descripción (opcional)</span>
              <span class="field__control"><input id="af-descripcion" [(ngModel)]="formArticulo.description" name="af-descripcion" /></span>
            </label>
            <div class="acciones">
              @if (idArticuloEditando() !== null) {
                <button class="btn btn--ghost" type="button" (click)="cancelarEdicionArticulo()">Cancelar</button>
              }
              <button class="btn btn--primary" type="button" [disabled]="guardandoArticulo() || !formArticulo.name" (click)="guardarArticulo()">
                @if (guardandoArticulo()) { <span class="spinner"></span> Guardando… } @else { {{ idArticuloEditando() === null ? 'Registrar' : 'Guardar cambios' }} }
              </button>
            </div>
            @if (errorArticulo()) { <div class="alert alert--danger" role="alert">{{ errorArticulo() }}</div> }

            @if (stockBajoTotal() > 0) {
              <div class="alert alert--warning" role="status">{{ stockBajoTotal() }} artículo(s) con stock igual o por debajo del mínimo.</div>
            }
          }

          <div class="cabecera-catalogo">
            <h2 class="subtitulo">Catálogo</h2>
            @if (puedeGestionarCatalogo()) {
              <label class="toggle-bajas">
                <input type="checkbox" [ngModel]="mostrarBajas()" (ngModelChange)="alternarBajas($event)" name="mostrarBajas" />
                Mostrar los dados de baja
              </label>
            }
          </div>
          @if (errorArticulo()) { <div class="alert alert--danger" role="alert">{{ errorArticulo() }}</div> }
          @if (cargandoArticulos()) {
            <app-cargando />
          } @else if (items().length === 0) {
            <p class="aviso">Sin artículos registrados todavía.</p>
          } @else {
            <div class="tabla">
              @for (a of items(); track a.itemId) {
                <div class="fila-articulo" [class.fila-articulo--bajo]="a.currentStock <= a.minimumStock">
                  <span class="badge">{{ etiquetaTipo(a.type) }}</span>
                  <span class="nombre-articulo">{{ a.name }}@if (a.size) { · {{ a.size }} }</span>
                  <span class="stock-articulo">{{ a.currentStock }} {{ a.unitOfMeasure }}</span>
                  @if (!a.active) { <span class="badge badge--danger">De baja</span> }
                  @if (puedeGestionarCatalogo()) {
                    @if (a.active) {
                      <button class="btn btn--ghost btn--pequeno" type="button" (click)="editarArticulo(a)">Editar</button>
                      <app-confirmar-accion etiqueta="Baja"
                                            [pregunta]="'¿Dar de baja ' + a.name + '?'"
                                            textoConfirmar="Sí, dar de baja" enCurso="Dando de baja…"
                                            [ocupado]="guardandoArticulo()" (confirmado)="eliminarArticulo(a)" />
                    } @else {
                      <app-confirmar-accion etiqueta="Reactivar" [peligrosa]="false"
                                            [pregunta]="'¿Volver a poner ' + a.name + ' en el catálogo?'"
                                            textoConfirmar="Sí, reactivar" enCurso="Reactivando…"
                                            [ocupado]="guardandoArticulo()" (confirmado)="reactivarArticulo(a)" />
                    }
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
                <select id="mf-articulo" [(ngModel)]="formMovimiento.itemId" name="mf-articulo">
                  <option [ngValue]="null" disabled>Selecciona…</option>
                  @for (a of items(); track a.itemId) {
                    <option [ngValue]="a.itemId">{{ a.name }} (stock: {{ a.currentStock }})</option>
                  }
                </select>
              </span>
            </label>
            <label class="field" for="mf-tipo">
              <span class="field__label">Tipo</span>
              <span class="field__control">
                <select id="mf-tipo" [(ngModel)]="formMovimiento.movementType" name="mf-tipo">
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
              <span class="field__control"><input id="mf-cantidad" type="number" min="1" [(ngModel)]="formMovimiento.quantity" name="mf-cantidad" /></span>
            </label>
            <label class="field" for="mf-motivo">
              <span class="field__label">Motivo (opcional)</span>
              <span class="field__control"><input id="mf-motivo" [(ngModel)]="formMovimiento.reason" name="mf-motivo" /></span>
            </label>
          </div>
          <div class="acciones">
            <button class="btn btn--primary" type="button" [disabled]="guardandoMovimiento() || !formMovimiento.itemId || !formMovimiento.quantity" (click)="registrarMovimiento()">
              @if (guardandoMovimiento()) { <span class="spinner"></span> Guardando… } @else { Registrar movimiento }
            </button>
          </div>
          @if (errorMovimiento()) { <div class="alert alert--danger" role="alert">{{ errorMovimiento() }}</div> }

          <h2 class="subtitulo">Historial</h2>
          @if (cargandoMovimientos()) {
            <app-cargando />
          } @else if (movimientos().length === 0) {
            <p class="aviso">Sin movimientos registrados todavía.</p>
          } @else {
            @for (m of movimientos(); track m.movementId) {
              <div class="fila-movimiento">
                <span class="badge" [class.badge--success]="m.movementType === 'ENTRADA'" [class.badge--danger]="m.movementType === 'SALIDA'" [class.badge--info]="m.movementType === 'AJUSTE'">
                  {{ m.movementType }}
                </span>
                <span class="nombre-articulo">{{ m.item }}</span>
                <span class="cantidad-movimiento">{{ m.quantity }}</span>
                <span class="meta-movimiento">{{ m.registeredBy }} · {{ fechaHora(m.movementDate) }}</span>
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
                <select id="asf-articulo" [(ngModel)]="formAsignacion.itemId" name="asf-articulo">
                  <option [ngValue]="null" disabled>Selecciona…</option>
                  @for (a of items(); track a.itemId) {
                    <option [ngValue]="a.itemId">{{ a.name }} (stock: {{ a.currentStock }})</option>
                  }
                </select>
              </span>
            </label>
            <label class="field" for="asf-cantidad">
              <span class="field__label">Cantidad</span>
              <span class="field__control"><input id="asf-cantidad" type="number" min="1" [(ngModel)]="formAsignacion.quantity" name="asf-cantidad" /></span>
            </label>
          </div>
          <div class="tabs tabs--secundario">
            <button type="button" class="tab" [class.tab--activo]="formAsignacion.recipientType === 'ESTUDIANTE'" (click)="cambiarTipoDestinatario('ESTUDIANTE')">Estudiante</button>
            <button type="button" class="tab" [class.tab--activo]="formAsignacion.recipientType === 'ENTRENADOR'" (click)="cambiarTipoDestinatario('ENTRENADOR')">Entrenador</button>
          </div>
          <label class="field" for="asf-destinatario">
            <span class="field__label">{{ formAsignacion.recipientType === 'ESTUDIANTE' ? 'Estudiante' : 'Entrenador' }}</span>
            <span class="field__control">
              <select id="asf-destinatario" [(ngModel)]="formAsignacion.idDestinatario" name="asf-destinatario">
                <option [ngValue]="null" disabled>Selecciona…</option>
                @for (p of opcionesDestinatario(); track p.id) {
                  <option [ngValue]="p.id">{{ p.fullName }}</option>
                }
              </select>
            </span>
          </label>
          <label class="field" for="asf-observaciones">
            <span class="field__label">Observaciones (opcional)</span>
            <span class="field__control"><input id="asf-observaciones" [(ngModel)]="formAsignacion.notes" name="asf-observaciones" /></span>
          </label>
          <div class="acciones">
            <button class="btn btn--primary" type="button"
                    [disabled]="guardandoAsignacion() || !formAsignacion.itemId || !formAsignacion.idDestinatario"
                    (click)="crearAsignacion()">
              @if (guardandoAsignacion()) { <span class="spinner"></span> Guardando… } @else { Asignar }
            </button>
          </div>
          @if (errorAsignacion()) { <div class="alert alert--danger" role="alert">{{ errorAsignacion() }}</div> }

          <h2 class="subtitulo">Asignaciones</h2>
          @if (cargandoAsignaciones()) {
            <app-cargando />
          } @else if (asignaciones().length === 0) {
            <p class="aviso">Sin asignaciones registradas todavía.</p>
          } @else {
            @for (a of asignaciones(); track a.assignmentId) {
              <div class="fila-asignacion">
                <span class="badge" [class.badge--info]="a.status === 'ASIGNADO'" [class.badge--success]="a.status === 'DEVUELTO'" [class.badge--danger]="a.status === 'PERDIDO'">
                  {{ a.status }}
                </span>
                <span class="nombre-articulo">{{ a.item }} × {{ a.quantity }}</span>
                <span class="destinatario-asignacion">{{ a.student ?? a.coach }}</span>
                <span class="meta-movimiento">{{ a.assignmentDate }}</span>
                @if (a.status === 'ASIGNADO') {
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
    .cabecera-catalogo { display: flex; align-items: center; justify-content: space-between; gap: 1rem; flex-wrap: wrap; }
    .toggle-bajas { display: flex; align-items: center; gap: .4rem; font-size: .82rem; color: var(--color-text-muted); }
    .fila-articulo--bajo .stock-articulo { color: var(--color-danger-600, #c0392b); font-weight: 700; }
    .name-articulo, .destinatario-asignacion { flex: 1; }
    .stock-articulo, .quantity-movimiento { font-weight: 600; }
    .meta-movimiento { color: var(--color-text-faint); font-size: .8rem; }
    .alert--warning { background: #fff8e1; color: #8a6100; border: 1px solid #f0d98c; padding: .6rem .9rem; border-radius: var(--radius-sm); font-size: .85rem; }
    .badge--danger { background: #fdecea; color: #c0392b; }
  `],
})
export class InventarioComponent implements OnInit {
  readonly fechaHora = fechaHoraCorta;

  private readonly auth = inject(AuthService);
  private readonly servicio = inject(InventarioService);

  readonly rol = computed(() => this.auth.currentUser()?.role ?? '');
  readonly puedeGestionarCatalogo = computed(() => this.rol() === 'ADMINISTRADOR' || this.rol() === 'RECEPCIONISTA');

  readonly tabsVisibles = computed<Tab[]>(() =>
    this.rol() === 'ENTRENADOR' ? ['asignaciones'] : ['articulos', 'movimientos', 'asignaciones']);
  readonly tabActiva = signal<Tab>('articulos');

  readonly items = signal<ArticuloResponse[]>([]);
  readonly mostrarBajas = signal(false);
  readonly cargandoArticulos = signal(true);
  readonly stockBajoTotal = signal(0);

  readonly idArticuloEditando = signal<number | null>(null);
  formArticulo = { name: '', type: 'IMPLEMENTO' as TipoArticulo, size: '', description: '', minimumStock: 0, unitOfMeasure: 'unidad' };
  readonly guardandoArticulo = signal(false);
  readonly errorArticulo = signal('');

  readonly movimientos = signal<MovimientoResponse[]>([]);
  readonly cargandoMovimientos = signal(true);
  formMovimiento: { itemId: number | null; movementType: TipoMovimiento; quantity: number | null; reason: string } =
    { itemId: null, movementType: 'ENTRADA', quantity: null, reason: '' };
  readonly guardandoMovimiento = signal(false);
  readonly errorMovimiento = signal('');

  readonly asignaciones = signal<AsignacionResponse[]>([]);
  readonly cargandoAsignaciones = signal(true);
  readonly estudiantesOpcion = signal<PersonaOpcion[]>([]);
  readonly entrenadoresOpcion = signal<PersonaOpcion[]>([]);
  readonly opcionesDestinatario = computed(() =>
    this.formAsignacion.recipientType === 'ESTUDIANTE' ? this.estudiantesOpcion() : this.entrenadoresOpcion());
  formAsignacion: { itemId: number | null; quantity: number | null; recipientType: TipoDestinatario; idDestinatario: number | null; notes: string } =
    { itemId: null, quantity: 1, recipientType: 'ESTUDIANTE', idDestinatario: null, notes: '' };
  readonly guardandoAsignacion = signal(false);
  readonly errorAsignacion = signal('');

  ngOnInit(): void {
    this.tabActiva.set(this.tabsVisibles()[0]);

    this.cargarArticulos();

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
      this.servicio.stockBajo().subscribe({ next: (r) => this.stockBajoTotal.set(r.totalElements) });
    }
  }

  cargarArticulos(): void {
    this.cargandoArticulos.set(true);
    this.errorArticulo.set('');

    if (this.mostrarBajas()) {
      this.servicio.listarArticulosConBajas().subscribe({
        next: (pagina) => { this.items.set(pagina.content); this.cargandoArticulos.set(false); },
        error: (e) => { this.errorArticulo.set(this.mensajeDeError(e)); this.cargandoArticulos.set(false); },
      });
      return;
    }

    this.servicio.listarArticulosActivos().subscribe({
      next: (a) => { this.items.set(a); this.cargandoArticulos.set(false); },
      error: (e) => { this.errorArticulo.set(this.mensajeDeError(e)); this.cargandoArticulos.set(false); },
    });
  }

  alternarBajas(mostrar: boolean): void {
    this.mostrarBajas.set(mostrar);
    this.cargarArticulos();
  }

  reactivarArticulo(a: ArticuloResponse): void {
    this.guardandoArticulo.set(true);
    this.errorArticulo.set('');
    this.servicio.reactivarArticulo(a.itemId).subscribe({
      next: () => { this.guardandoArticulo.set(false); this.cargarArticulos(); },
      error: (e) => { this.guardandoArticulo.set(false); this.errorArticulo.set(this.mensajeDeError(e)); },
    });
  }

  etiquetaTab(t: Tab): string {
    return t === 'articulos' ? 'Artículos' : t === 'movimientos' ? 'Movimientos' : 'Asignaciones';
  }

  etiquetaTipo(t: TipoArticulo): string {
    return ETIQUETA_TIPO_ARTICULO[t];
  }

  cambiarTipoDestinatario(tipo: TipoDestinatario): void {
    this.formAsignacion.recipientType = tipo;
    this.formAsignacion.idDestinatario = null;
  }

  editarArticulo(a: ArticuloResponse): void {
    this.idArticuloEditando.set(a.itemId);
    this.formArticulo = { name: a.name, type: a.type, size: a.size ?? '', description: a.description ?? '', minimumStock: a.minimumStock, unitOfMeasure: a.unitOfMeasure };
    this.errorArticulo.set('');
  }

  cancelarEdicionArticulo(): void {
    this.idArticuloEditando.set(null);
    this.formArticulo = { name: '', type: 'IMPLEMENTO', size: '', description: '', minimumStock: 0, unitOfMeasure: 'unidad' };
  }

  guardarArticulo(): void {
    if (!this.formArticulo.name) return;
    this.guardandoArticulo.set(true);
    this.errorArticulo.set('');

    const request = {
      name: this.formArticulo.name,
      type: this.formArticulo.type,
      size: this.formArticulo.size || null,
      description: this.formArticulo.description || null,
      minimumStock: this.formArticulo.minimumStock,
      unitOfMeasure: this.formArticulo.unitOfMeasure || null,
    };

    const idEditando = this.idArticuloEditando();
    const peticion = idEditando === null ? this.servicio.crearArticulo(request) : this.servicio.editarArticulo(idEditando, request);

    peticion.subscribe({
      next: (articulo) => {
        this.guardandoArticulo.set(false);
        const lista = this.items().filter((a) => a.itemId !== articulo.itemId);
        this.items.set([...lista, articulo].sort((a, b) => a.name.localeCompare(b.name)));
        this.cancelarEdicionArticulo();
      },
      error: (err) => { this.guardandoArticulo.set(false); this.errorArticulo.set(this.mensajeDeError(err)); },
    });
  }

  eliminarArticulo(a: ArticuloResponse): void {
    this.guardandoArticulo.set(true);
    this.errorArticulo.set('');
    this.servicio.eliminarArticulo(a.itemId).subscribe({
      next: () => { this.guardandoArticulo.set(false); this.cargarArticulos(); },
      error: (e) => { this.guardandoArticulo.set(false); this.errorArticulo.set(this.mensajeDeError(e)); },
    });
  }

  registrarMovimiento(): void {
    const { itemId, movementType, quantity, reason } = this.formMovimiento;
    if (itemId === null || !quantity) return;
    this.guardandoMovimiento.set(true);
    this.errorMovimiento.set('');

    this.servicio.registrarMovimiento({ itemId, movementType, quantity, reason: reason || null }).subscribe({
      next: (movimiento) => {
        this.guardandoMovimiento.set(false);
        this.movimientos.set([movimiento, ...this.movimientos()]);
        this.actualizarStockLocal(itemId, movementType === 'SALIDA' ? -quantity : quantity);
        this.formMovimiento = { itemId: null, movementType: 'ENTRADA', quantity: null, reason: '' };
      },
      error: (err) => { this.guardandoMovimiento.set(false); this.errorMovimiento.set(this.mensajeDeError(err)); },
    });
  }

  crearAsignacion(): void {
    const { itemId, quantity, recipientType, idDestinatario, notes } = this.formAsignacion;
    if (itemId === null || !quantity || idDestinatario === null) return;
    this.guardandoAsignacion.set(true);
    this.errorAsignacion.set('');

    this.servicio.crearAsignacion({
      itemId, quantity, recipientType,
      studentId: recipientType === 'ESTUDIANTE' ? idDestinatario : null,
      coachId: recipientType === 'ENTRENADOR' ? idDestinatario : null,
      expectedReturnDate: null,
      notes: notes || null,
    }).subscribe({
      next: (asignacion) => {
        this.guardandoAsignacion.set(false);
        this.asignaciones.set([asignacion, ...this.asignaciones()]);
        this.actualizarStockLocal(itemId, -quantity);
        this.formAsignacion = { itemId: null, quantity: 1, recipientType, idDestinatario: null, notes: '' };
      },
      error: (err) => { this.guardandoAsignacion.set(false); this.errorAsignacion.set(this.mensajeDeError(err)); },
    });
  }

  devolver(a: AsignacionResponse, status: EstadoAsignacion & ('DEVUELTO' | 'PERDIDO')): void {
    this.errorAsignacion.set('');
    this.servicio.devolverAsignacion(a.assignmentId, { status, notes: null }).subscribe({
      next: (actualizada) => {
        this.asignaciones.set(this.asignaciones().map((x) => x.assignmentId === actualizada.assignmentId ? actualizada : x));
        if (status === 'DEVUELTO') this.actualizarStockLocal(a.itemId, a.quantity);
      },
      error: (err) => this.errorAsignacion.set(this.mensajeDeError(err)),
    });
  }

  private actualizarStockLocal(idArticulo: number, delta: number): void {
    this.items.set(this.items().map((art) =>
      art.itemId === idArticulo ? { ...art, currentStock: art.currentStock + delta } : art));
  }

  private mensajeDeError(err: unknown): string {
    return traducirError(err);
  }
}
