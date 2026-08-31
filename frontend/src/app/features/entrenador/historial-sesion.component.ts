import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CargandoComponent } from '../../core/cargando.component';
import { mensajeDeError } from '../../core/mensaje-error';
import { horaCorta, inicialesDe } from '../../core/formato-texto';
import { SesionesService } from './sesiones.service';
import { HistorialSesion, FilaAsistenciaHistorial } from './sesiones.models';

const ETIQUETA_ESTADO: Record<string, string> = {
  PRESENTE: 'Presente',
  TARDE: 'Llegó tarde',
  AUSENTE: 'Ausente',
  JUSTIFICADO: 'Justificado',
  SIN_REGISTRO: 'Sin registro',
};

const COLOR_ESTADO: Record<string, string> = {
  PRESENTE: 'success',
  TARDE: 'warning',
  AUSENTE: 'danger',
  JUSTIFICADO: 'info',
  SIN_REGISTRO: 'info',
};

@Component({
  selector: 'app-historial-sesion',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, CargandoComponent],
  template: `
    <div class="pantalla">
      <a class="btn btn--ghost volver" routerLink="/entrenador/sesiones">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="19" y1="12" x2="5" y2="12"></line><polyline points="12 19 5 12 12 5"></polyline></svg>
        Volver a sesiones
      </a>

      @if (cargando()) {
        <app-cargando mensaje="Cargando el historial…" />
      } @else if (error()) {
        <p class="alert alert--danger">{{ error() }}</p>
      } @else if (historial(); as h) {
        <header class="cabecera">
          <div>
            <h1>{{ h.categoria }} · {{ h.fecha }}</h1>
            <p class="subt">
              {{ h.entrenador }}
              @if (h.horaInicio) { · {{ horaCorta(h.horaInicio) }}@if (h.horaFin) {–{{ horaCorta(h.horaFin) }}} }
              @if (h.campo) { · {{ h.campo }} }
            </p>
          </div>
          <a class="btn btn--ghost btn--sm" [routerLink]="['/entrenador/sesion', h.idSesion]">
            {{ h.tieneEvaluacion ? 'Ver evaluación' : 'Evaluar' }}
          </a>
        </header>

        <section class="resumen">
          <div class="dato dato--fuerte">
            <span class="cifra">{{ h.resumen.presentes + h.resumen.tarde }}</span>
            <span class="rotulo">entrenaron</span>
          </div>
          <div class="dato">
            <span class="cifra">{{ h.resumen.convocados }}</span>
            <span class="rotulo">convocados</span>
          </div>
          <div class="dato">
            <span class="cifra">{{ h.resumen.tarde }}</span>
            <span class="rotulo">tarde</span>
          </div>
          <div class="dato">
            <span class="cifra">{{ h.resumen.ausentes }}</span>
            <span class="rotulo">ausentes</span>
          </div>
          <div class="dato">
            <span class="cifra">{{ h.resumen.justificados }}</span>
            <span class="rotulo">justificados</span>
          </div>
          @if (h.resumen.sinRegistro > 0) {
            <div class="dato dato--aviso">
              <span class="cifra">{{ h.resumen.sinRegistro }}</span>
              <span class="rotulo">sin registro</span>
            </div>
          }
        </section>

        @if (h.resumen.sinRegistro === h.resumen.convocados && h.resumen.convocados > 0) {
          <p class="alert alert--warning">
            Nadie pasó lista en esta sesión. Eso no es lo mismo que “no vino nadie”:
            sin registro, la asistencia de ese día no cuenta para nada.
            <a [routerLink]="['/entrenador/sesion', h.idSesion, 'asistencia']">Pasar lista ahora</a>
          </p>
        }

        <div class="filtros">
          @for (f of filtros; track f.valor) {
            <button type="button" class="chip" [class.chip--activo]="filtro() === f.valor"
                    (click)="filtro.set(f.valor)">{{ f.etiqueta }}</button>
          }
        </div>

        @if (h.asistencias.length > UMBRAL_BUSCADOR) {
          <input class="buscar" type="search" [ngModel]="busqueda()"
                 (ngModelChange)="busqueda.set($event)"
                 [attr.placeholder]="'Buscar entre ' + h.asistencias.length + ' convocados…'"
                 aria-label="Buscar jugador" />
        }

        <section class="card lista">
          @if (filas().length === 0) {
            <p class="aviso">
              @if (busqueda()) { Nadie coincide con «{{ busqueda() }}». }
              @else { No hay jugadores en este filtro. }
            </p>
          } @else {
            <div class="lista-scroll">
            @for (f of visibles(); track f.idEstudiante) {
              <div class="fila">
                <span class="avatar avatar--muted">{{ iniciales(f.nombreCompleto) }}</span>
                <div class="fila-info">
                  <span class="nombre">
                    {{ f.nombreCompleto }}
                    @if (f.posicion) { <span class="puesto">{{ f.posicion }}</span> }
                  </span>
                  @if (f.observacion) { <span class="obs">{{ f.observacion }}</span> }
                </div>
                <span class="hora">
                  @if (f.horaEntrada) {
                    {{ horaCorta(f.horaEntrada) }}
                    <span class="metodo" title="Lo midió el lector de QR">QR</span>
                  } @else if (f.metodo === 'MANUAL') {
                    <span class="metodo" title="Lo marcó el entrenador a mano">manual</span>
                  }
                </span>
                <span class="badge" [class]="'badge badge--' + color(f.estado)">
                  {{ etiqueta(f.estado) }}
                </span>
              </div>
            }
            </div>

            @if (visibles().length < filas().length) {
              <p class="aviso recorte">
                Se muestran {{ visibles().length }} de {{ filas().length }}.
                @if (!busqueda()) { Buscá por nombre para llegar al resto. }
              </p>
            }
          }
        </section>
      }
    </div>
  `,
  styles: [`
    .pantalla { max-width: 780px; margin: 0 auto; padding: 1.25rem 1rem 3rem; }
    .volver { margin-bottom: 1rem; }
    .cabecera { display: flex; justify-content: space-between; align-items: flex-start;
                gap: 1rem; flex-wrap: wrap; margin-bottom: 1rem; }
    h1 { font-size: 1.15rem; }
    .subt { margin-top: .3rem; color: var(--color-text-muted); font-size: .85rem; }
    .resumen { display: flex; gap: .55rem; flex-wrap: wrap; margin-bottom: 1rem; }
    .dato { flex: 1 1 82px; padding: .65rem .5rem; text-align: center;
            background: var(--color-surface); border: 1px solid var(--color-border-light);
            border-radius: var(--radius-md); }
    .dato--fuerte { border-color: var(--color-primary-600); }
    .dato--aviso { border-color: var(--color-warning-line, #f59e0b); }
    .cifra { display: block; font-size: 1.4rem; font-weight: 700;
             font-variant-numeric: tabular-nums; line-height: 1.15; }
    .rotulo { display: block; font-size: .7rem; color: var(--color-text-muted);
              text-transform: uppercase; letter-spacing: .04em; }
    .filtros { display: flex; gap: .4rem; flex-wrap: wrap; margin-bottom: .8rem; }
    .chip { padding: .3rem .7rem; font-size: .78rem; border-radius: 999px; cursor: pointer;
            border: 1px solid var(--color-border); background: var(--color-surface);
            color: var(--color-text-muted); }
    .chip--activo { border-color: var(--color-primary-600); color: var(--color-primary-700);
                    background: var(--color-primary-50); }
    .buscar { width: 100%; padding: .45rem .65rem; font-size: .85rem; margin-bottom: .7rem;
              border: 1px solid var(--color-border); border-radius: var(--radius-sm);
              background: var(--color-surface); color: var(--color-text); }
    .lista { padding: .8rem 1rem; }
    .lista-scroll { max-height: 28rem; overflow-y: auto; overscroll-behavior: contain; }
    .recorte { margin: .6rem 0 0; }
    .fila { display: flex; align-items: center; gap: .7rem; padding: .55rem 0;
            border-bottom: 1px solid var(--color-border-light); font-size: .9rem; }
    .fila:last-child { border-bottom: none; }
    .fila-info { flex: 1; min-width: 0; display: flex; flex-direction: column; }
    .nombre { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .puesto { font-size: .7rem; color: var(--color-text-faint); margin-left: .35rem;
              font-family: ui-monospace, monospace; }
    .obs { font-size: .74rem; color: var(--color-text-muted); }
    .hora { font-size: .76rem; color: var(--color-text-muted); white-space: nowrap;
            font-variant-numeric: tabular-nums; }
    .metodo { font-size: .65rem; text-transform: uppercase; letter-spacing: .05em;
              color: var(--color-text-faint); margin-left: .25rem; }
    .aviso { color: var(--color-text-muted); font-size: .88rem; padding: .5rem 0; }
  `],
})
export class HistorialSesionComponent implements OnInit {
  private readonly ruta = inject(ActivatedRoute);
  private readonly servicio = inject(SesionesService);

  readonly horaCorta = horaCorta;
  readonly historial = signal<HistorialSesion | null>(null);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly filtro = signal<'TODOS' | 'ENTRENARON' | 'FALTARON' | 'SIN_REGISTRO'>('TODOS');
  readonly busqueda = signal('');

  readonly UMBRAL_BUSCADOR = 25;
  private readonly TOPE_FILAS = 80;

  readonly filtros = [
    { valor: 'TODOS' as const, etiqueta: 'Todos' },
    { valor: 'ENTRENARON' as const, etiqueta: 'Entrenaron' },
    { valor: 'FALTARON' as const, etiqueta: 'Faltaron' },
    { valor: 'SIN_REGISTRO' as const, etiqueta: 'Sin registro' },
  ];

  readonly visibles = computed<FilaAsistenciaHistorial[]>(
    () => this.filas().slice(0, this.TOPE_FILAS));

  readonly filas = computed<FilaAsistenciaHistorial[]>(() => {
    const h = this.historial();
    if (!h) return [];
    const texto = this.busqueda().trim().toLowerCase();
    const porNombre = (f: FilaAsistenciaHistorial) =>
      !texto || f.nombreCompleto.toLowerCase().includes(texto);
    switch (this.filtro()) {
      case 'ENTRENARON':
        return h.asistencias.filter((f) => porNombre(f)
          && (f.estado === 'PRESENTE' || f.estado === 'TARDE'));
      case 'FALTARON':
        return h.asistencias.filter((f) => porNombre(f)
          && (f.estado === 'AUSENTE' || f.estado === 'JUSTIFICADO'));
      case 'SIN_REGISTRO':
        return h.asistencias.filter((f) => porNombre(f) && f.estado === 'SIN_REGISTRO');
      default:
        return h.asistencias.filter(porNombre);
    }
  });

  ngOnInit(): void {
    const idSesion = Number(this.ruta.snapshot.paramMap.get('idSesion'));
    this.servicio.historial(idSesion).subscribe({
      next: (h) => { this.historial.set(h); this.cargando.set(false); },
      error: (e) => {
        this.cargando.set(false);
        this.error.set(e.status === 404
          ? 'Esa sesión no existe.'
          : mensajeDeError(e, 'No se pudo cargar el historial.'));
      },
    });
  }

  iniciales(nombre: string): string {
    return inicialesDe(nombre);
  }

  etiqueta(estado: string): string {
    return ETIQUETA_ESTADO[estado] ?? estado;
  }

  color(estado: string): string {
    return COLOR_ESTADO[estado] ?? 'info';
  }
}
