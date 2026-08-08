import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PagosService } from './pagos.service';
import { EstudianteOpcionPago, PagoResponse } from './pagos.models';

const NOMBRES_MES = [
  'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
  'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre',
];

/**
 * Pagos: membresía mensual (cubre meses exactos, uno o varios a la vez)
 * o diario/eventual (un solo día, sin período). El backend rechaza todo
 * el lote si algún mes de la membresía ya está cubierto -no se cobra a
 * medias-, así que aquí se muestra ese error tal cual llega.
 */
@Component({
  selector: 'app-pagos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="pantalla">
      <h1 class="titulo-pantalla">Pagos</h1>

      <div class="card formulario">
        <label class="field" for="idEstudiante">
          <span class="field__label">Estudiante</span>
          <span class="field__control">
            @if (cargandoEstudiantes()) {
              <span class="aviso">Cargando…</span>
            } @else {
              <select id="idEstudiante" [ngModel]="idEstudiante()" (ngModelChange)="seleccionarEstudiante($event)" name="idEstudiante">
                <option [ngValue]="null" disabled>Selecciona…</option>
                @for (e of estudiantes(); track e.idEstudiante) {
                  <option [ngValue]="e.idEstudiante">{{ e.nombreCompleto }} · {{ e.categoria }}</option>
                }
              </select>
            }
          </span>
        </label>

        @if (idEstudiante() !== null) {
          <div class="tabs">
            <button type="button" class="tab" [class.tab--activo]="tipo() === 'MEMBRESIA'" (click)="tipo.set('MEMBRESIA')">Membresía mensual</button>
            <button type="button" class="tab" [class.tab--activo]="tipo() === 'DIARIO'" (click)="tipo.set('DIARIO')">Diario / eventual</button>
          </div>

          @if (tipo() === 'MEMBRESIA') {
            <div class="fila-2">
              <label class="field" for="anio">
                <span class="field__label">Año</span>
                <span class="field__control"><input id="anio" type="number" [(ngModel)]="anio" name="anio" /></span>
              </label>
              <label class="field" for="montoMembresia">
                <span class="field__label">Monto por mes</span>
                <span class="field__control"><input id="montoMembresia" type="number" step="0.01" min="0.01" [(ngModel)]="montoMembresia" name="montoMembresia" /></span>
              </label>
            </div>
            <span class="field__label">Meses a cubrir</span>
            <div class="meses">
              @for (m of meses; track m) {
                <label class="opcion-mes">
                  <input type="checkbox" [checked]="mesesSeleccionados().has(m)" (change)="alternarMes(m)" />
                  {{ nombreMes(m) }}
                </label>
              }
            </div>
            <button class="btn btn--primary" type="button" [disabled]="guardando() || mesesSeleccionados().size === 0" (click)="registrarMembresia()">
              @if (guardando()) { <span class="spinner"></span> Guardando… } @else { Registrar membresía }
            </button>
          } @else {
            <label class="field" for="montoDiario">
              <span class="field__label">Monto</span>
              <span class="field__control"><input id="montoDiario" type="number" step="0.01" min="0.01" [(ngModel)]="montoDiario" name="montoDiario" /></span>
            </label>
            <button class="btn btn--primary" type="button" [disabled]="guardando()" (click)="registrarDiario()">
              @if (guardando()) { <span class="spinner"></span> Guardando… } @else { Registrar pago diario }
            </button>
          }

          @if (error()) { <div class="alert alert--danger" role="alert">{{ error() }}</div> }
          @if (exito()) { <div class="alert alert--success" role="status">{{ exito() }}</div> }
        }
      </div>

      @if (idEstudiante() !== null) {
        <div class="card historial">
          <h2 class="subtitulo">Historial de pagos</h2>
          @if (cargandoHistorial()) {
            <p class="aviso">Cargando…</p>
          } @else if (historial().length === 0) {
            <p class="aviso">Sin pagos registrados todavía.</p>
          } @else {
            @for (p of historial(); track p.idPago) {
              <div class="fila-pago">
                <span class="badge" [class.badge--info]="p.tipo === 'MEMBRESIA'" [class.badge--success]="p.tipo === 'DIARIO'">
                  {{ p.tipo === 'MEMBRESIA' ? (nombreMes(p.mes!) + ' ' + p.anio) : 'Diario' }}
                </span>
                <span class="monto-pago">{{ p.monto | number: '1.2-2' }}</span>
                <span class="fecha-pago">{{ p.fechaPago }}</span>
              </div>
            }
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .pantalla { max-width: 700px; margin: 0 auto; padding: 1.5rem 1.25rem 3rem; display: flex; flex-direction: column; gap: 1.25rem; }
    .titulo-pantalla { font-size: 1.2rem; }
    .subtitulo { font-size: .95rem; margin-bottom: .75rem; }

    .formulario { padding: 1.5rem; display: flex; flex-direction: column; gap: .9rem; }
    .fila-2 { display: grid; grid-template-columns: 1fr 1fr; gap: .85rem; }
    @media (max-width: 480px) { .fila-2 { grid-template-columns: 1fr; } }

    .field__control select { flex: 1; border: none; outline: none; padding: .75rem 0; font-size: .95rem; background: transparent; color: var(--color-text); width: 100%; }
    .aviso { color: var(--color-text-muted); font-size: .85rem; }

    .tabs { display: flex; gap: .4rem; border-bottom: 1px solid var(--color-border-light); padding-bottom: .1rem; }
    .tab {
      border: none; background: none; padding: .55rem .9rem; font-size: .87rem; font-weight: 600;
      color: var(--color-text-muted); cursor: pointer; border-bottom: 2px solid transparent;
    }
    .tab--activo { color: var(--color-primary-700); border-bottom-color: var(--color-primary-500); }

    .meses { display: grid; grid-template-columns: repeat(3, 1fr); gap: .4rem .75rem; }
    @media (max-width: 480px) { .meses { grid-template-columns: repeat(2, 1fr); } }
    .opcion-mes { display: flex; align-items: center; gap: .5rem; font-size: .85rem; cursor: pointer; }

    .historial { padding: 1.25rem 1.5rem; }
    .fila-pago { display: flex; align-items: center; gap: .75rem; padding: .55rem 0; border-bottom: 1px solid var(--color-border-light); font-size: .88rem; }
    .fila-pago:last-child { border-bottom: none; }
    .monto-pago { font-weight: 600; flex: 1; }
    .fecha-pago { color: var(--color-text-faint); font-size: .8rem; }
  `]
})
export class PagosComponent implements OnInit {

  private readonly servicio = inject(PagosService);

  readonly estudiantes = signal<EstudianteOpcionPago[]>([]);
  readonly cargandoEstudiantes = signal(true);
  readonly idEstudiante = signal<number | null>(null);

  readonly tipo = signal<'MEMBRESIA' | 'DIARIO'>('MEMBRESIA');
  readonly meses = Array.from({ length: 12 }, (_, i) => i + 1);
  readonly mesesSeleccionados = signal<Set<number>>(new Set());
  anio = new Date().getFullYear();
  montoMembresia: number | null = null;
  montoDiario: number | null = null;

  readonly guardando = signal(false);
  readonly error = signal('');
  readonly exito = signal('');

  readonly historial = signal<PagoResponse[]>([]);
  readonly cargandoHistorial = signal(false);

  ngOnInit(): void {
    this.servicio.listarEstudiantes().subscribe({
      next: (estudiantes) => { this.estudiantes.set(estudiantes); this.cargandoEstudiantes.set(false); },
      error: () => this.cargandoEstudiantes.set(false),
    });
  }

  seleccionarEstudiante(id: number): void {
    this.idEstudiante.set(id);
    this.error.set('');
    this.exito.set('');
    this.cargarHistorial(id);
  }

  private cargarHistorial(idEstudiante: number): void {
    this.cargandoHistorial.set(true);
    this.servicio.historialDe(idEstudiante).subscribe({
      next: (historial) => { this.historial.set(historial); this.cargandoHistorial.set(false); },
      error: () => this.cargandoHistorial.set(false),
    });
  }

  alternarMes(mes: number): void {
    const actuales = new Set(this.mesesSeleccionados());
    if (actuales.has(mes)) actuales.delete(mes); else actuales.add(mes);
    this.mesesSeleccionados.set(actuales);
  }

  nombreMes(mes: number): string {
    return NOMBRES_MES[mes - 1] ?? String(mes);
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
      },
      error: (err) => { this.guardando.set(false); this.error.set(this.mensajeDeError(err)); },
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
      },
      error: (err) => { this.guardando.set(false); this.error.set(this.mensajeDeError(err)); },
    });
  }

  private mensajeDeError(err: any): string {
    return err?.error?.detail ?? 'Error del servidor';
  }
}
