import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CargandoComponent } from '../../core/cargando.component';
import { mensajeDeError } from '../../core/mensaje-error';
import { horaCorta } from '../../core/formato-texto';
import { SesionesService } from '../entrenador/sesiones.service';
import { CategoriaOpcion } from '../entrenador/sesiones.models';
import { PartidosService } from './partidos.service';
import { Partido } from './partidos.models';

@Component({
  selector: 'app-partidos',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, CargandoComponent],
  template: `
    <div class="pantalla">
      <header class="cabecera">
        <div>
          <h1>Partidos</h1>
          <p class="subt">
            Agendá el partido, generá la plantilla con lo que viene rindiendo cada
            uno y cargá el resultado cuando termine.
          </p>
        </div>
        <button type="button" class="btn btn--primary" (click)="alternarFormulario()">
          {{ mostrandoFormulario() ? 'Cancelar' : '+ Nuevo partido' }}
        </button>
      </header>

      @if (mostrandoFormulario()) {
        <section class="card formulario">
          <h2>Agendar partido</h2>
          <div class="fila-form">
            <label class="field">
              <span class="field__label">Categoría</span>
              <select class="field__control" [(ngModel)]="nuevaCategoria" name="categoria">
                <option [ngValue]="null" disabled>Elegí una categoría</option>
                @for (c of categorias(); track c.idCategoria) {
                  <option [ngValue]="c.idCategoria">{{ c.nombre }}</option>
                }
              </select>
            </label>

            <label class="field">
              <span class="field__label">Fecha</span>
              <input class="field__control" type="date" [(ngModel)]="nuevaFecha" name="fecha" />
            </label>

            <label class="field">
              <span class="field__label">Hora <span class="opcional">(opcional)</span></span>
              <input class="field__control" type="time" [(ngModel)]="nuevaHora" name="hora" />
            </label>
          </div>

          <label class="field">
            <span class="field__label">Nota <span class="opcional">(opcional)</span></span>
            <input class="field__control" type="text" maxlength="500" name="observacion"
                   [(ngModel)]="nuevaObservacion" placeholder="Amistoso, torneo interno…" />
          </label>

          @if (errorFormulario()) { <p class="alert alert--danger">{{ errorFormulario() }}</p> }

          <button type="button" class="btn btn--primary" [disabled]="guardando()" (click)="crear()">
            {{ guardando() ? 'Guardando…' : 'Agendar' }}
          </button>
        </section>
      }

      <div class="filtro">
        <label class="field field--inline">
          <span class="field__label">Categoría</span>
          <select class="field__control" [ngModel]="filtroCategoria()"
                  (ngModelChange)="filtrar($event)" name="filtro">
            <option [ngValue]="null">Todas</option>
            @for (c of categorias(); track c.idCategoria) {
              <option [ngValue]="c.idCategoria">{{ c.nombre }}</option>
            }
          </select>
        </label>
      </div>

      @if (cargando()) {
        <app-cargando mensaje="Cargando los partidos…" />
      } @else if (error()) {
        <p class="alert alert--danger">{{ error() }}</p>
      } @else if (partidos().length === 0) {
        <div class="card vacio">
          <h2>Todavía no hay partidos</h2>
          <p>
            Agendá el primero y el sistema arma la convocatoria con el promedio y la
            asistencia de las últimas semanas.
          </p>
        </div>
      } @else {
        <ul class="lista">
          @for (p of partidos(); track p.idPartido) {
            <li class="partido" [class.partido--jugado]="p.resultado !== 'PENDIENTE'">
              <div class="partido__fecha">
                <span class="dia">{{ diaDe(p.fecha) }}</span>
                <span class="mes">{{ mesDe(p.fecha) }}</span>
              </div>

              <div class="partido__cuerpo">
                <div class="partido__titulo">
                  <strong>{{ p.categoria }}</strong>
                  <span class="badge" [class]="'badge badge--' + colorDe(p.resultado)">
                    {{ etiquetaDe(p) }}
                  </span>
                </div>
                <p class="partido__meta">
                  @if (p.hora) { {{ horaCorta(p.hora) }} · }
                  @if (p.tieneAlineacion) {
                    {{ p.titulares }} titular{{ p.titulares === 1 ? '' : 'es' }} confirmado{{ p.titulares === 1 ? '' : 's' }}
                  } @else {
                    sin plantilla armada
                  }
                  @if (p.observacion) { · {{ p.observacion }} }
                </p>
              </div>

              <div class="partido__acciones">
                <a class="btn btn--ghost btn--sm" [routerLink]="['/partidos', p.idPartido, 'alineacion']">
                  {{ p.tieneAlineacion ? 'Ver plantilla' : 'Generar plantilla' }}
                </a>
                @if (editandoResultado() === p.idPartido) {
                  <div class="marcador">
                    <input class="gol" type="number" min="0" max="99" [(ngModel)]="golesFavor"
                           name="gf" aria-label="Goles a favor" />
                    <span class="guion">–</span>
                    <input class="gol" type="number" min="0" max="99" [(ngModel)]="golesContra"
                           name="gc" aria-label="Goles en contra" />
                    <button type="button" class="btn btn--primary btn--sm"
                            [disabled]="guardando()" (click)="guardarResultado(p)">Guardar</button>
                    <button type="button" class="btn btn--ghost btn--sm"
                            (click)="editandoResultado.set(null)">✕</button>
                  </div>
                } @else {
                  <button type="button" class="btn btn--ghost btn--sm" (click)="editarResultado(p)">
                    {{ p.resultado === 'PENDIENTE' ? 'Cargar resultado' : 'Corregir resultado' }}
                  </button>
                }
              </div>
            </li>
          }
        </ul>

        @if (totalPaginas() > 1) {
          <nav class="paginacion">
            <button type="button" class="btn btn--ghost btn--sm"
                    [disabled]="pagina() === 0" (click)="irA(pagina() - 1)">‹ Anterior</button>
            <span>Página {{ pagina() + 1 }} de {{ totalPaginas() }}</span>
            <button type="button" class="btn btn--ghost btn--sm"
                    [disabled]="pagina() + 1 >= totalPaginas()" (click)="irA(pagina() + 1)">Siguiente ›</button>
          </nav>
        }
      }
    </div>
  `,
  styles: [`
    .pantalla { max-width: 860px; margin: 0 auto; padding: 1.25rem 1rem 3rem; }
    .cabecera { display: flex; justify-content: space-between; align-items: flex-start;
                gap: 1rem; flex-wrap: wrap; margin-bottom: 1.1rem; }
    h1 { font-size: 1.2rem; }
    .subt { margin-top: .3rem; color: var(--color-text-muted); font-size: .85rem; max-width: 52ch; }
    .formulario { padding: 1.1rem 1.2rem; margin-bottom: 1.1rem; }
    .formulario h2 { font-size: 1rem; margin-bottom: .8rem; }
    .fila-form { display: flex; gap: .8rem; flex-wrap: wrap; }
    .fila-form .field { flex: 1 1 170px; }
    .opcional { color: var(--color-text-faint); font-weight: 400; }
    .filtro { margin-bottom: .9rem; }
    .field--inline { display: flex; align-items: center; gap: .5rem; max-width: 320px; }
    .field--inline .field__label { margin: 0; white-space: nowrap; }
    .vacio { text-align: center; padding: 2.2rem 1.5rem; }
    .vacio h2 { font-size: 1.05rem; margin: 0 0 .5rem; }
    .vacio p { color: var(--color-text-muted); font-size: .88rem; max-width: 46ch; margin: 0 auto; }
    .lista { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: .6rem; }
    .partido { display: flex; align-items: center; gap: 1rem; padding: .85rem 1rem;
               background: var(--color-surface); border: 1px solid var(--color-border-light);
               border-radius: var(--radius-md); flex-wrap: wrap; }
    .partido--jugado { border-left: 3px solid var(--color-border); }
    .partido__fecha { display: flex; flex-direction: column; align-items: center;
                      min-width: 44px; line-height: 1.1; }
    .dia { font-size: 1.35rem; font-weight: 700; font-variant-numeric: tabular-nums; }
    .mes { font-size: .68rem; text-transform: uppercase; letter-spacing: .06em;
           color: var(--color-text-muted); }
    .partido__cuerpo { flex: 1 1 200px; min-width: 0; }
    .partido__titulo { display: flex; align-items: center; gap: .55rem; flex-wrap: wrap; }
    .partido__meta { margin-top: .25rem; font-size: .8rem; color: var(--color-text-muted); }
    .partido__acciones { display: flex; align-items: center; gap: .4rem; flex-wrap: wrap; }
    .marcador { display: flex; align-items: center; gap: .3rem; }
    .gol { width: 3rem; padding: .3rem .35rem; text-align: center; font-variant-numeric: tabular-nums;
           border: 1px solid var(--color-border); border-radius: var(--radius-sm);
           background: var(--color-surface); color: var(--color-text); }
    .guion { color: var(--color-text-faint); }
    .paginacion { display: flex; align-items: center; justify-content: center; gap: .8rem;
                  margin-top: 1.1rem; font-size: .82rem; color: var(--color-text-muted); }
  `],
})
export class PartidosComponent implements OnInit {
  private readonly servicio = inject(PartidosService);
  private readonly sesiones = inject(SesionesService);

  readonly horaCorta = horaCorta;

  readonly partidos = signal<Partido[]>([]);
  readonly categorias = signal<CategoriaOpcion[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly guardando = signal(false);

  readonly pagina = signal(0);
  readonly totalPaginas = signal(1);
  readonly filtroCategoria = signal<number | null>(null);

  readonly mostrandoFormulario = signal(false);
  readonly errorFormulario = signal<string | null>(null);
  nuevaCategoria: number | null = null;
  nuevaFecha = '';
  nuevaHora = '';
  nuevaObservacion = '';

  readonly editandoResultado = signal<number | null>(null);
  golesFavor: number | null = null;
  golesContra: number | null = null;

  private readonly MESES = ['ENE', 'FEB', 'MAR', 'ABR', 'MAY', 'JUN',
                            'JUL', 'AGO', 'SEP', 'OCT', 'NOV', 'DIC'];

  ngOnInit(): void {
    this.sesiones.listarCategoriasActivas().subscribe({
      next: (c) => this.categorias.set(c),
      error: () => this.categorias.set([]),
    });
    this.cargar();
  }

  private cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.servicio.listar(this.filtroCategoria(), this.pagina()).subscribe({
      next: (page) => {
        this.partidos.set(page.contenido);
        this.totalPaginas.set(Math.max(page.totalPaginas, 1));
        this.cargando.set(false);
      },
      error: (e) => {
        this.cargando.set(false);
        this.error.set(mensajeDeError(e, 'No se pudieron cargar los partidos.'));
      },
    });
  }

  alternarFormulario(): void {
    this.mostrandoFormulario.set(!this.mostrandoFormulario());
    this.errorFormulario.set(null);
    if (this.mostrandoFormulario() && !this.nuevaFecha) {
      this.nuevaFecha = new Date().toISOString().slice(0, 10);
    }
  }

  crear(): void {
    if (this.guardando()) return;
    if (!this.nuevaCategoria || !this.nuevaFecha) {
      this.errorFormulario.set('Elegí la categoría y la fecha del partido.');
      return;
    }
    this.guardando.set(true);
    this.errorFormulario.set(null);
    this.servicio.crear({
      idCategoria: this.nuevaCategoria,
      fecha: this.nuevaFecha,
      hora: this.nuevaHora || null,
      observacion: this.nuevaObservacion.trim() || null,
    }).subscribe({
      next: () => {
        this.guardando.set(false);
        this.mostrandoFormulario.set(false);
        this.nuevaObservacion = '';
        this.nuevaHora = '';
        this.pagina.set(0);
        this.cargar();
      },
      error: (e) => {
        this.guardando.set(false);
        this.errorFormulario.set(mensajeDeError(e, 'No se pudo agendar el partido.'));
      },
    });
  }

  editarResultado(p: Partido): void {
    this.editandoResultado.set(p.idPartido);
    this.golesFavor = p.golesFavor;
    this.golesContra = p.golesContra;
  }

  guardarResultado(p: Partido): void {
    if (this.guardando()) return;

    if (this.golesFavor == null || this.golesContra == null) {
      this.error.set('Cargá los dos marcadores, el propio y el del rival.');
      return;
    }
    this.guardando.set(true);
    this.error.set(null);
    this.servicio.registrarResultado(p.idPartido, {
      golesFavor: this.golesFavor,
      golesContra: this.golesContra,
      observacion: null,
    }).subscribe({
      next: (actualizado) => {
        this.partidos.set(this.partidos().map(
          (x) => (x.idPartido === actualizado.idPartido ? actualizado : x)));
        this.editandoResultado.set(null);
        this.guardando.set(false);
      },
      error: (e) => {
        this.guardando.set(false);
        this.error.set(mensajeDeError(e, 'No se pudo guardar el resultado.'));
      },
    });
  }

  filtrar(idCategoria: number | null): void {
    this.filtroCategoria.set(idCategoria);
    this.pagina.set(0);
    this.cargar();
  }

  irA(pagina: number): void {
    this.pagina.set(Math.max(pagina, 0));
    this.cargar();
  }

  diaDe(fecha: string): string {
    return fecha.slice(8, 10);
  }

  mesDe(fecha: string): string {
    return this.MESES[Number(fecha.slice(5, 7)) - 1] ?? '';
  }

  etiquetaDe(p: Partido): string {
    if (p.resultado === 'PENDIENTE') return 'Por jugar';
    return `${p.golesFavor} – ${p.golesContra}`;
  }

  colorDe(resultado: Partido['resultado']): string {
    if (resultado === 'GANADO') return 'success';
    if (resultado === 'PERDIDO') return 'danger';
    if (resultado === 'EMPATADO') return 'warning';
    return 'info';
  }
}
