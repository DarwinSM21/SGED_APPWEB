import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { CargandoComponent } from '../../core/cargando.component';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AsistenciaSesionService } from './asistencia.service';
import { EstadoAsistencia, Nomina } from './asistencia.models';
import { horaCorta } from './plantilla.models';

/** Un estado por columna, en el orden en que se usan de verdad. */
const ESTADOS: { valor: EstadoAsistencia; etiqueta: string; plural: string; corta: string }[] = [
  { valor: 'PRESENTE', etiqueta: 'Presente', plural: 'presentes', corta: 'P' },
  { valor: 'TARDE', etiqueta: 'Tarde', plural: 'tarde', corta: 'T' },
  { valor: 'AUSENTE', etiqueta: 'Ausente', plural: 'ausentes', corta: 'A' },
  { valor: 'JUSTIFICADO', etiqueta: 'Justificado', plural: 'justificados', corta: 'J' },
];

@Component({
  selector: 'app-lista-asistencia',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule, RouterLink, CargandoComponent],
  template: `
    <div class="pantalla">
      @if (cargando()) {
        <app-cargando mensaje="Cargando la nómina…" />
      } @else if (error()) {
        <p class="alert alert--danger">{{ error() }}</p>
      } @else if (nomina(); as n) {

        <a class="btn btn--ghost volver" [routerLink]="['/entrenador/sesion', n.idSesion]">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="19" y1="12" x2="5" y2="12"></line><polyline points="12 19 5 12 12 5"></polyline></svg>
          Volver a la evaluación
        </a>

        <header class="cabecera">
          <div>
            <h1>Lista de asistencia</h1>
            <p class="subt">
              {{ n.categoria }} · {{ n.fecha }}
              @if (n.horaInicio) { · {{ hora(n.horaInicio) }} }
            </p>
          </div>
          <div class="resumen" aria-live="polite">
            <span class="conteo">{{ presentes() }}<span class="de">/{{ n.filas.length }}</span></span>
            <span class="conteo-etiqueta">en la cancha</span>
          </div>
        </header>

        @if (!n.editable) {
          <p class="alert alert--warning">{{ n.motivoNoEditable }}</p>
        } @else if (n.filas.length > 0) {
          <div class="atajos">
            <span class="atajos-titulo">Empezar por:</span>
            @for (e of estados; track e.valor) {
              <button type="button" class="btn btn--ghost btn--sm"
                      (click)="marcarTodos(e.valor)">Todos {{ e.plural }}</button>
            }
          </div>
        }

        <ul class="nomina">
          @for (f of n.filas; track f.idEstudiante) {
            <li class="fila" [class.fila--sin-marcar]="!marcas()[f.idEstudiante]">
              <div class="quien">
                <span class="avatar">{{ iniciales(f.nombreCompleto) }}</span>
                <div>
                  <span class="nombre">{{ f.nombreCompleto }}</span>
                  @if (f.metodo === 'QR' && f.horaEntrada) {
                    <span class="origen" title="Lo marcó el propio estudiante al escanear el QR">
                      escaneó a las {{ hora(f.horaEntrada) }}
                    </span>
                  } @else if (!marcas()[f.idEstudiante]) {
                    <span class="origen origen--pendiente">sin marcar</span>
                  }
                </div>
              </div>

              <div class="opciones" role="group" [attr.aria-label]="'Asistencia de ' + f.nombreCompleto">
                @for (e of estados; track e.valor) {
                  <button type="button"
                          [class]="claseOpcion(f.idEstudiante, e.valor)"
                          [disabled]="!n.editable"
                          [attr.aria-pressed]="marcas()[f.idEstudiante] === e.valor"
                          (click)="marcar(f.idEstudiante, e.valor)">
                    <span class="opcion-larga">{{ e.etiqueta }}</span>
                    <span class="opcion-corta" aria-hidden="true">{{ e.corta }}</span>
                  </button>
                }
              </div>

              @if (necesitaNota(f.idEstudiante)) {
                <input class="nota" type="text" maxlength="255"
                       [disabled]="!n.editable"
                       [ngModel]="notas()[f.idEstudiante] ?? ''"
                       (ngModelChange)="anotar(f.idEstudiante, $event)"
                       [placeholder]="marcas()[f.idEstudiante] === 'JUSTIFICADO'
                          ? 'Motivo de la justificación' : 'Observación (opcional)'" />
              }
            </li>
          }
        </ul>

        @if (n.filas.length === 0) {
          <p class="alert alert--info">
            No hay estudiantes activos en {{ n.categoria }}. Matricula estudiantes en esta
            categoría desde Personas y aparecerán aquí.
          </p>
        }

        @if (n.editable && n.filas.length > 0) {
          <div class="pie">
            @if (mensaje(); as m) { <span class="ok">{{ m }}</span> }
            <button type="button" class="btn btn--primary"
                    [disabled]="!hayCambios() || guardando()"
                    (click)="guardar()">
              {{ guardando() ? 'Guardando…' : 'Guardar lista' }}
            </button>
          </div>
        }
      }
    </div>
  `,
  styles: [`
    .pantalla { max-width: 780px; margin: 0 auto; padding: 1.25rem 1rem 4rem; }
    .volver { margin-bottom: 1rem; gap: .4rem; }
    .volver svg { width: 16px; height: 16px; }
    .cabecera { display: flex; justify-content: space-between; align-items: flex-end; gap: 1rem; margin-bottom: 1.25rem; }
    h1 { font-size: 1.4rem; margin: 0; }
    .subt { color: var(--color-text-muted); font-size: .88rem; margin: .25rem 0 0; }
    .resumen { text-align: right; }
    .conteo { font-size: 1.75rem; font-weight: 700; font-variant-numeric: tabular-nums; line-height: 1; }
    .conteo .de { font-size: 1rem; font-weight: 500; color: var(--color-text-faint); }
    .conteo-etiqueta { display: block; font-size: .72rem; text-transform: uppercase; letter-spacing: .05em; color: var(--color-text-muted); margin-top: .2rem; }

    .atajos { display: flex; flex-wrap: wrap; align-items: center; gap: .4rem; margin-bottom: 1rem; }
    .atajos-titulo { font-size: .78rem; color: var(--color-text-muted); margin-right: .2rem; }
    .btn--sm { padding: .3rem .6rem; font-size: .76rem; }

    .nomina { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: .5rem; }
    .fila { display: grid; grid-template-columns: 1fr auto; gap: .6rem; align-items: center;
            padding: .7rem .85rem; border: 1px solid var(--color-border); border-radius: 10px;
            background: var(--color-surface); }
    /* Una barra al canto senala de un vistazo a quien falta, sin depender del color. */
    .fila--sin-marcar { border-left: 3px solid var(--color-warning); }
    .quien { display: flex; align-items: center; gap: .6rem; min-width: 0; }
    .avatar { display: grid; place-items: center; width: 34px; height: 34px; border-radius: 50%;
              background: var(--color-border-light); color: var(--color-text-muted);
              font-size: .74rem; font-weight: 600; flex-shrink: 0; }
    .nombre { display: block; font-weight: 500; font-size: .92rem; }
    .origen { font-size: .72rem; color: var(--color-text-faint); }
    .origen--pendiente { color: var(--color-warning-text); }

    .opciones { display: flex; gap: 2px; }
    .opcion { border: 1px solid var(--color-border); background: var(--color-surface);
              color: var(--color-text-muted); padding: .35rem .6rem; font-size: .78rem;
              cursor: pointer; transition: background .12s, color .12s, border-color .12s; }
    .opcion:first-child { border-radius: 7px 0 0 7px; }
    .opcion:last-child { border-radius: 0 7px 7px 0; }
    .opcion:disabled { cursor: not-allowed; opacity: .55; }
    .opcion:not(:disabled):hover { border-color: var(--color-text-faint); }
    .opcion--activa { font-weight: 600; }
    .opcion--activa.opcion--presente { background: var(--color-success-bg); color: var(--color-success-text); border-color: var(--color-success); }
    .opcion--activa.opcion--tarde { background: var(--color-warning-bg); color: var(--color-warning-text); border-color: var(--color-warning); }
    .opcion--activa.opcion--ausente { background: var(--color-danger-bg); color: var(--color-danger-text); border-color: var(--color-danger); }
    .opcion--activa.opcion--justificado { background: var(--color-info-bg); color: var(--color-info-text); border-color: var(--color-info); }
    .opcion-corta { display: none; }

    .nota { grid-column: 1 / -1; width: 100%; padding: .4rem .6rem; font-size: .82rem;
            border: 1px solid var(--color-border); border-radius: 7px;
            background: var(--color-surface); color: var(--color-text); }

    .pie { position: sticky; bottom: 0; display: flex; justify-content: flex-end; align-items: center;
           gap: .75rem; padding: .9rem 0 .2rem; margin-top: 1rem;
           background: linear-gradient(to top, var(--color-bg) 65%, transparent); }
    .ok { font-size: .82rem; color: var(--color-success-text); }

    /* En el celular, que es donde se pasa lista de verdad, las cuatro etiquetas
       no caben junto al nombre: se reducen a su inicial y la fila respira. */
    @media (max-width: 560px) {
      .fila { grid-template-columns: 1fr; }
      .opciones { justify-content: stretch; }
      .opcion { flex: 1; padding: .45rem 0; text-align: center; }
      .opcion-larga { display: none; }
      .opcion-corta { display: inline; font-size: .85rem; }
    }
  `],
})
export class ListaAsistenciaComponent {

  private readonly ruta = inject(ActivatedRoute);
  private readonly servicio = inject(AsistenciaSesionService);

  readonly estados = ESTADOS;

  readonly cargando = signal(true);
  readonly guardando = signal(false);
  readonly error = signal<string | null>(null);
  readonly mensaje = signal<string | null>(null);
  readonly nomina = signal<Nomina | null>(null);

  /** Lo que el entrenador tiene puesto ahora, contra lo que vino del servidor. */
  readonly marcas = signal<Record<number, EstadoAsistencia | undefined>>({});
  readonly notas = signal<Record<number, string>>({});
  private original: Record<number, EstadoAsistencia | undefined> = {};
  private notasOriginales: Record<number, string> = {};

  readonly presentes = computed(() =>
    Object.values(this.marcas()).filter((e) => e === 'PRESENTE' || e === 'TARDE').length);

  readonly hayCambios = computed(() => {
    const m = this.marcas();
    const n = this.notas();
    const ids = new Set([...Object.keys(m), ...Object.keys(this.original)].map(Number));
    for (const id of ids) {
      if (m[id] !== this.original[id]) return true;
      if ((n[id] ?? '') !== (this.notasOriginales[id] ?? '')) return true;
    }
    return false;
  });

  constructor() {
    this.cargar(Number(this.ruta.snapshot.paramMap.get('idSesion')));
  }

  private cargar(idSesion: number): void {
    this.cargando.set(true);
    this.servicio.nomina(idSesion).subscribe({
      next: (n) => this.recibir(n),
      error: () => {
        this.error.set('No se pudo cargar la nómina de esta sesión.');
        this.cargando.set(false);
      },
    });
  }

  private recibir(n: Nomina): void {
    const marcas: Record<number, EstadoAsistencia | undefined> = {};
    const notas: Record<number, string> = {};
    for (const f of n.filas) {
      if (f.estado) marcas[f.idEstudiante] = f.estado;
      if (f.observacion) notas[f.idEstudiante] = f.observacion;
    }
    this.nomina.set(n);
    this.marcas.set(marcas);
    this.notas.set(notas);
    this.original = { ...marcas };
    this.notasOriginales = { ...notas };
    this.cargando.set(false);
  }

  claseOpcion(idEstudiante: number, estado: EstadoAsistencia): string {
    const activa = this.marcas()[idEstudiante] === estado ? ' opcion--activa' : '';
    return `opcion opcion--${estado.toLowerCase()}${activa}`;
  }

  marcar(idEstudiante: number, estado: EstadoAsistencia): void {
    this.mensaje.set(null);
    this.marcas.update((m) => ({ ...m, [idEstudiante]: estado }));
  }

  /**
   * El caso normal es que casi todos vinieron: se marca todo de una y se
   * corrigen las dos o tres excepciones, en vez de tocar veinte veces.
   */
  marcarTodos(estado: EstadoAsistencia): void {
    const n = this.nomina();
    if (!n) return;
    this.mensaje.set(null);
    const m: Record<number, EstadoAsistencia> = {};
    for (const f of n.filas) m[f.idEstudiante] = estado;
    this.marcas.set(m);
  }

  anotar(idEstudiante: number, texto: string): void {
    this.notas.update((n) => ({ ...n, [idEstudiante]: texto }));
  }

  /** La nota solo estorba mientras no haya algo que explicar. */
  necesitaNota(idEstudiante: number): boolean {
    const e = this.marcas()[idEstudiante];
    return e === 'AUSENTE' || e === 'JUSTIFICADO' || e === 'TARDE';
  }

  guardar(): void {
    const n = this.nomina();
    if (!n) return;
    const m = this.marcas();
    const notas = this.notas();
    const marcas = n.filas
      .filter((f) => m[f.idEstudiante])
      .map((f) => ({
        idEstudiante: f.idEstudiante,
        estado: m[f.idEstudiante]!,
        observacion: notas[f.idEstudiante]?.trim() || null,
      }));

    if (marcas.length === 0) {
      this.error.set('Marca al menos un estudiante antes de guardar.');
      return;
    }

    this.guardando.set(true);
    this.error.set(null);
    this.servicio.pasarLista(n.idSesion, marcas).subscribe({
      next: (actualizada) => {
        this.recibir(actualizada);
        this.guardando.set(false);
        this.mensaje.set('Lista guardada');
      },
      error: (e) => {
        this.error.set(e?.error?.detail ?? 'No se pudo guardar la lista.');
        this.guardando.set(false);
      },
    });
  }

  hora(valor: string): string {
    return horaCorta(valor) ?? valor;
  }

  iniciales(nombre: string): string {
    return nombre.split(' ').filter(Boolean).slice(0, 2).map((p) => p[0]).join('').toUpperCase();
  }
}
