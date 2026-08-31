import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CargandoComponent } from '../../core/cargando.component';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PagosService } from './pagos.service';
import { EstudianteOpcionPago, IngresosMes, PagoResponse } from './pagos.models';
import { mensajeDeError as traducirError } from '../../core/mensaje-error';
import { BuscadorOpcionesComponent, OpcionBuscable } from '../../core/buscador-opciones.component';

const NOMBRES_MES = [
  'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
  'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre',
];

@Component({
  selector: 'app-pagos',
  standalone: true,
  imports: [CommonModule, FormsModule, BuscadorOpcionesComponent, CargandoComponent],
  template: `
    <div class="pantalla">
      <div class="encabezado">
        <h1 class="titulo-pantalla">Gestión de Pagos</h1>
        <p class="subtitulo-pantalla">Administra las membresías y pagos mensuales de los estudiantes de forma rápida y segura.</p>
      </div>

      <div class="kpi-ingresos">
        <span class="kpi-ingresos__icono">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="9"/>
            <path d="M12 7v10M9.5 9.5c0-1.1 1.12-2 2.5-2s2.5.9 2.5 2-1.12 2-2.5 2-2.5.9-2.5 2 1.12 2 2.5 2 2.5-.9 2.5-2"/>
          </svg>
        </span>
        <div class="kpi-ingresos__info">
          <span class="kpi-ingresos__etiqueta">
            @if (ingresosMes(); as ing) { Ingresos de {{ nombreMes(ing.mes) }} } @else { Ingresos del mes }
          </span>
          <strong class="kpi-ingresos__monto">{{ (ingresosMes()?.total ?? 0) | number: '1.2-2' }}</strong>
          <span class="kpi-ingresos__caption">
            @if ((ingresosMes()?.cantidadPagos ?? 0) === 1) { 1 pago registrado } @else { {{ ingresosMes()?.cantidadPagos ?? 0 }} pagos registrados }
          </span>
        </div>
      </div>

      <div class="layout">
        <div class="card formulario">
          <h2 class="titulo-card">Registro de Membresía</h2>

          @if (idEstudiante() === null) {
            <app-buscador-opciones
              etiqueta="Estudiante"
              marcador="Escribe el nombre o la categoría…"
              [opciones]="opcionesEstudiantes()"
              [cargando]="cargandoEstudiantes()"
              (seleccionada)="seleccionarEstudiante($event.id)" />
          } @else if (estudianteSeleccionado(); as est) {
            <div class="chip-estudiante">
              <span class="avatar">{{ iniciales(est.nombreCompleto) }}</span>
              <span class="chip-estudiante__info">
                <span class="chip-estudiante__etiqueta">Estudiante seleccionado</span>
                <span class="chip-estudiante__nombre">{{ est.nombreCompleto }}</span>
              </span>
              <span class="badge badge--info">Categoría: {{ est.categoria }}</span>
              <button type="button" class="btn btn--ghost btn--cambiar" (click)="cambiarEstudiante()">Cambiar</button>
            </div>

            <span class="field__label">Tipo de cobro</span>
            <div class="segmentado">
              <button type="button" class="segmento" [class.segmento--activo]="tipo() === 'MEMBRESIA'" (click)="tipo.set('MEMBRESIA')">Membresía mensual</button>
              <button type="button" class="segmento" [class.segmento--activo]="tipo() === 'DIARIO'" (click)="tipo.set('DIARIO')">Diario / eventual</button>
            </div>

            @if (tipo() === 'MEMBRESIA') {
              <div class="fila-2">
                <label class="field" for="anio">
                  <span class="field__label">Año</span>
                  <span class="field__control">
                    <select id="anio" [(ngModel)]="anio" name="anio">
                      @for (a of aniosDisponibles; track a) {
                        <option [ngValue]="a">{{ a }}</option>
                      }
                    </select>
                  </span>
                </label>
                <label class="field" for="montoMembresia">
                  <span class="field__label">Monto por mes</span>
                  <span class="field__control">
                    <span class="field__prefijo">$</span>
                    <input id="montoMembresia" type="number" step="0.01" min="0.01" [(ngModel)]="montoMembresia" name="montoMembresia" />
                  </span>
                </label>
              </div>

              <div class="meses-encabezado">
                <span class="field__label">Meses a cubrir</span>
                <button type="button" class="enlace" (click)="alternarTodosLosMeses()">
                  {{ todosLosMesesSeleccionados() ? 'Quitar todos' : 'Seleccionar todos' }}
                </button>
              </div>
              <div class="meses">
                @for (m of meses; track m) {
                  <button type="button" class="pill-mes"
                          [class.pill-mes--activo]="mesesSeleccionados().has(m)"
                          [class.pill-mes--bloqueado]="!mesDisponible(m)"
                          [disabled]="!mesDisponible(m)"
                          [attr.title]="motivoMesNoDisponible(m)"
                          (click)="alternarMes(m)">
                    @if (mesesSeleccionados().has(m)) {
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12.5l4.5 4.5L19 7"/></svg>
                    } @else if (mesesPagados().has(m)) {
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12.5l4.5 4.5L19 7"/></svg>
                    }
                    {{ nombreMes(m) }}
                  </button>
                }
              </div>
              @if (mesesPagados().size > 0) {
                <p class="leyenda-meses">
                  Los meses con visto ya están pagados y no se pueden volver a cobrar.
                </p>
              }

              <div class="resumen-total">
                <div class="resumen-total__info">
                  <span class="resumen-total__etiqueta">Total estimado a registrar</span>
                  <strong class="resumen-total__monto">{{ totalEstimado() | number: '1.2-2' }}</strong>
                  <span class="resumen-total__caption">
                    {{ mesesSeleccionados().size }} {{ mesesSeleccionados().size === 1 ? 'mes seleccionado' : 'meses seleccionados' }}
                  </span>
                </div>
                <button class="btn btn--primary" type="button" [disabled]="guardando() || mesesSeleccionados().size === 0" (click)="registrarMembresia()">
                  @if (guardando()) {
                    <span class="spinner"></span> Guardando…
                  } @else {
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M8.5 12.5l2.5 2.5 5-5"/></svg>
                    Registrar membresía
                  }
                </button>
              </div>
            } @else {
              <label class="field" for="montoDiario">
                <span class="field__label">Monto</span>
                <span class="field__control">
                  <span class="field__prefijo">$</span>
                  <input id="montoDiario" type="number" step="0.01" min="0.01" [(ngModel)]="montoDiario" name="montoDiario" />
                </span>
              </label>
              @if (tieneMembresiaVigente()) {
                <div class="alert alert--warning" role="status">
                  Este estudiante ya tiene la membresía de {{ nombreMes(mesActual) }} pagada.
                  Registra un pago diario solo si es un extra (torneo, clase suelta);
                  de lo contrario estarías cobrando dos veces el mismo mes.
                </div>
              }
              <button class="btn btn--primary btn--block" type="button" [disabled]="guardando()" (click)="registrarDiario()">
                @if (guardando()) { <span class="spinner"></span> Guardando… } @else { Registrar pago diario }
              </button>
            }

            @if (error()) { <div class="alert alert--danger" role="alert">{{ error() }}</div> }
            @if (exito()) { <div class="alert alert--success" role="status">{{ exito() }}</div> }
          }
        </div>

        @if (idEstudiante() !== null) {
          <div class="columna-historial">
            <div class="card historial">
              <h2 class="titulo-card">Historial de pagos</h2>
              @if (cargandoHistorial()) {
                <app-cargando />
              } @else if (historial().length === 0) {
                <div class="vacio">
                  <span class="vacio__icono">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M6 3h12v18l-3-2-3 2-3-2-3 2V3z"/>
                      <line x1="9" y1="8" x2="15" y2="8"/>
                      <line x1="9" y1="12" x2="15" y2="12"/>
                    </svg>
                  </span>
                  <p class="vacio__titulo">Sin pagos registrados todavía</p>
                  <p class="vacio__texto">Los registros de pagos recientes para este estudiante aparecerán aquí.</p>
                </div>
              } @else {
                @for (p of historial(); track p.idPago) {
                  <div class="fila-pago" [class.fila-pago--anulada]="p.anuladoEn">
                    <span class="badge"
                          [class.badge--info]="p.tipo === 'MEMBRESIA' && !p.anuladoEn"
                          [class.badge--success]="p.tipo === 'DIARIO' && !p.anuladoEn"
                          [class.badge--neutral]="!!p.anuladoEn">
                      {{ p.tipo === 'MEMBRESIA' ? (nombreMes(p.mes!) + ' ' + p.anio) : 'Diario' }}
                    </span>
                    <span class="monto-pago">{{ p.monto | number: '1.2-2' }}</span>
                    <span class="fecha-pago">{{ p.fechaPago }}</span>
                    @if (p.anuladoEn) {
                      <span class="badge badge--danger">Anulado</span>
                    } @else {
                      <button type="button" class="btn btn--ghost btn--sm"
                              [disabled]="anulando() === p.idPago" (click)="pedirAnulacion(p)">
                        {{ anulando() === p.idPago ? 'Anulando…' : 'Anular' }}
                      </button>
                    }
                  </div>
                  @if (p.anuladoEn) {
                    <p class="motivo-anulacion">
                      {{ p.motivoAnulacion }} — {{ p.anuladoPor }}
                    </p>
                  }
                }
              }
            </div>

            <div class="alert alert--info consejo">
              Asegúrese de verificar los montos antes de registrar una membresía para evitar discrepancias contables.
            </div>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .pantalla { max-width: 1100px; margin: 0 auto; padding: 1.5rem 1.25rem 3rem; display: flex; flex-direction: column; gap: 1.5rem; }
    .encabezado { display: flex; flex-direction: column; gap: .3rem; }
    .titulo-pantalla { font-size: 1.5rem; }
    .subtitulo-pantalla { color: var(--color-text-muted); font-size: .92rem; max-width: 640px; }
    .titulo-card { font-size: 1.05rem; margin-bottom: 1.1rem; }
    .kpi-ingresos {
      display: flex; align-items: center; gap: 1rem;
      background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-md);
      box-shadow: var(--shadow-sm); padding: 1.1rem 1.3rem; max-width: 320px;
    }
    .kpi-ingresos__icono {
      width: 44px; height: 44px; border-radius: 50%; background: var(--color-success-bg); color: var(--color-success-text);
      display: flex; align-items: center; justify-content: center; flex-shrink: 0;
    }
    .kpi-ingresos__icono svg { width: 22px; height: 22px; }
    .kpi-ingresos__info { display: flex; flex-direction: column; gap: .1rem; min-width: 0; }
    .kpi-ingresos__etiqueta { font-size: .78rem; color: var(--color-text-muted); }
    .kpi-ingresos__monto { font-size: 1.4rem; color: var(--color-text); }
    .kpi-ingresos__caption { font-size: .75rem; color: var(--color-text-faint); }
    .layout { display: grid; grid-template-columns: 1.6fr 1fr; gap: 1.25rem; align-items: start; }
    @media (max-width: 960px) { .layout { grid-template-columns: 1fr; } }
    .formulario { padding: 1.5rem; display: flex; flex-direction: column; gap: .9rem; }
    .fila-2 { display: grid; grid-template-columns: 1fr 1fr; gap: .85rem; }
    @media (max-width: 480px) { .fila-2 { grid-template-columns: 1fr; } }
    .field__control select { flex: 1; border: none; outline: none; padding: .75rem 0; font-size: .95rem; background: transparent; color: var(--color-text); width: 100%; }
    .field__prefijo { color: var(--color-text-faint); font-weight: 600; }
    .aviso { color: var(--color-text-muted); font-size: .85rem; }
    .chip-estudiante {
      display: flex; align-items: center; gap: .75rem;
      background: var(--color-primary-50); border: 1px solid var(--color-primary-100);
      border-radius: var(--radius-sm); padding: .75rem 1rem;
    }
    .chip-estudiante__info { display: flex; flex-direction: column; gap: .1rem; flex: 1; min-width: 0; }
    .chip-estudiante__etiqueta { font-size: .68rem; font-weight: 700; letter-spacing: .04em; text-transform: uppercase; color: var(--color-primary-600); }
    .chip-estudiante__nombre { font-weight: 700; color: var(--color-text); }
    .btn--cambiar { padding: .4rem .75rem; font-size: .8rem; flex-shrink: 0; }
    .segmentado { display: flex; gap: .3rem; background: var(--color-bg); border-radius: var(--radius-sm); padding: .3rem; }
    .segmento {
      flex: 1; border: none; background: transparent; border-radius: calc(var(--radius-sm) - 4px);
      padding: .6rem .9rem; font-size: .87rem; font-weight: 600; color: var(--color-text-muted); cursor: pointer;
      transition: background var(--transition), color var(--transition), box-shadow var(--transition);
    }
    .segmento--activo { background: var(--gradient-primary); color: #fff; box-shadow: var(--shadow-sm); }
    .meses-encabezado { display: flex; align-items: baseline; justify-content: space-between; }
    .enlace { border: none; background: none; color: var(--color-primary-600); font-size: .8rem; font-weight: 700; cursor: pointer; padding: 0; }
    .enlace:hover { text-decoration: underline; }
    .meses { display: flex; flex-wrap: wrap; gap: .5rem; }
    .pill-mes--bloqueado {
      opacity: .45; cursor: not-allowed; text-decoration: line-through;
    }
    .pill-mes--bloqueado:hover { background: inherit; }
    .leyenda-meses { margin: .5rem 0 0; font-size: .76rem; color: var(--color-text-faint); }
    .pill-mes {
      display: inline-flex; align-items: center; gap: .35rem;
      border: 1.5px solid var(--color-border); background: var(--color-surface); color: var(--color-text-muted);
      border-radius: var(--radius-full); padding: .45rem .9rem; font-size: .83rem; font-weight: 600; cursor: pointer;
      transition: background var(--transition), border-color var(--transition), color var(--transition);
    }
    .pill-mes svg { width: 13px; height: 13px; flex-shrink: 0; }
    .pill-mes--activo { border-color: var(--color-primary-500); background: var(--color-primary-50); color: var(--color-primary-700); }
    .resumen-total {
      display: flex; align-items: center; justify-content: space-between; gap: 1rem; flex-wrap: wrap;
      background: var(--color-bg); border-radius: var(--radius-sm); padding: .9rem 1.1rem;
    }
    .resumen-total__info { display: flex; flex-direction: column; gap: .15rem; }
    .resumen-total__etiqueta { font-size: .78rem; color: var(--color-text-muted); }
    .resumen-total__monto { font-size: 1.3rem; color: var(--color-text); }
    .resumen-total__caption { font-size: .75rem; color: var(--color-text-faint); }
    .resumen-total .btn { flex-shrink: 0; }
    .columna-historial { display: flex; flex-direction: column; gap: 1rem; }
    .historial { padding: 1.25rem 1.5rem; }
    .fila-pago--anulada { opacity: .6; }
    .fila-pago--anulada .monto-pago { text-decoration: line-through; }
    .motivo-anulacion {
      margin: -.2rem 0 .5rem; padding-left: .2rem;
      font-size: .74rem; color: var(--color-text-faint);
    }
    .fila-pago { display: flex; align-items: center; gap: .75rem; padding: .55rem 0; border-bottom: 1px solid var(--color-border-light); font-size: .88rem; }
    .fila-pago:last-child { border-bottom: none; }
    .monto-pago { font-weight: 600; flex: 1; }
    .fecha-pago { color: var(--color-text-faint); font-size: .8rem; }
    .vacio { display: flex; flex-direction: column; align-items: center; text-align: center; gap: .3rem; padding: 1.5rem .5rem; }
    .vacio__icono {
      width: 52px; height: 52px; border-radius: 50%; background: var(--color-bg); color: var(--color-text-faint);
      display: flex; align-items: center; justify-content: center; margin-bottom: .5rem;
    }
    .vacio__icono svg { width: 24px; height: 24px; }
    .vacio__titulo { font-weight: 700; font-size: .92rem; }
    .vacio__texto { color: var(--color-text-muted); font-size: .82rem; max-width: 220px; }
    .consejo { margin: 0; }
  `]
})
export class PagosComponent implements OnInit {
  private readonly servicio = inject(PagosService);

  readonly estudiantes = signal<EstudianteOpcionPago[]>([]);
  readonly cargandoEstudiantes = signal(true);
  readonly idEstudiante = signal<number | null>(null);
  readonly opcionesEstudiantes = computed<OpcionBuscable[]>(() =>
    this.estudiantes().map((e) => ({
      id: e.idEstudiante,
      titulo: e.nombreCompleto,
      subtitulo: e.categoria,
    })));

  readonly estudianteSeleccionado = computed(() =>
    this.estudiantes().find((e) => e.idEstudiante === this.idEstudiante()) ?? null,
  );

  readonly tipo = signal<'MEMBRESIA' | 'DIARIO'>('MEMBRESIA');
  readonly meses = Array.from({ length: 12 }, (_, i) => i + 1);
  readonly mesesSeleccionados = signal<Set<number>>(new Set());
  readonly todosLosMesesSeleccionados = computed(() => {
    const disponibles = this.meses.filter((m) => this.mesDisponible(m));
    return disponibles.length > 0 && disponibles.every((m) => this.mesesSeleccionados().has(m));
  });

  readonly aniosDisponibles = (() => {
    const actual = new Date().getFullYear();
    return [actual - 1, actual, actual + 1];
  })();
  anio = new Date().getFullYear();
  readonly mesActual = new Date().getMonth() + 1;
  montoMembresia: number | null = null;
  montoDiario: number | null = null;

  readonly guardando = signal(false);
  readonly error = signal('');
  readonly exito = signal('');

  readonly historial = signal<PagoResponse[]>([]);
  readonly cargandoHistorial = signal(false);

  readonly ingresosMes = signal<IngresosMes | null>(null);

  ngOnInit(): void {
    this.servicio.listarEstudiantes().subscribe({
      next: (estudiantes) => { this.estudiantes.set(estudiantes); this.cargandoEstudiantes.set(false); },
      error: () => this.cargandoEstudiantes.set(false),
    });
    this.cargarIngresosMes();
  }

  private cargarIngresosMes(): void {
    this.servicio.ingresosDelMes().subscribe({
      next: (ingresos) => this.ingresosMes.set(ingresos),
      error: () => {},
    });
  }

  seleccionarEstudiante(id: number): void {
    this.idEstudiante.set(id);
    this.error.set('');
    this.exito.set('');
    this.cargarHistorial(id);
  }

  cambiarEstudiante(): void {
    this.idEstudiante.set(null);
    this.mesesSeleccionados.set(new Set());
    this.montoMembresia = null;
    this.montoDiario = null;
    this.error.set('');
    this.exito.set('');
    this.historial.set([]);
  }

  private cargarHistorial(idEstudiante: number): void {
    this.cargandoHistorial.set(true);
    this.servicio.historialDe(idEstudiante).subscribe({
      next: (historial) => { this.historial.set(historial); this.cargandoHistorial.set(false); },
      error: () => this.cargandoHistorial.set(false),
    });
  }

  motivoMesNoDisponible(mes: number): string | null {
    if (this.mesesPagados().has(mes)) return 'Ya pagado';

    const est = this.estudianteSeleccionado();
    if (!est) return null;

    const [anioIngreso, mesIngreso] = est.fechaIngreso.split('-').map(Number);
    if (this.anio < anioIngreso || (this.anio === anioIngreso && mes < mesIngreso)) {
      return 'Antes de su ingreso';
    }
    return null;
  }

  mesDisponible(mes: number): boolean {
    return this.motivoMesNoDisponible(mes) === null;
  }

  readonly mesesPagados = computed(() => {
    const pagados = new Set<number>();
    for (const p of this.historial()) {
      if (p.tipo === 'MEMBRESIA' && p.anio === this.anio && p.mes) pagados.add(p.mes);
    }
    return pagados;
  });

  readonly tieneMembresiaVigente = computed(() => {
    const hoy = new Date();
    return this.historial().some((p) =>
      p.tipo === 'MEMBRESIA' && p.anio === hoy.getFullYear() && p.mes === hoy.getMonth() + 1);
  });

  alternarMes(mes: number): void {
    if (!this.mesDisponible(mes)) return;

    const actuales = new Set(this.mesesSeleccionados());
    if (actuales.has(mes)) actuales.delete(mes); else actuales.add(mes);
    this.mesesSeleccionados.set(actuales);
  }

  alternarTodosLosMeses(): void {
    const disponibles = this.meses.filter((m) => this.mesDisponible(m));
    const todosPuestos = disponibles.every((m) => this.mesesSeleccionados().has(m));
    this.mesesSeleccionados.set(todosPuestos ? new Set() : new Set(disponibles));
  }

  totalEstimado(): number {
    return (this.montoMembresia ?? 0) * this.mesesSeleccionados().size;
  }

  nombreMes(mes: number): string {
    return NOMBRES_MES[mes - 1] ?? String(mes);
  }

  iniciales(nombreCompleto: string): string {
    return nombreCompleto
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((parte) => parte[0]?.toUpperCase() ?? '')
      .join('');
  }

  registrarMembresia(): void {
    const idEstudiante = this.idEstudiante();
    if (idEstudiante === null || !this.montoMembresia) return;
    this.guardando.set(true);
    this.error.set('');
    this.exito.set('');

    this.servicio.registrarMembresia({
      idEstudiante, anio: this.anio, meses: Array.from(this.mesesSeleccionados()),
      monto: this.montoMembresia, fechaPago: null,
    }).subscribe({
      next: () => {
        this.guardando.set(false);
        this.exito.set('Membresía registrada');
        this.mesesSeleccionados.set(new Set());
        this.cargarHistorial(idEstudiante);
        this.cargarIngresosMes();
      },
      error: (err) => { this.guardando.set(false); this.error.set(this.mensajeDeError(err)); },
    });
  }

  readonly anulando = signal<number | null>(null);

  pedirAnulacion(pago: PagoResponse): void {
    const motivo = window.prompt(
      'Motivo de la anulación (queda registrado en el historial):', '');
    if (motivo === null) return;
    if (!motivo.trim()) {
      this.error.set('Hay que indicar por qué se anula el pago');
      return;
    }

    const idEstudiante = this.idEstudiante();
    if (idEstudiante === null) return;

    this.anulando.set(pago.idPago);
    this.error.set('');
    this.exito.set('');
    this.servicio.anular(pago.idPago, motivo.trim()).subscribe({
      next: () => {
        this.anulando.set(null);
        this.exito.set('Pago anulado. Queda en el historial y ya no cuenta en los totales.');
        this.cargarHistorial(idEstudiante);
      },
      error: (e) => {
        this.anulando.set(null);
        this.error.set(this.mensajeDeError(e));
      },
    });
  }

  registrarDiario(): void {
    const idEstudiante = this.idEstudiante();
    if (idEstudiante === null || !this.montoDiario) return;
    this.guardando.set(true);
    this.error.set('');
    this.exito.set('');

    this.servicio.registrarDiario({ idEstudiante, monto: this.montoDiario, fechaPago: null }).subscribe({
      next: () => {
        this.guardando.set(false);
        this.exito.set('Pago diario registrado');
        this.montoDiario = null;
        this.cargarHistorial(idEstudiante);
        this.cargarIngresosMes();
      },
      error: (err) => { this.guardando.set(false); this.error.set(this.mensajeDeError(err)); },
    });
  }

  private mensajeDeError(err: unknown): string {
    return traducirError(err);
  }
}
