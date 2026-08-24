import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { mensajeDeError } from '../../core/mensaje-error';
import { CargandoComponent } from '../../core/cargando.component';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PlantillaService } from './plantilla.service';
import {
  Plantilla, JugadorPlantilla, ZonaCancha, zonaDe, inicialesDe, apellidoDe, COORDENADA_POR_ABREVIATURA, Alineacion, JugadorEnCancha,
} from './plantilla.models';

interface Token {
  jugador: JugadorPlantilla;
  zona: ZonaCancha;
  x: number;
  y: number;
}

const COLOR_ZONA: Record<ZonaCancha, string> = {
  POR: '#f59e0b',
  DEF: '#3b82f6',
  MED: '#8b5cf6',
  DEL: '#ef4444',
  SIN_POSICION: '#94a3b8',
};

const ETIQUETA_ZONA: Record<ZonaCancha, string> = {
  POR: 'Portero', DEF: 'Defensa', MED: 'Mediocampo', DEL: 'Delantero', SIN_POSICION: 'Sin posición registrada',
};

/**
 * Formacion sugerida, visualizada sobre una cancha.
 *
 * La seleccion y el orden de la alineacion los calculo el backend con una
 * regla deterministica (promedio acumulado, lesionados excluidos); esta
 * pantalla solo la dibuja. El comentario de IA es una accion aparte que el
 * entrenador dispara con el boton "Feedback IA": no se pide sola al abrir
 * la pantalla.
 */
@Component({
  selector: 'app-plantilla',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, CargandoComponent],
  template: `
    <div class="pantalla">
      @if (cargando()) {
        <app-cargando mensaje="Calculando la alineación…" />
      } @else if (error()) {
        <p class="alert alert--danger">{{ error() }}</p>
      } @else if (plantilla(); as p) {

        <a class="btn btn--ghost volver" [routerLink]="['/entrenador/sesion', p.idSesion]">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="19" y1="12" x2="5" y2="12"></line><polyline points="12 19 5 12 12 5"></polyline></svg>
          Volver a la evaluación
        </a>

        <header class="cabecera">
          <div>
            <h1>Formación sugerida por ranking</h1>
            <p class="subt">
              {{ p.categoria }} ·
              @if (alineacion()?.guardada) {
                <strong>formación puesta por el entrenador</strong>
              } @else {
                sugerida por el sistema, ordenada por promedio
              }
              @if (p.excluidosPorLesion.length > 0) {
                · {{ p.excluidosPorLesion.length }}
                jugador{{ p.excluidosPorLesion.length === 1 ? '' : 'es' }}
                excluido{{ p.excluidosPorLesion.length === 1 ? '' : 's' }} por lesión
              }
            </p>
          </div>
          <button class="btn btn--primary" (click)="pedirFeedback()" [disabled]="cargandoFeedback() || p.titulares.length === 0">
            @if (cargandoFeedback()) { <span class="spinner"></span> Pensando… } @else { ✦ Comentario con IA }
          </button>
        </header>

        @if (feedback(); as f) {
          <div class="alert" [class.alert--info]="f.generadoPorIa" [class.alert--warning]="!f.generadoPorIa">
            {{ f.generadoPorIa ? f.comentario : ('IA no disponible: ' + f.motivoNoDisponible) }}
          </div>
        }

        @if (p.titulares.length === 0) {
          <div class="card sin-alineacion">
            <h2>Todavía no hay alineación</h2>
            @if (p.suplentes.length === 0) {
              <p>
                Nadie tiene asistencia registrada en esta sesión, y la formación se arma
                solo con quienes estuvieron en la cancha.
              </p>
              <a class="btn btn--primary" [routerLink]="['/entrenador/sesion', p.idSesion, 'asistencia']">
                Pasar lista de asistencia
              </a>
            } @else {
              <p>
                {{ p.suplentes.length }} jugador{{ p.suplentes.length === 1 ? '' : 'es' }} con
                asistencia, pero ninguno tiene posición asignada: sin posición no se le puede
                dar un puesto en el campo.
              </p>
              <a class="btn btn--primary" [routerLink]="['/entrenador/sesion', p.idSesion]">
                Asignar posiciones en la evaluación
              </a>
            }
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
              <path d="M 0 12 A 12 12 0 0 0 12 0" fill="none" stroke="#ffffff66" stroke-width="2" />
              <path d="M 388 0 A 12 12 0 0 0 400 12" fill="none" stroke="#ffffff66" stroke-width="2" />
              <path d="M 400 548 A 12 12 0 0 0 388 560" fill="none" stroke="#ffffff66" stroke-width="2" />
              <path d="M 12 560 A 12 12 0 0 0 0 548" fill="none" stroke="#ffffff66" stroke-width="2" />

              @for (t of tokens(); track t.jugador.idEstudiante) {
                <g (pointerdown)="iniciarArrastre($event, t)"
                   (pointermove)="moverArrastre($event)"
                   (pointerup)="finalizarArrastre($event, t)"
                   (pointercancel)="finalizarArrastre($event, t)"
                   class="token"
                   [class.activo]="detalle()?.idEstudiante === t.jugador.idEstudiante"
                   [class.arrastrando]="idArrastrando() === t.jugador.idEstudiante">
                  <circle [attr.cx]="t.x" [attr.cy]="t.y" r="22" fill="#ffffff" [attr.stroke]="COLOR_ZONA[t.zona]" stroke-width="4" />
                  <text [attr.x]="t.x" [attr.y]="t.y + 5" text-anchor="middle" font-size="13" font-weight="700" fill="#1f2937">
                    {{ iniciales(t.jugador.nombreCompleto) }}
                  </text>
                  <text [attr.x]="t.x" [attr.y]="t.y + 38" text-anchor="middle" font-size="12" fill="#ffffff" font-weight="600">
                    {{ apellido(t.jugador.nombreCompleto) }}
                  </text>
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
            @if (huboArrastre()) {
              <button type="button" class="btn btn--ghost btn--sm reset-posiciones" (click)="restablecerPosiciones()">
                ↺ Restablecer posiciones sugeridas
              </button>
            }
          </div>

          <div class="panel-lateral">
            <aside class="card detalle-panel">
              @if (detalle(); as d) {
                <h2>{{ d.nombreCompleto }}</h2>
                <p class="posicion-detalle">{{ d.posicion ? etiquetaCompleta(d.posicion) : 'Sin posición registrada' }}</p>
                <p class="promedio">
                  <span class="valor">{{ d.promedioAcumulado }}</span>
                  <span class="unidad">/ 10 promedio acumulado</span>
                </p>
              } @else {
                <div class="vacio">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><path d="M12 6v6l4 2"></path></svg>
                  <p>Toca un jugador en el campo para ver su detalle, o arrastralo a otra posición</p>
                </div>
              }
            </aside>

            @if (banco().length > 0) {
              <section class="card suplentes">
                <h2>Banco</h2>
                <p class="ayuda-banco">Asistieron y están fuera del once. Pulsa «Entra» para hacer el cambio.</p>
                @for (s of banco(); track s.idEstudiante) {
                  <div class="suplente">
                    <span class="avatar avatar--muted">{{ iniciales(s.nombreCompleto) }}</span>
                    <span class="suplente-nombre">
                      {{ s.nombreCompleto }}
                      @if (s.posicion) { <span class="puesto-banco">{{ s.posicion }}</span> }
                    </span>
                    <span class="badge badge--info">{{ s.promedioAcumulado }}</span>
                    <button type="button" class="btn btn--ghost btn--sm" (click)="cambiar(s)">Entra</button>
                  </div>
                }
              </section>
            }

            <section class="card decision">
              <h2>La formación que pongo en cancha</h2>

              @if (detalle(); as d) {
                <button type="button" class="btn btn--ghost btn--sm sacar"
                        (click)="sacar(d)">Sacar a {{ apellido(d.nombreCompleto) }}</button>
              }

              <p class="etiqueta-val">¿Cómo funcionó?</p>
              <div class="estrellas" role="group" aria-label="Valoración de la formación">
                @for (v of estrellas; track v) {
                  <button type="button"
                          class="estrella"
                          [class.estrella--activa]="(valoracion() ?? 0) >= v"
                          [attr.aria-pressed]="valoracion() === v"
                          [attr.aria-label]="v + ' de 5'"
                          (click)="calificar(v)">★</button>
                }
                @if (valoracion()) { <span class="val-num">{{ valoracion() }}/5</span> }
              </div>

              <input class="obs" type="text" maxlength="500"
                     [ngModel]="observacion()" (ngModelChange)="observacion.set($event); sinGuardar.set(true)"
                     placeholder="Por qué este once (opcional)" />

              @if (mensaje(); as m) { <p class="ok">{{ m }}</p> }

              <div class="acciones">
                <button type="button" class="btn btn--primary btn--block"
                        [disabled]="guardando()" (click)="guardar()">
                  {{ guardando() ? 'Guardando…' : (sinGuardar() ? 'Guardar formación' : 'Guardar de nuevo') }}
                </button>
                @if (alineacion()?.guardada) {
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

    .cabecera { display: flex; justify-content: space-between; align-items: flex-start;
                gap: 1rem; flex-wrap: wrap; margin-bottom: .9rem; }
    h1 { font-size: 1.15rem; }
    .subt { margin-top: .3rem; color: var(--color-text-muted); font-size: .85rem; }

    .alert--info { background: var(--color-primary-50); color: var(--color-primary-700); }

    .cuerpo { display: flex; gap: 1.1rem; flex-wrap: wrap; margin-top: 1rem; align-items: flex-start; }
    .campo-envoltura { flex: 1 1 320px; min-width: 280px; }
    .campo { width: 100%; height: auto; display: block; box-shadow: var(--shadow-md); border-radius: 12px; touch-action: none; }
    .token { cursor: grab; touch-action: none; }
    .token circle { transition: stroke-width .15s; }
    .token:hover circle { stroke-width: 5; }
    .token.activo circle { stroke-width: 6; }
    .token.arrastrando { cursor: grabbing; }
    .token.arrastrando circle { stroke-width: 6; filter: drop-shadow(0 4px 6px rgb(0 0 0 / .35)); }

    .leyenda { display: flex; flex-wrap: wrap; gap: .8rem; margin-top: .75rem; font-size: .78rem; color: var(--color-text-muted); }
    .leyenda-item { display: inline-flex; align-items: center; gap: .35rem; }
    .punto { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
    .reset-posiciones { margin-top: .6rem; }

    /* Al costado de la cancha, no debajo de toda la pantalla: detalle y suplentes apilados en la misma columna. */
    .panel-lateral { flex: 1 1 240px; min-width: 220px; display: flex; flex-direction: column; gap: 1.1rem; }
    .detalle-panel { padding: 1.25rem; }
    .detalle-panel h2 { font-size: 1.05rem; margin-bottom: .3rem; }
    .posicion-detalle { color: var(--color-text-muted); font-size: .85rem; margin-bottom: 1rem; }
    .promedio .valor { font-size: 2rem; font-weight: 700; color: var(--color-primary-600); }
    .promedio .unidad { font-size: .8rem; color: var(--color-text-muted); margin-left: .3rem; }

    .sin-alineacion { text-align: center; padding: 2rem 1.5rem; margin-bottom: 1rem; }
    .sin-alineacion h2 { font-size: 1.05rem; margin: 0 0 .5rem; }
    .sin-alineacion p { color: var(--color-text-muted); font-size: .88rem; max-width: 46ch;
                        margin: 0 auto 1.1rem; line-height: 1.5; }
    .vacio { display: flex; flex-direction: column; align-items: center; gap: .65rem; text-align: center; color: var(--color-text-faint); padding: 1.5rem 0; }
    .vacio svg { width: 32px; height: 32px; opacity: .6; }
    .vacio p { font-size: .85rem; color: var(--color-text-muted); }

    .ayuda-banco { font-size: .76rem; color: var(--color-text-muted); margin: 0 0 .6rem; line-height: 1.4; }
    .puesto-banco { font-size: .7rem; color: var(--color-text-faint); margin-left: .35rem;
                    font-family: ui-monospace, monospace; }

    .decision { padding: 1.1rem 1.2rem; }
    .decision h2 { font-size: 1rem; margin-bottom: .7rem; }
    .sacar { width: 100%; margin-bottom: .8rem; }
    .etiqueta-val { font-size: .78rem; color: var(--color-text-muted); margin: 0 0 .3rem; }
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

    .suplentes { padding: 1.1rem 1.2rem; }
    .suplentes h2 { font-size: 1rem; margin-bottom: .6rem; }
    .suplente {
      display: flex; align-items: center; gap: .7rem; padding: .6rem .8rem;
      border: 1px solid var(--color-border-light); border-radius: var(--radius-sm); margin-bottom: .45rem; font-size: .9rem;
    }
    .suplente-nombre { flex: 1; }

    .volver { margin-bottom: 1rem; }

    .aviso { padding: 1rem; text-align: center; color: var(--color-text-muted); }
  `]
})
export class PlantillaComponent implements OnInit {

  private readonly ruta = inject(ActivatedRoute);
  private readonly servicio = inject(PlantillaService);

  readonly COLOR_ZONA = COLOR_ZONA;
  readonly ETIQUETA_ZONA = ETIQUETA_ZONA;
  readonly zonasLeyenda: ZonaCancha[] = ['POR', 'DEF', 'MED', 'DEL', 'SIN_POSICION'];

  readonly plantilla = signal<Plantilla | null>(null);
  readonly alineacion = signal<Alineacion | null>(null);
  /** Suplentes + presentes fuera del once: de aqui salen los cambios. */
  readonly banco = signal<JugadorPlantilla[]>([]);
  readonly valoracion = signal<number | null>(null);
  readonly observacion = signal<string>('');
  readonly guardando = signal(false);
  readonly mensaje = signal<string | null>(null);
  /** Hay cambios en pantalla que todavia no se mandaron al servidor. */
  readonly sinGuardar = signal(false);
  readonly estrellas = [1, 2, 3, 4, 5];
  private idSesion = 0;
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly detalle = signal<JugadorPlantilla | null>(null);
  readonly cargandoFeedback = signal(false);
  readonly feedback = signal<{ comentario: string | null; generadoPorIa: boolean; motivoNoDisponible: string | null } | null>(null);

  /**
   * Posiciones movidas a mano en esta pantalla (idEstudiante -> coordenadas
   * de la cancha). Es deliberadamente solo del cliente, no se manda al
   * backend ni sobrevive a un refresh: la alineacion "real" sigue siendo la
   * que calcula el servidor por promedio; esto es un ajuste visual puntual
   * para la demo, no una nueva fuente de verdad.
   */
  readonly arrastres = signal<Map<number, { x: number; y: number }>>(new Map());
  readonly huboArrastre = computed(() => this.arrastres().size > 0);
  readonly idArrastrando = signal<number | null>(null);
  private arrastreActual: { idEstudiante: number; inicioX: number; inicioY: number; movio: boolean } | null = null;

  /**
   * Cada posicion tiene una coordenada fija (COORDENADA_POR_ABREVIATURA):
   * como PlantillaService.calcular ahora titulariza a lo sumo un estudiante
   * por posicion, no hace falta repartir varios jugadores dentro de una
   * misma banda ancha como antes -- cada token va directo a su punto exacto.
   * Sin coordenada conocida (posicion no registrada, o un dato viejo que ya
   * no esta en el catalogo) se ubica al centro en vez de inventar una banda.
   */
  readonly tokens = computed<Token[]>(() => {
    const p = this.plantilla();
    if (!p) return [];
    const overrides = this.arrastres();

    return p.titulares.map((jugador) => {
      const manual = overrides.get(jugador.idEstudiante);
      const coord = (jugador.posicion && COORDENADA_POR_ABREVIATURA[jugador.posicion])
        || { x: 200, y: 280 };
      return {
        jugador,
        zona: zonaDe(jugador.posicion),
        x: manual?.x ?? coord.x,
        y: manual?.y ?? coord.y,
      };
    });
  });

  ngOnInit(): void {
    this.idSesion = Number(this.ruta.snapshot.paramMap.get('idSesion'));
    this.cargar();
  }

  /**
   * Pide la alineacion, no la sugerencia. El servidor decide cual devolver:
   * la que guardo el entrenador si existe, la calculada si no. La pantalla
   * solo necesita saber cual es para decirlo, via `guardada`.
   */
  private cargar(): void {
    this.cargando.set(true);
    this.servicio.obtenerAlineacion(this.idSesion).subscribe({
      next: (a) => this.recibir(a),
      error: (e) => {
        this.cargando.set(false);
        this.error.set(e.status === 404 ? 'Esa sesión no existe.' : 'No se pudo calcular la alineación.');
      },
    });
  }

  private recibir(a: Alineacion): void {
    this.alineacion.set(a);
    this.plantilla.set({
      idSesion: a.idSesion, categoria: a.categoria,
      titulares: a.titulares, suplentes: a.suplentes, excluidosPorLesion: [],
    });
    this.banco.set([...a.suplentes, ...a.disponibles]);
    this.valoracion.set(a.valoracion);
    this.observacion.set(a.observacion ?? '');
    this.sinGuardar.set(false);
    this.cargando.set(false);
  }

  /** Mete a un suplente por el titular del mismo puesto. */
  cambiar(entra: JugadorPlantilla): void {
    const p = this.plantilla();
    if (!p) return;

    const sale = p.titulares.find((t) => t.posicion === entra.posicion);
    const titulares = sale
      ? p.titulares.map((t) => (t.idEstudiante === sale.idEstudiante ? entra : t))
      : [...p.titulares, entra];
    const banco = this.banco().filter((b) => b.idEstudiante !== entra.idEstudiante);
    if (sale) banco.push(sale);

    this.plantilla.set({ ...p, titulares });
    this.banco.set(banco);
    this.sinGuardar.set(true);
    this.mensaje.set(null);
  }

  /** Saca a un titular al banco sin meter a nadie. */
  sacar(sale: JugadorPlantilla): void {
    const p = this.plantilla();
    if (!p) return;
    this.plantilla.set({ ...p, titulares: p.titulares.filter((t) => t.idEstudiante !== sale.idEstudiante) });
    this.banco.set([...this.banco(), sale]);
    this.sinGuardar.set(true);
    this.mensaje.set(null);
  }

  guardar(): void {
    const p = this.plantilla();
    if (!p || this.guardando()) return;

    const jugadores: JugadorEnCancha[] = [
      ...p.titulares.map((t) => ({
        idEstudiante: t.idEstudiante, idPosicion: t.idPosicion ?? null, titular: true })),
      ...this.banco().map((b) => ({
        idEstudiante: b.idEstudiante, idPosicion: b.idPosicion ?? null, titular: false })),
    ];

    this.guardando.set(true);
    this.error.set(null);
    this.servicio.guardarAlineacion(this.idSesion, jugadores,
                                    this.valoracion(), this.observacion().trim() || null)
      .subscribe({
        next: (a) => {
          this.recibir(a);
          this.guardando.set(false);
          this.mensaje.set('Formación guardada');
        },
        error: (e) => {
          this.guardando.set(false);
          this.error.set(e?.error?.detail ?? 'No se pudo guardar la formación.');
        },
      });
  }

  restablecer(): void {
    if (this.guardando()) return;
    this.guardando.set(true);
    this.servicio.restablecerAlineacion(this.idSesion).subscribe({
      next: (a) => {
        this.recibir(a);
        this.arrastres.set(new Map());
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

  verDetalle(jugador: JugadorPlantilla): void {
    this.detalle.set(jugador);
  }

  /**
   * Arrastre con Pointer Events (unifica mouse/touch/lapiz, sirve igual en
   * celular). setPointerCapture ata los eventos siguientes al mismo <g>
   * aunque el dedo/cursor se salga de su area, para que el arrastre no se
   * corte. Un movimiento menor a 4px se trata como clic (ver
   * finalizarArrastre), no como arrastre.
   */
  iniciarArrastre(event: PointerEvent, t: Token): void {
    this.arrastreActual = { idEstudiante: t.jugador.idEstudiante, inicioX: event.clientX, inicioY: event.clientY, movio: false };
    this.idArrastrando.set(t.jugador.idEstudiante);

    // La captura va despues de fijar el estado y entre try/catch a
    // proposito: es una mejora -mantiene los eventos llegando aunque el
    // puntero se salga del token-, no un requisito. Si se hiciera primero
    // y fallara, el arrastre quedaria muerto sin haber registrado nada.
    try {
      (event.currentTarget as SVGGraphicsElement).setPointerCapture(event.pointerId);
    } catch {
      // Sin captura el arrastre sigue funcionando mientras el puntero no
      // abandone el token; no hay nada que reportar al usuario.
    }

    event.preventDefault();
  }

  moverArrastre(event: PointerEvent): void {
    const actual = this.arrastreActual;
    if (!actual) return;

    if (!actual.movio) {
      const dx = event.clientX - actual.inicioX;
      const dy = event.clientY - actual.inicioY;
      if (Math.hypot(dx, dy) < 4) return;
      actual.movio = true;
    }

    const svg = (event.currentTarget as SVGGraphicsElement).ownerSVGElement;
    const punto = this.aCoordenadasSvg(svg, event.clientX, event.clientY);
    if (!punto) return;

    const copia = new Map(this.arrastres());
    copia.set(actual.idEstudiante, {
      x: Math.min(378, Math.max(22, punto.x)),
      y: Math.min(538, Math.max(22, punto.y)),
    });
    this.arrastres.set(copia);
  }

  finalizarArrastre(event: PointerEvent, t: Token): void {
    const actual = this.arrastreActual;
    this.arrastreActual = null;
    this.idArrastrando.set(null);
    if (!actual) return;
    if (!actual.movio) this.verDetalle(t.jugador);
  }

  restablecerPosiciones(): void {
    this.arrastres.set(new Map());
  }

  private aCoordenadasSvg(svg: SVGSVGElement | null, clienteX: number, clienteY: number): DOMPoint | null {
    if (!svg) return null;
    const ctm = svg.getScreenCTM();
    if (!ctm) return null;
    const punto = svg.createSVGPoint();
    punto.x = clienteX;
    punto.y = clienteY;
    return punto.matrixTransform(ctm.inverse());
  }

  pedirFeedback(): void {
    const p = this.plantilla();
    if (!p) return;
    this.cargandoFeedback.set(true);
    this.servicio.pedirFeedback(p.idSesion).subscribe({
      next: (f) => { this.feedback.set(f); this.cargandoFeedback.set(false); },
      error: () => {
        this.feedback.set({ comentario: null, generadoPorIa: false, motivoNoDisponible: 'No se pudo contactar al servicio' });
        this.cargandoFeedback.set(false);
      },
    });
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
