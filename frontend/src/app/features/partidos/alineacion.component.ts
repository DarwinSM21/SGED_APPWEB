import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { CargandoComponent } from '../../core/cargando.component';
import { mensajeDeError } from '../../core/mensaje-error';
import { apellidoDe, inicialesDe } from '../../core/formato-texto';
import {
  COORDENADA_POR_ABREVIATURA, ZonaCancha, zonaDe,
} from './cancha';
import { PartidosService } from './partidos.service';
import {
  Alineacion, FeedbackAlineacion, JugadorConvocado, JugadorEnCancha, NoConvocable, Posicion,
} from './partidos.models';

const COLOR_ZONA: Record<ZonaCancha, string> = {
  POR: '#f59e0b',
  DEF: '#3b82f6',
  MED: '#8b5cf6',
  DEL: '#ef4444',
  SIN_POSICION: '#94a3b8',
};

const ETIQUETA_ZONA: Record<ZonaCancha, string> = {
  POR: 'Portero', DEF: 'Defensa', MED: 'Mediocampo', DEL: 'Delantero',
  SIN_POSICION: 'Sin posición registrada',
};

/** Un hueco del campo. Puede estar ocupado o vacío: eso es lo que permite el cambio. */
interface Puesto {
  idPosicion: number;
  abreviatura: string;
  nombre: string;
  zona: ZonaCancha;
  x: number;
  y: number;
  jugador: JugadorConvocado | null;
}

/**
 * Lo que el entrenador tiene tomado en la mano. Un cambio necesita SIEMPRE
 * dos extremos -quién sale y quién entra-, así que la pantalla obliga a
 * elegir los dos en vez de decidir uno por su cuenta.
 */
type Seleccion =
  | { tipo: 'jugador'; idEstudiante: number; origen: 'cancha' | 'banco' }
  | { tipo: 'puesto'; idPosicion: number }
  | null;

/**
 * El once de un partido, sobre la cancha.
 *
 * <p>La sugerencia la calcula el backend con una regla determinista sobre el
 * rendimiento de las últimas semanas; esta pantalla la dibuja y deja que el
 * entrenador la cambie.
 *
 * <p>El campo son <b>once huecos fijos</b>, uno por posición del catálogo, y
 * no una lista de jugadores. Eso es lo que arregla los dos defectos que tenía
 * el cambio: antes el sistema elegía por vos a quién sacaba -emparejaba por
 * posición nominal-, y si el suplente jugaba en un puesto que nadie ocupaba
 * no sustituía a nadie, lo agregaba, y terminabas con doce en la cancha.
 * Con huecos, sacar y meter son la misma operación sobre el mismo hueco, y
 * pasar de once es imposible porque no hay un doceavo lugar donde ponerlo.
 */
@Component({
  selector: 'app-alineacion',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, CargandoComponent],
  template: `
    <div class="pantalla">
      @if (cargando()) {
        <app-cargando mensaje="Armando la convocatoria…" />
      } @else if (error() && !alineacion()) {
        <p class="alert alert--danger">{{ error() }}</p>
      } @else if (alineacion(); as a) {

        <a class="btn btn--ghost volver" routerLink="/partidos">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="19" y1="12" x2="5" y2="12"></line><polyline points="12 19 5 12 12 5"></polyline></svg>
          Volver a partidos
        </a>

        <header class="cabecera">
          <div>
            <h1>Plantilla del partido</h1>
            <p class="subt">
              {{ a.categoria }} · {{ a.fecha }} ·
              @if (a.guardada) {
                <strong>la formación que armaste</strong>
              } @else {
                sugerencia del sistema
              }
            </p>
            <p class="ventana">
              Calculada con {{ a.ventana.entrenamientos }}
              entrenamiento{{ a.ventana.entrenamientos === 1 ? '' : 's' }} de las últimas
              {{ a.ventana.semanas }} semanas ({{ a.ventana.desde }} a {{ a.ventana.hasta }}):
              promedio de evaluación primero, asistencia para desempatar.
            </p>
          </div>
          <button type="button" class="btn btn--primary"
                  (click)="pedirFeedback()" [disabled]="cargandoFeedback() || enCancha().length === 0">
            @if (cargandoFeedback()) { <span class="spinner"></span> Pensando… }
            @else { ✦ Comentario con IA }
          </button>
        </header>

        @if (feedback(); as f) {
          <div class="alert" [class.alert--info]="f.disponible" [class.alert--warning]="!f.disponible">
            {{ f.disponible ? f.comentario : ('IA no disponible: ' + f.motivo) }}
          </div>
        }

        @if (error()) { <p class="alert alert--danger">{{ error() }}</p> }
        @if (aviso()) { <p class="alert alert--warning">{{ aviso() }}</p> }

        @if (enCancha().length === 0 && banco().length === 0) {
          <div class="card vacio">
            <h2>No hay a quién convocar</h2>
            <p>
              @if (a.noConvocables.length > 0) {
                Los {{ a.noConvocables.length }} jugadores de {{ a.categoria }} están fuera:
                mirá los motivos más abajo.
              } @else {
                No hay estudiantes activos en {{ a.categoria }}.
              }
            </p>
          </div>
        }

        <div class="cuerpo">
          <div class="campo-envoltura">
            <svg class="campo" viewBox="0 0 400 560" preserveAspectRatio="xMidYMid meet">
              <defs>
                <linearGradient id="pasto" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#22a352" />
                  <stop offset="100%" stop-color="#146c37" />
                </linearGradient>
              </defs>
              <rect x="0" y="0" width="400" height="560" rx="12" fill="url(#pasto)" />
              <rect x="4" y="4" width="392" height="552" rx="8" fill="none" stroke="#ffffff66" stroke-width="2" />
              <line x1="4" y1="280" x2="396" y2="280" stroke="#ffffff66" stroke-width="2" />
              <circle cx="200" cy="280" r="48" fill="none" stroke="#ffffff66" stroke-width="2" />
              <circle cx="200" cy="280" r="3" fill="#ffffff99" />
              <rect x="100" y="0" width="200" height="60" fill="none" stroke="#ffffff66" stroke-width="2" />
              <rect x="160" y="0" width="80" height="24" fill="none" stroke="#ffffff66" stroke-width="2" />
              <rect x="100" y="500" width="200" height="60" fill="none" stroke="#ffffff66" stroke-width="2" />
              <rect x="160" y="536" width="80" height="24" fill="none" stroke="#ffffff66" stroke-width="2" />

              @for (p of puestos(); track p.idPosicion) {
                <g class="puesto"
                   [class.puesto--vacio]="!p.jugador"
                   [class.puesto--elegido]="estaElegido(p)"
                   [attr.aria-label]="rotuloPuesto(p)"
                   role="button" tabindex="0"
                   (click)="tocarPuesto(p)"
                   (keydown.enter)="tocarPuesto(p)"
                   (keydown.space)="tocarPuesto(p)">
                  @if (p.jugador) {
                    <circle [attr.cx]="p.x" [attr.cy]="p.y" r="22" fill="#ffffff"
                            [attr.stroke]="COLOR_ZONA[p.zona]" stroke-width="4" />
                    <text [attr.x]="p.x" [attr.y]="p.y + 5" text-anchor="middle"
                          font-size="13" font-weight="700" fill="#1f2937">
                      {{ iniciales(p.jugador.nombreCompleto) }}
                    </text>
                    <text [attr.x]="p.x" [attr.y]="p.y + 38" text-anchor="middle"
                          font-size="12" fill="#ffffff" font-weight="600">
                      {{ apellido(p.jugador.nombreCompleto) }}
                    </text>
                  } @else {
                    <circle [attr.cx]="p.x" [attr.cy]="p.y" r="22" fill="#ffffff22"
                            stroke="#ffffffaa" stroke-width="2" stroke-dasharray="4 4" />
                    <text [attr.x]="p.x" [attr.y]="p.y + 5" text-anchor="middle"
                          font-size="11" font-weight="700" fill="#ffffffcc">
                      {{ p.abreviatura }}
                    </text>
                  }
                </g>
              }
            </svg>

            <div class="leyenda">
              @for (zona of zonasLeyenda; track zona) {
                <span class="leyenda-item">
                  <span class="punto" [style.background]="COLOR_ZONA[zona]"></span>
                  {{ ETIQUETA_ZONA[zona] }}
                </span>
              }
            </div>

            <p class="instruccion">
              @if (seleccion(); as s) {
                @if (s.tipo === 'jugador') {
                  <strong>{{ apellido(nombreDe(s.idEstudiante)) }}</strong> está elegido.
                  Tocá un hueco libre para moverlo, otro jugador para intercambiarlos,
                  o alguien del banco para hacer el cambio.
                } @else {
                  Hueco <strong>{{ abreviaturaDe(s.idPosicion) }}</strong> elegido.
                  Tocá a quien lo va a ocupar.
                }
                <button type="button" class="btn btn--ghost btn--sm" (click)="limpiarSeleccion()">Cancelar</button>
              } @else {
                Tocá a un jugador de la cancha o un hueco libre para empezar un cambio.
              }
            </p>
          </div>

          <div class="panel-lateral">
            @if (detalle(); as d) {
              <aside class="card detalle-panel">
                <h2>{{ d.nombreCompleto }}</h2>
                <p class="posicion-detalle">
                  {{ d.posicion ? etiquetaCompleta(d.posicion) : 'Sin posición registrada' }}
                </p>
                <div class="numeros">
                  <div class="numero">
                    <span class="valor">{{ d.promedio ?? '—' }}</span>
                    <span class="unidad">promedio {{ ventanaCorta() }}</span>
                  </div>
                  <div class="numero">
                    <span class="valor">{{ d.presencias }}<span class="de">/{{ d.entrenamientos }}</span></span>
                    <span class="unidad">entrenamientos</span>
                  </div>
                </div>
                @if (d.promedio === null) {
                  <p class="sin-nota">Todavía no lo evaluaron en esta ventana.</p>
                }
                @if (estaEnCancha(d.idEstudiante)) {
                  <button type="button" class="btn btn--ghost btn--sm btn--block"
                          (click)="sacarAlBanco(d.idEstudiante)">
                    Sacar al banco
                  </button>
                }
              </aside>
            }

            <section class="card banco">
              <h2>Banco <span class="cuenta">{{ banco().length }}</span></h2>
              @if (banco().length === 0) {
                <p class="ayuda">Todos los convocados están en la cancha.</p>
              } @else {
                <p class="ayuda">
                  Elegí primero a quién sacás (o un hueco libre) y después tocá a quien entra.
                </p>

                @if (banco().length > UMBRAL_BUSCADOR) {
                  <input class="buscar-banco" type="search" [ngModel]="filtroBanco()"
                         (ngModelChange)="filtroBanco.set($event)"
                         [attr.placeholder]="'Buscar entre ' + banco().length + ' jugadores…'"
                         aria-label="Buscar en el banco" />
                }

                @if (bancoVisible().length === 0) {
                  <p class="ayuda">Nadie coincide con «{{ filtroBanco() }}».</p>
                }

                <div class="banco-lista">
                @for (s of bancoVisible(); track s.idEstudiante) {
                  <button type="button" class="suplente"
                          [class.suplente--elegido]="esElegido(s.idEstudiante)"
                          (click)="tocarBanco(s)">
                    <span class="avatar avatar--muted">{{ iniciales(s.nombreCompleto) }}</span>
                    <span class="suplente-nombre">
                      {{ s.nombreCompleto }}
                      @if (s.posicion) { <span class="puesto-banco">{{ s.posicion }}</span> }
                    </span>
                    <span class="suplente-num">
                      {{ s.promedio ?? '—' }} · {{ s.presencias }}/{{ s.entrenamientos }}
                    </span>
                  </button>
                }
                </div>

                @if (bancoVisible().length < banco().length) {
                  <p class="ayuda recorte">
                    Se muestran {{ bancoVisible().length }} de {{ banco().length }}.
                    @if (!filtroBanco()) { Buscá por nombre para encontrar al resto. }
                  </p>
                }
              }
            </section>

            @if (a.noConvocables.length > 0) {
              <section class="card fuera">
                <h2>No pueden jugar</h2>
                @for (n of a.noConvocables; track n.idEstudiante) {
                  <div class="fuera-fila">
                    <span class="fuera-nombre">{{ n.nombreCompleto }}</span>
                    <span class="badge badge--warning">{{ n.motivo }}</span>
                  </div>
                }
              </section>
            }

            <section class="card decision">
              <h2>Cómo funcionó</h2>
              <div class="estrellas" role="group" aria-label="Valoración de la formación">
                @for (v of estrellas; track v) {
                  <button type="button" class="estrella"
                          [class.estrella--activa]="(valoracion() ?? 0) >= v"
                          [attr.aria-pressed]="valoracion() === v"
                          [attr.aria-label]="v + ' de 5'"
                          (click)="calificar(v)">★</button>
                }
                @if (valoracion()) { <span class="val-num">{{ valoracion() }}/5</span> }
              </div>

              <input class="obs" type="text" maxlength="500"
                     [ngModel]="observacion()"
                     (ngModelChange)="observacion.set($event); sinGuardar.set(true)"
                     placeholder="Por qué este once (opcional)" />

              @if (mensaje(); as m) { <p class="ok">{{ m }}</p> }

              <div class="acciones">
                <button type="button" class="btn btn--primary btn--block"
                        [disabled]="guardando()" (click)="guardar()">
                  {{ guardando() ? 'Guardando…' : (sinGuardar() ? 'Guardar plantilla' : 'Guardar de nuevo') }}
                </button>
                @if (a.guardada) {
                  <button type="button" class="btn btn--ghost btn--block"
                          [disabled]="guardando()" (click)="restablecer()">
                    Volver a la sugerencia del sistema
                  </button>
                }
              </div>
            </section>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .pantalla { max-width: 920px; margin: 0 auto; padding: 1.25rem 1rem 3rem; }
    .volver { margin-bottom: 1rem; }

    .cabecera { display: flex; justify-content: space-between; align-items: flex-start;
                gap: 1rem; flex-wrap: wrap; margin-bottom: .9rem; }
    h1 { font-size: 1.15rem; }
    .subt { margin-top: .3rem; color: var(--color-text-muted); font-size: .85rem; }
    .ventana { margin-top: .35rem; font-size: .78rem; color: var(--color-text-faint);
               max-width: 56ch; line-height: 1.45; }

    .alert--info { background: var(--color-primary-50); color: var(--color-primary-700); }

    .vacio { text-align: center; padding: 2rem 1.5rem; margin-bottom: 1rem; }
    .vacio h2 { font-size: 1.05rem; margin: 0 0 .5rem; }
    .vacio p { color: var(--color-text-muted); font-size: .88rem; max-width: 46ch; margin: 0 auto; }

    .cuerpo { display: flex; gap: 1.1rem; flex-wrap: wrap; margin-top: 1rem; align-items: flex-start; }
    .campo-envoltura { flex: 1 1 320px; min-width: 280px; }
    .campo { width: 100%; height: auto; display: block; box-shadow: var(--shadow-md);
             border-radius: 12px; touch-action: manipulation; }

    .puesto { cursor: pointer; }
    .puesto circle { transition: stroke-width .15s, opacity .15s; }
    .puesto:hover circle { stroke-width: 5; }
    .puesto--vacio:hover circle { opacity: .85; }
    .puesto--elegido circle { stroke-width: 6; filter: drop-shadow(0 3px 5px rgb(0 0 0 / .4)); }
    .puesto:focus-visible { outline: none; }
    .puesto:focus-visible circle { stroke: #ffffff; stroke-width: 6; }

    .leyenda { display: flex; flex-wrap: wrap; gap: .8rem; margin-top: .75rem;
               font-size: .78rem; color: var(--color-text-muted); }
    .leyenda-item { display: inline-flex; align-items: center; gap: .35rem; }
    .punto { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }

    .instruccion { margin-top: .7rem; font-size: .8rem; color: var(--color-text-muted);
                   line-height: 1.5; display: flex; align-items: center; gap: .5rem;
                   flex-wrap: wrap; }

    .panel-lateral { flex: 1 1 250px; min-width: 230px; display: flex;
                     flex-direction: column; gap: 1.1rem; }

    .detalle-panel { padding: 1.15rem 1.2rem; }
    .detalle-panel h2 { font-size: 1.02rem; margin-bottom: .25rem; }
    .posicion-detalle { color: var(--color-text-muted); font-size: .82rem; margin-bottom: .9rem; }
    .numeros { display: flex; gap: 1.4rem; margin-bottom: .6rem; }
    .numero { display: flex; flex-direction: column; }
    .numero .valor { font-size: 1.55rem; font-weight: 700; color: var(--color-primary-600);
                     font-variant-numeric: tabular-nums; line-height: 1.1; }
    .numero .de { font-size: .9rem; color: var(--color-text-faint); font-weight: 500; }
    .numero .unidad { font-size: .72rem; color: var(--color-text-muted); }
    .sin-nota { font-size: .76rem; color: var(--color-text-faint); margin: 0 0 .7rem; }

    .banco { padding: 1.1rem 1.2rem; }
    .banco h2, .fuera h2, .decision h2 { font-size: 1rem; margin-bottom: .6rem; }
    .cuenta { font-size: .78rem; color: var(--color-text-faint); font-weight: 500; }
    .ayuda { font-size: .76rem; color: var(--color-text-muted); margin: 0 0 .6rem; line-height: 1.45; }
    .recorte { margin: .5rem 0 0; }

    .buscar-banco { width: 100%; padding: .4rem .6rem; font-size: .82rem; margin-bottom: .55rem;
                    border: 1px solid var(--color-border); border-radius: var(--radius-sm);
                    background: var(--color-surface); color: var(--color-text); }

    /* Con el plantel entero en el banco -que es lo normal en una categoría
       grande- la tarjeta medía 36.000 px y empujaba la cancha fuera de la
       vista: para meter un suplente había que dejar de ver el campo. Ahora
       la lista scrollea dentro de sí misma. */
    .banco-lista { max-height: 22rem; overflow-y: auto; overscroll-behavior: contain;
                   margin: 0 -.2rem; padding: 0 .2rem; }
    .suplente { display: flex; width: 100%; align-items: center; gap: .65rem; padding: .55rem .7rem;
                border: 1px solid var(--color-border-light); border-radius: var(--radius-sm);
                margin-bottom: .4rem; font-size: .88rem; background: var(--color-surface);
                color: var(--color-text); text-align: left; cursor: pointer;
                transition: border-color .12s, background .12s; }
    .suplente:hover { border-color: var(--color-primary-600); }
    .suplente--elegido { border-color: var(--color-primary-600);
                         background: var(--color-primary-50); }
    .suplente-nombre { flex: 1; min-width: 0; }
    .puesto-banco { font-size: .7rem; color: var(--color-text-faint); margin-left: .35rem;
                    font-family: ui-monospace, monospace; }
    .suplente-num { font-size: .72rem; color: var(--color-text-muted);
                    font-variant-numeric: tabular-nums; white-space: nowrap; }

    .fuera { padding: 1.1rem 1.2rem; }
    .fuera-fila { display: flex; align-items: center; justify-content: space-between;
                  gap: .6rem; padding: .35rem 0; font-size: .84rem; }
    .fuera-nombre { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

    .decision { padding: 1.1rem 1.2rem; }
    .estrellas { display: flex; align-items: center; gap: .15rem; margin-bottom: .7rem; }
    .estrella { background: none; border: none; cursor: pointer; padding: .1rem .15rem;
                font-size: 1.35rem; line-height: 1; color: var(--color-border);
                transition: color .12s, transform .12s; }
    .estrella:hover { transform: scale(1.12); }
    .estrella--activa { color: #f59e0b; }
    .estrella:focus-visible { outline: 2px solid var(--color-primary-600); outline-offset: 2px; }
    .val-num { font-size: .78rem; color: var(--color-text-muted); margin-left: .4rem;
               font-variant-numeric: tabular-nums; }
    .obs { width: 100%; padding: .45rem .6rem; font-size: .82rem; margin-bottom: .7rem;
           border: 1px solid var(--color-border); border-radius: var(--radius-sm);
           background: var(--color-surface); color: var(--color-text); }
    .acciones { display: flex; flex-direction: column; gap: .4rem; }
    .ok { font-size: .8rem; color: var(--color-success-text); margin: 0 0 .5rem; }
  `],
})
export class AlineacionComponent implements OnInit {

  private readonly ruta = inject(ActivatedRoute);
  private readonly servicio = inject(PartidosService);

  readonly COLOR_ZONA = COLOR_ZONA;
  readonly ETIQUETA_ZONA = ETIQUETA_ZONA;
  readonly zonasLeyenda: ZonaCancha[] = ['POR', 'DEF', 'MED', 'DEL'];
  readonly estrellas = [1, 2, 3, 4, 5];

  readonly alineacion = signal<Alineacion | null>(null);
  readonly catalogo = signal<Posicion[]>([]);
  /** El once, por hueco. La clave es idPosicion; sin clave no hay hueco. */
  readonly enCanchaPorPuesto = signal<Map<number, JugadorConvocado>>(new Map());
  readonly banco = signal<JugadorConvocado[]>([]);
  readonly seleccion = signal<Seleccion>(null);
  readonly filtroBanco = signal('');

  /** A partir de aquí buscar es más rápido que recorrer con el dedo. */
  readonly UMBRAL_BUSCADOR = 12;
  /** Cuántos se dibujan de una. El resto sale por búsqueda. */
  private readonly TOPE_BANCO = 60;

  readonly valoracion = signal<number | null>(null);
  readonly observacion = signal('');
  readonly sinGuardar = signal(false);
  readonly guardando = signal(false);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly aviso = signal<string | null>(null);
  readonly mensaje = signal<string | null>(null);
  readonly cargandoFeedback = signal(false);
  readonly feedback = signal<FeedbackAlineacion | null>(null);

  private idPartido = 0;

  /** Los once huecos del campo, ocupados o no. */
  readonly puestos = computed<Puesto[]>(() => {
    const ocupados = this.enCanchaPorPuesto();
    return this.catalogo().map((p) => {
      const coord = COORDENADA_POR_ABREVIATURA[p.abreviatura] ?? { x: 200, y: 280 };
      return {
        idPosicion: p.idPosicion,
        abreviatura: p.abreviatura,
        nombre: p.nombre,
        zona: zonaDe(p.abreviatura),
        x: coord.x,
        y: coord.y,
        jugador: ocupados.get(p.idPosicion) ?? null,
      };
    });
  });

  readonly enCancha = computed(() => [...this.enCanchaPorPuesto().values()]);

  /**
   * El banco que se dibuja. Se filtra por nombre y se corta en TOPE_BANCO
   * porque en una categoría grande el banco ES el plantel entero: pintar 574
   * botones daba una tarjeta de 36.000 px que empujaba la cancha fuera de la
   * pantalla. El corte no esconde a nadie —el buscador llega a todos— y la
   * pantalla dice cuántos quedan sin mostrar.
   */
  readonly bancoVisible = computed<JugadorConvocado[]>(() => {
    const texto = this.filtroBanco().trim().toLowerCase();
    const lista = texto
      ? this.banco().filter((j) => j.nombreCompleto.toLowerCase().includes(texto)
          || (j.posicion ?? '').toLowerCase().includes(texto))
      : this.banco();
    return lista.slice(0, this.TOPE_BANCO);
  });

  /** El jugador cuya ficha se muestra al costado. */
  readonly detalle = computed<JugadorConvocado | null>(() => {
    const s = this.seleccion();
    if (!s || s.tipo !== 'jugador') return null;
    return this.buscar(s.idEstudiante);
  });

  readonly ventanaCorta = computed(() => {
    const a = this.alineacion();
    return a ? `de ${a.ventana.semanas} semanas` : '';
  });

  ngOnInit(): void {
    this.idPartido = Number(this.ruta.snapshot.paramMap.get('idPartido'));
    this.cargar();
  }

  private cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    forkJoin({
      alineacion: this.servicio.alineacion(this.idPartido),
      posiciones: this.servicio.posiciones(),
    }).subscribe({
      next: ({ alineacion, posiciones }) => {
        this.catalogo.set(posiciones);
        this.recibir(alineacion);
        this.cargando.set(false);
      },
      error: (e) => {
        this.cargando.set(false);
        this.error.set(e.status === 404
          ? 'Ese partido no existe.'
          : mensajeDeError(e, 'No se pudo armar la convocatoria.'));
      },
    });
  }

  private recibir(a: Alineacion): void {
    this.alineacion.set(a);

    // El banco es todo el que está convocado y no arranca. `disponibles` llega
    // vacío mientras nadie guardó nada -el cálculo reparte a todo el plantel-,
    // pero deja de estarlo cuando el entrenador guardó una lista más corta.
    const sinPuesto: JugadorConvocado[] = [];
    const porPuesto = new Map<number, JugadorConvocado>();
    for (const t of a.titulares) {
      if (t.idPosicion != null && !porPuesto.has(t.idPosicion)) {
        porPuesto.set(t.idPosicion, t);
      } else {
        // Un titular sin puesto no se puede dibujar en la cancha, así que va al
        // banco en vez de desaparecer sin que nadie se entere.
        sinPuesto.push(t);
      }
    }

    this.enCanchaPorPuesto.set(porPuesto);
    this.banco.set([...sinPuesto, ...a.suplentes, ...a.disponibles]);
    this.valoracion.set(a.valoracion);
    this.observacion.set(a.observacion ?? '');
    this.seleccion.set(null);
    this.filtroBanco.set('');
    this.sinGuardar.set(false);
    this.aviso.set(sinPuesto.length > 0
      ? `${sinPuesto.length} jugador${sinPuesto.length === 1 ? '' : 'es'} sin puesto asignado quedó en el banco: `
        + 'asignale un hueco en la cancha o dejalo de suplente.'
      : null);
  }

  // ---------------------------------------------------------------- cambios

  /**
   * Toque sobre un hueco de la cancha. Es la mitad de un cambio: la otra
   * mitad es el toque anterior o el siguiente.
   */
  tocarPuesto(p: Puesto): void {
    const s = this.seleccion();
    this.mensaje.set(null);

    if (s?.tipo === 'jugador') {
      const jugador = this.buscar(s.idEstudiante);
      if (!jugador) { this.seleccion.set(null); return; }

      // Tocarse a sí mismo cancela, en vez de dejar la selección pegada.
      if (p.jugador?.idEstudiante === s.idEstudiante) { this.seleccion.set(null); return; }

      if (s.origen === 'banco') {
        this.meterDesdeBanco(jugador, p);
      } else {
        this.moverEnCancha(jugador, p);
      }
      this.seleccion.set(null);
      this.sinGuardar.set(true);
      return;
    }

    if (s?.tipo === 'puesto') {
      // Dos huecos vacíos seguidos: se queda con el último elegido.
      this.seleccion.set(p.jugador
        ? { tipo: 'jugador', idEstudiante: p.jugador.idEstudiante, origen: 'cancha' }
        : { tipo: 'puesto', idPosicion: p.idPosicion });
      return;
    }

    this.seleccion.set(p.jugador
      ? { tipo: 'jugador', idEstudiante: p.jugador.idEstudiante, origen: 'cancha' }
      : { tipo: 'puesto', idPosicion: p.idPosicion });
  }

  /** Toque sobre alguien del banco. */
  tocarBanco(jugador: JugadorConvocado): void {
    const s = this.seleccion();
    this.mensaje.set(null);

    if (s?.tipo === 'jugador' && s.origen === 'cancha') {
      const puesto = this.puestoDe(s.idEstudiante);
      if (puesto) {
        this.meterDesdeBanco(jugador, puesto);
        this.seleccion.set(null);
        this.sinGuardar.set(true);
        return;
      }
    }

    if (s?.tipo === 'puesto') {
      const puesto = this.puestos().find((p) => p.idPosicion === s.idPosicion);
      if (puesto) {
        this.meterDesdeBanco(jugador, puesto);
        this.seleccion.set(null);
        this.sinGuardar.set(true);
        return;
      }
    }

    // Sin nada elegido, tocar al suplente lo deja elegido a él: el próximo
    // toque en la cancha decide dónde entra y a quién saca.
    this.seleccion.set(s?.tipo === 'jugador' && s.idEstudiante === jugador.idEstudiante
      ? null
      : { tipo: 'jugador', idEstudiante: jugador.idEstudiante, origen: 'banco' });
  }

  /** Mete a alguien del banco en un hueco; si está ocupado, el que estaba sale. */
  private meterDesdeBanco(entra: JugadorConvocado, destino: Puesto): void {
    const puestos = new Map(this.enCanchaPorPuesto());
    const sale = puestos.get(destino.idPosicion) ?? null;

    puestos.set(destino.idPosicion, { ...entra, idPosicion: destino.idPosicion,
      posicion: destino.abreviatura, titular: true });

    const banco = this.banco().filter((b) => b.idEstudiante !== entra.idEstudiante);
    if (sale) banco.unshift({ ...sale, titular: false });

    this.enCanchaPorPuesto.set(puestos);
    this.banco.set(banco);
  }

  /** Mueve a un titular a otro hueco; si está ocupado, los dos se intercambian. */
  private moverEnCancha(jugador: JugadorConvocado, destino: Puesto): void {
    const origen = this.puestoDe(jugador.idEstudiante);
    if (!origen) return;

    const puestos = new Map(this.enCanchaPorPuesto());
    const otro = puestos.get(destino.idPosicion) ?? null;

    puestos.set(destino.idPosicion, { ...jugador, idPosicion: destino.idPosicion,
      posicion: destino.abreviatura, titular: true });

    if (otro) {
      puestos.set(origen.idPosicion, { ...otro, idPosicion: origen.idPosicion,
        posicion: origen.abreviatura, titular: true });
    } else {
      puestos.delete(origen.idPosicion);
    }
    this.enCanchaPorPuesto.set(puestos);
  }

  sacarAlBanco(idEstudiante: number): void {
    const puesto = this.puestoDe(idEstudiante);
    if (!puesto || !puesto.jugador) return;

    const puestos = new Map(this.enCanchaPorPuesto());
    puestos.delete(puesto.idPosicion);
    this.enCanchaPorPuesto.set(puestos);
    this.banco.set([{ ...puesto.jugador, titular: false }, ...this.banco()]);
    this.seleccion.set(null);
    this.sinGuardar.set(true);
    this.mensaje.set(null);
  }

  limpiarSeleccion(): void {
    this.seleccion.set(null);
  }

  // --------------------------------------------------------------- guardado

  guardar(): void {
    if (this.guardando()) return;

    const jugadores: JugadorEnCancha[] = [
      ...this.enCancha().map((t) => ({
        idEstudiante: t.idEstudiante, idPosicion: t.idPosicion, titular: true })),
      ...this.banco().map((b) => ({
        idEstudiante: b.idEstudiante, idPosicion: b.idPosicion, titular: false })),
    ];
    if (jugadores.length === 0) {
      this.error.set('No hay a quién convocar todavía.');
      return;
    }

    this.guardando.set(true);
    this.error.set(null);
    this.servicio.guardarAlineacion(this.idPartido, jugadores,
                                    this.valoracion(), this.observacion().trim() || null)
      .subscribe({
        next: (a) => {
          this.recibir(a);
          this.guardando.set(false);
          this.mensaje.set('Plantilla guardada');
        },
        error: (e) => {
          this.guardando.set(false);
          this.error.set(mensajeDeError(e, 'No se pudo guardar la plantilla.'));
        },
      });
  }

  restablecer(): void {
    if (this.guardando()) return;
    this.guardando.set(true);
    this.servicio.restablecerAlineacion(this.idPartido).subscribe({
      next: (a) => {
        this.recibir(a);
        this.guardando.set(false);
        this.mensaje.set('Se volvió a la sugerencia del sistema');
      },
      error: (e) => {
        this.guardando.set(false);
        this.error.set(mensajeDeError(e, 'No se pudo restablecer.'));
      },
    });
  }

  calificar(valor: number): void {
    this.valoracion.set(this.valoracion() === valor ? null : valor);
    this.sinGuardar.set(true);
  }

  pedirFeedback(): void {
    this.cargandoFeedback.set(true);
    this.servicio.feedback(this.idPartido).subscribe({
      next: (f) => { this.feedback.set(f); this.cargandoFeedback.set(false); },
      error: () => {
        this.feedback.set({ comentario: null, disponible: false,
          motivo: 'No se pudo contactar al servicio' });
        this.cargandoFeedback.set(false);
      },
    });
  }

  // ----------------------------------------------------------------- apoyos

  private buscar(idEstudiante: number): JugadorConvocado | null {
    return this.enCancha().find((t) => t.idEstudiante === idEstudiante)
      ?? this.banco().find((b) => b.idEstudiante === idEstudiante)
      ?? null;
  }

  private puestoDe(idEstudiante: number): Puesto | null {
    return this.puestos().find((p) => p.jugador?.idEstudiante === idEstudiante) ?? null;
  }

  estaEnCancha(idEstudiante: number): boolean {
    return this.puestoDe(idEstudiante) !== null;
  }

  estaElegido(p: Puesto): boolean {
    const s = this.seleccion();
    if (!s) return false;
    if (s.tipo === 'puesto') return s.idPosicion === p.idPosicion;
    return p.jugador?.idEstudiante === s.idEstudiante;
  }

  esElegido(idEstudiante: number): boolean {
    const s = this.seleccion();
    return s?.tipo === 'jugador' && s.idEstudiante === idEstudiante;
  }

  nombreDe(idEstudiante: number): string {
    return this.buscar(idEstudiante)?.nombreCompleto ?? '';
  }

  abreviaturaDe(idPosicion: number): string {
    return this.catalogo().find((p) => p.idPosicion === idPosicion)?.abreviatura ?? '';
  }

  rotuloPuesto(p: Puesto): string {
    return p.jugador ? `${p.nombre}: ${p.jugador.nombreCompleto}` : `${p.nombre}: libre`;
  }

  iniciales(nombre: string): string {
    return inicialesDe(nombre);
  }

  apellido(nombre: string): string {
    return apellidoDe(nombre);
  }

  etiquetaCompleta(abreviatura: string): string {
    return `${abreviatura} · ${ETIQUETA_ZONA[zonaDe(abreviatura)]}`;
  }
}
