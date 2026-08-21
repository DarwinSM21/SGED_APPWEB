import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { EvaluacionService } from './evaluacion.service';
import { EvaluacionSesion, JugadorEvaluable, PosicionOpcion } from './evaluacion.models';
import { inicialesDe } from './plantilla.models';
import { mensajeDeError } from '../../core/mensaje-error';

const DIAS = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];
const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];

function formatearFechaEs(fechaIso: string): string {
  // new Date('2026-07-31') se interpreta en UTC; se parsean las partes a
  // mano para no perder un dia por la zona horaria local del navegador.
  const [anio, mes, dia] = fechaIso.split('-').map(Number);
  const fecha = new Date(anio, mes - 1, dia);
  return `${DIAS[fecha.getDay()]} ${dia} ${MESES[mes - 1]} ${anio}`;
}

const ESTADO_ETIQUETA: Partial<Record<string, string>> = {
  PRESENTE: 'PRESENTE', TARDE: 'TARDE', AUSENTE: 'AUSENTE', JUSTIFICADO: 'JUSTIFICADO',
};

/**
 * Pantalla de evaluacion diaria: tarjetas de jugador plegables, pensadas
 * para usarse con una mano, de pie en la cancha y con sol de frente. Los
 * jugadores que no marcaron asistencia aparecen bloqueados con el motivo a
 * la vista en vez de ocultarse, para que el entrenador entienda por que no
 * puede calificarlos.
 */
@Component({
  selector: 'app-evaluacion-diaria',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="pantalla">
      <a class="btn btn--ghost volver" routerLink="/entrenador/sesiones">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="19" y1="12" x2="5" y2="12"></line><polyline points="12 19 5 12 12 5"></polyline></svg>
        Volver a mis sesiones
      </a>
      <h1 class="titulo-pantalla">Evaluación diaria</h1>

      @if (cargando()) {
        <p class="aviso">Cargando la sesión…</p>
      } @else if (error()) {
        <p class="alert alert--danger">{{ error() }}</p>
      } @else if (sesion(); as s) {

        <div class="banner">
          <div>
            <p class="banner-titulo">Evaluación diaria — {{ s.categoria }}</p>
            <p class="banner-sub">
              {{ fechaFormateada() }}
              @if (hayPrecargados()) { · Valores heredados del día anterior }
            </p>
          </div>
          @if (s.estado === 'BORRADOR') {
            <button class="btn btn--primary" (click)="finalizar()" [disabled]="finalizando()">
              @if (finalizando()) { <span class="spinner"></span> Finalizando… } @else { Finalizar sesión }
            </button>
          } @else {
            <span class="badge">Finalizada</span>
          }
        </div>

        <p class="estado-guardado" [class]="claseEstado()">
          <span class="punto-estado"></span>{{ textoEstado() }}
        </p>

        @if (errorPosicion()) { <p class="alert alert--danger">{{ errorPosicion() }}</p> }

        @for (j of s.jugadores; track j.idEstudiante) {
          <article class="card jugador" [class.bloqueado]="!j.puedeEvaluarse">
            <button type="button" class="jugador-cabecera" (click)="alternar(j.idEstudiante)"
                    [attr.aria-expanded]="estaExpandido(j.idEstudiante)">
              <span class="avatar" [class.avatar--muted]="!j.puedeEvaluarse">{{ iniciales(j.nombreCompleto) }}</span>
              <span class="jugador-info">
                <span class="nombre">{{ j.nombreCompleto }}</span>
                <span class="categoria-chica">
                  {{ j.categoria }}
                  @if (j.lesionado) { · <span class="texto-lesion">Lesionado</span> }
                  @if (j.precargado && j.puedeEvaluarse) { · Valores heredados }
                </span>
              </span>
              <span class="badge" [class]="'estado-' + (j.estadoAsistencia ?? 'sin_marcar').toLowerCase()">
                {{ etiquetaEstado(j.estadoAsistencia) }}
              </span>
              <svg class="chevron" [class.abierto]="estaExpandido(j.idEstudiante)" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"></polyline></svg>
            </button>

            @if (estaExpandido(j.idEstudiante)) {
              <div class="panel-lesion">
                @if (!j.lesionado) {
                  @if (formularioLesionAbierto() === j.idEstudiante) {
                    <div class="form-lesion">
                      <label class="campo-lesion">
                        <span>Descripción</span>
                        <textarea rows="2" placeholder="¿Qué le pasó?"
                                  [ngModel]="descripcionLesion()" (ngModelChange)="descripcionLesion.set($event)"></textarea>
                      </label>
                      <label class="campo-lesion">
                        <span>Fecha estimada de retorno (opcional)</span>
                        <input type="date" [ngModel]="fechaRetornoLesion()" (ngModelChange)="fechaRetornoLesion.set($event)" />
                      </label>
                      @if (errorLesion()) { <p class="alert alert--danger">{{ errorLesion() }}</p> }
                      <div class="acciones-lesion">
                        <button type="button" class="btn btn--danger btn--sm" (click)="guardarLesion(j)" [disabled]="guardandoLesion()">
                          @if (guardandoLesion()) { <span class="spinner"></span> Guardando… } @else { Guardar }
                        </button>
                        <button type="button" class="btn btn--ghost btn--sm" (click)="cancelarFormularioLesion()">Cancelar</button>
                      </div>
                    </div>
                  } @else {
                    <button type="button" class="btn btn--ghost btn--sm" (click)="abrirFormularioLesion(j.idEstudiante)">
                      Marcar lesión
                    </button>
                  }
                } @else {
                  <div class="lesion-activa">
                    @if (errorLesion() && dandoDeAlta() === j.idEstudiante) {
                      <p class="alert alert--danger">{{ errorLesion() }}</p>
                    }
                    <button type="button" class="btn btn--secondary btn--sm" (click)="darDeAlta(j)" [disabled]="dandoDeAlta() === j.idEstudiante">
                      @if (dandoDeAlta() === j.idEstudiante) { <span class="spinner"></span> Dando de alta… } @else { Dar de alta }
                    </button>
                  </div>
                }
              </div>

              <label class="campo-posicion">
                <span>Posición</span>
                <select
                  [ngModel]="j.idPosicion"
                  (ngModelChange)="cambiarPosicion(j, $event)"
                  [disabled]="guardandoPosicion() === j.idEstudiante"
                  [attr.aria-label]="'Posición de ' + j.nombreCompleto">
                  <option [ngValue]="null">Sin posición</option>
                  @for (p of posiciones(); track p.idPosicion) {
                    <option [ngValue]="p.idPosicion">{{ p.nombre }} ({{ p.abreviatura }})</option>
                  }
                </select>
              </label>

              @if (!j.puedeEvaluarse) {
                <p class="motivo">{{ j.motivoBloqueo }}</p>
              } @else {
                <div class="criterios">
                  @for (c of s.criterios; track c.idCriterio) {
                    <label class="criterio">
                      <span class="criterio-nombre">
                        {{ c.nombre }}
                        <b>{{ valor(j, c.nombre) }}/{{ c.puntajeMaximo }}</b>
                      </span>
                      <input
                        type="range"
                        min="0" [max]="c.puntajeMaximo" step="0.5"
                        [ngModel]="valor(j, c.nombre)"
                        (ngModelChange)="cambiar(j, c.nombre, c.idCriterio, $event)"
                        [disabled]="s.estado === 'FINALIZADA'"
                        [attr.aria-label]="c.nombre + ' de ' + j.nombreCompleto" />
                      <span class="escala"><span>Bajo</span><span>Medio</span><span>Alto</span></span>
                    </label>
                  }
                </div>
              }
            }
          </article>
        } @empty {
          <div class="vacio">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
            <p>Nadie marcó asistencia en esta sesión todavía.</p>
          </div>
        }

        <a class="btn btn--secondary btn--block ver-plantilla" [routerLink]="['/entrenador/sesion', s.idSesion, 'plantilla']">
          Ver formación sugerida
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="12" x2="19" y2="12"></line><polyline points="12 5 19 12 12 19"></polyline></svg>
        </a>
      }
    </div>
  `,
  styles: [`
    .pantalla { max-width: 640px; margin: 0 auto; padding: 1.25rem 1rem 3rem; }

    .volver { margin-bottom: 1rem; }

    .titulo-pantalla { font-size: 1.2rem; margin-bottom: 1.1rem; }

    .banner {
      display: flex; justify-content: space-between; align-items: center; gap: .75rem;
      background: var(--color-info-bg); border: 1px solid var(--color-primary-100); border-radius: var(--radius-md);
      padding: 1rem 1.15rem; margin-bottom: .85rem; flex-wrap: wrap;
    }
    .banner-titulo { font-weight: 700; color: var(--color-primary-700); font-size: .95rem; }
    .banner-sub { margin-top: .2rem; font-size: .8rem; color: var(--color-info-text); }

    .estado-guardado {
      display: flex; align-items: center; gap: .4rem;
      font-size: .78rem; margin: 0 .1rem .9rem; color: var(--color-success-text); font-weight: 600;
    }
    .punto-estado { width: 7px; height: 7px; border-radius: 50%; background: currentColor; flex-shrink: 0; }
    .estado-guardado.trabajando { color: var(--color-warning-text); }
    .estado-guardado.pendiente { color: var(--color-danger-text); }

    .aviso { padding: .75rem .1rem; color: var(--color-text-muted); }

    .vacio {
      display: flex; flex-direction: column; align-items: center; gap: .65rem;
      color: var(--color-text-faint); text-align: center; padding: 2.5rem 1rem;
    }
    .vacio svg { width: 34px; height: 34px; opacity: .6; }
    .vacio p { font-size: .88rem; color: var(--color-text-muted); }

    .jugador { margin-bottom: .65rem; overflow: hidden; }
    .jugador.bloqueado { background: var(--color-border-light); }

    .jugador-cabecera {
      display: flex; align-items: center; gap: .7rem; width: 100%;
      padding: .85rem .95rem; background: none; border: none; cursor: pointer;
      text-align: left; font: inherit; color: inherit;
    }
    .jugador-info { display: flex; flex-direction: column; flex: 1; min-width: 0; }
    .nombre { font-weight: 600; }
    .categoria-chica { font-size: .76rem; color: var(--color-text-muted); }
    .texto-lesion { color: var(--color-danger-text); font-weight: 700; }

    .badge.estado-presente { background: var(--color-success-bg); color: var(--color-success-text); }
    .badge.estado-tarde { background: var(--color-warning-bg); color: var(--color-warning-text); }
    .badge.estado-ausente, .badge.estado-justificado, .badge.estado-sin_marcar { background: var(--color-neutral-bg); color: var(--color-neutral-text); }

    .chevron { width: 19px; height: 19px; color: var(--color-text-faint); flex-shrink: 0; transition: transform .15s; }
    .chevron.abierto { transform: rotate(90deg); }

    .motivo { margin: 0 .95rem .9rem; color: var(--color-text-muted); font-size: .875rem; }

    .campo-posicion {
      display: flex; flex-direction: column; gap: .35rem; font-size: .8rem; font-weight: 600;
      color: var(--color-text); padding: .8rem .95rem 0; border-top: 1px solid var(--color-border-light);
    }
    .campo-posicion select {
      border: 1.5px solid var(--color-border); border-radius: var(--radius-sm);
      padding: .5rem .6rem; font-size: .85rem; font-family: inherit;
      background: var(--color-surface); color: var(--color-text);
    }

    .criterios { padding: .2rem .95rem .95rem; }

    .panel-lesion { padding: .7rem .95rem; border-top: 1px solid var(--color-border-light); }
    .btn--sm { padding: .4rem .8rem; font-size: .8rem; }
    .form-lesion { display: flex; flex-direction: column; gap: .7rem; }
    .campo-lesion { display: flex; flex-direction: column; gap: .35rem; font-size: .8rem; font-weight: 600; color: var(--color-text); }
    .campo-lesion textarea, .campo-lesion input {
      border: 1.5px solid var(--color-border); border-radius: var(--radius-sm);
      padding: .6rem .7rem; font-size: .85rem; font-family: inherit;
      background: var(--color-surface); color: var(--color-text); resize: vertical;
    }
    .acciones-lesion { display: flex; gap: .5rem; }
    .lesion-activa { display: flex; flex-direction: column; gap: .5rem; align-items: flex-start; }

    .criterio { display: block; margin-top: .9rem; }
    .criterio-nombre { display: flex; justify-content: space-between; font-size: .875rem; margin-bottom: .3rem; }
    .criterio-nombre b { color: var(--color-primary-600); }
    .escala { display: flex; justify-content: space-between; font-size: .68rem; color: var(--color-text-faint); margin-top: .15rem; }

    /* Alto generoso: se manipula con el pulgar, no con un raton. */
    input[type=range] {
      width: 100%; height: 2.25rem; -webkit-appearance: none; appearance: none;
      background: transparent; cursor: pointer;
    }
    input[type=range]::-webkit-slider-runnable-track {
      height: 6px; border-radius: 999px; background: var(--color-border);
    }
    input[type=range]::-webkit-slider-thumb {
      -webkit-appearance: none; margin-top: -9px;
      width: 22px; height: 22px; border-radius: 50%;
      background: #fff; border: 3px solid var(--color-primary-600); box-shadow: var(--shadow-sm);
    }
    input[type=range]:disabled::-webkit-slider-thumb { border-color: var(--color-text-faint); }
    input[type=range]::-moz-range-track { height: 6px; border-radius: 999px; background: var(--color-border); }
    input[type=range]::-moz-range-progress { height: 6px; border-radius: 999px; background: var(--color-primary-500); }
    input[type=range]::-moz-range-thumb {
      width: 16px; height: 16px; border-radius: 50%;
      background: #fff; border: 3px solid var(--color-primary-600); box-shadow: var(--shadow-sm);
    }

    .ver-plantilla { margin-top: 1.4rem; }
  `]
})
export class EvaluacionDiariaComponent implements OnInit {

  private readonly ruta = inject(ActivatedRoute);
  private readonly servicio = inject(EvaluacionService);

  readonly sesion = signal<EvaluacionSesion | null>(null);
  readonly posiciones = signal<PosicionOpcion[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly finalizando = signal(false);
  /** idEstudiante cuya posición se está guardando ahora mismo, o null. */
  readonly guardandoPosicion = signal<number | null>(null);
  readonly errorPosicion = signal<string | null>(null);
  readonly expandidos = signal<Set<number>>(new Set());

  /** Formulario inline de "Marcar lesión": qué jugador lo tiene abierto, o null si ninguno. */
  readonly formularioLesionAbierto = signal<number | null>(null);
  readonly descripcionLesion = signal('');
  readonly fechaRetornoLesion = signal('');
  readonly guardandoLesion = signal(false);
  /** idEstudiante de quien se está dando de alta ahora mismo, o null. */
  readonly dandoDeAlta = signal<number | null>(null);
  readonly errorLesion = signal('');

  private idSesion!: number;

  readonly hayPrecargados = computed(() =>
    (this.sesion()?.jugadores ?? []).some((j) => j.precargado && j.puedeEvaluarse));

  readonly fechaFormateada = computed(() => {
    const s = this.sesion();
    return s ? formatearFechaEs(s.fecha) : '';
  });

  readonly claseEstado = computed(() => {
    switch (this.servicio.estado()) {
      case 'guardado': return '';
      case 'guardando': return 'trabajando';
      default: return 'pendiente';
    }
  });

  readonly textoEstado = computed(() => {
    switch (this.servicio.estado()) {
      case 'guardado': return 'Todo guardado';
      case 'guardando': return 'Guardando…';
      case 'pendiente': return `Sin conexión · ${this.servicio.pendientes()} por enviar`;
      case 'error': return 'No se pudo guardar el último cambio';
    }
  });

  ngOnInit(): void {
    this.idSesion = Number(this.ruta.snapshot.paramMap.get('idSesion'));
    this.servicio.posicionesActivas().subscribe({ next: (p) => this.posiciones.set(p) });
    this.servicio.abrirSesion(this.idSesion).subscribe({
      next: (s) => {
        this.sesion.set(s);
        this.cargando.set(false);
      },
      error: (e) => {
        this.cargando.set(false);
        this.error.set(e.status === 404
          ? 'Esa sesión de entrenamiento no existe.'
          : 'No se pudo cargar la sesión.');
      },
    });
  }

  estaExpandido(idEstudiante: number): boolean {
    return this.expandidos().has(idEstudiante);
  }

  alternar(idEstudiante: number): void {
    const actuales = new Set(this.expandidos());
    if (actuales.has(idEstudiante)) {
      actuales.delete(idEstudiante);
    } else {
      actuales.add(idEstudiante);
    }
    this.expandidos.set(actuales);
  }

  valor(jugador: JugadorEvaluable, criterio: string): number {
    return jugador.puntajes[criterio] ?? 0;
  }

  /**
   * Un movimiento de slider. Actualiza la vista al instante y programa el
   * envio: la interfaz no espera al servidor para responder.
   */
  cambiar(jugador: JugadorEvaluable, criterio: string, idCriterio: number, valor: number): void {
    jugador.puntajes = { ...jugador.puntajes, [criterio]: valor };
    // Deja de estar "heredado" en cuanto el entrenador lo toca.
    jugador.precargado = false;

    const s = this.sesion();
    if (!s) { return; }

    this.servicio.guardarConRetardo(this.idSesion, {
      idEstudiante: jugador.idEstudiante,
      idPosicionJugada: jugador.idPosicion,
      puntajes: s.criterios.map((c) => ({
        idCriterio: c.idCriterio,
        puntaje: jugador.puntajes[c.nombre] ?? 0,
      })),
    });
  }

  /**
   * Posición nominal del estudiante -- la misma que edita ADMINISTRADOR
   * desde Personas, no un valor aparte de esta sesión. Se guarda contra
   * PUT /api/estudiantes/{id}/posicion (no contra el autoguardado de la
   * evaluación): a diferencia de los criterios, un fallo acá sí se muestra,
   * y si falla se revierte el select al valor anterior en vez de dejarlo
   * mostrando algo que no se guardó.
   */
  cambiarPosicion(jugador: JugadorEvaluable, idPosicion: number | null): void {
    const anterior = { idPosicion: jugador.idPosicion, posicion: jugador.posicion };
    jugador.idPosicion = idPosicion;
    jugador.posicion = idPosicion === null
      ? null
      : (this.posiciones().find((p) => p.idPosicion === idPosicion)?.nombre ?? null);

    this.guardandoPosicion.set(jugador.idEstudiante);
    this.errorPosicion.set(null);
    this.servicio.actualizarPosicionEstudiante(jugador.idEstudiante, idPosicion).subscribe({
      next: () => this.guardandoPosicion.set(null),
      error: (err) => {
        jugador.idPosicion = anterior.idPosicion;
        jugador.posicion = anterior.posicion;
        this.guardandoPosicion.set(null);
        this.errorPosicion.set(mensajeDeError(err, 'No se pudo guardar la posición.'));
      },
    });
  }

  finalizar(): void {
    const s = this.sesion();
    if (!s || this.finalizando()) return;
    this.finalizando.set(true);
    this.servicio.finalizar(this.idSesion, s.observacionGeneral ?? '').subscribe({
      next: () => {
        this.finalizando.set(false);
        this.sesion.set({ ...s, estado: 'FINALIZADA' });
      },
      error: () => { this.finalizando.set(false); },
    });
  }

  abrirFormularioLesion(idEstudiante: number): void {
    this.errorLesion.set('');
    this.descripcionLesion.set('');
    this.fechaRetornoLesion.set('');
    this.formularioLesionAbierto.set(idEstudiante);
  }

  cancelarFormularioLesion(): void {
    this.formularioLesionAbierto.set(null);
    this.errorLesion.set('');
  }

  guardarLesion(jugador: JugadorEvaluable): void {
    const descripcion = this.descripcionLesion().trim();
    if (!descripcion) {
      this.errorLesion.set('Describí qué le pasó antes de guardar.');
      return;
    }
    this.errorLesion.set('');
    this.guardandoLesion.set(true);

    this.servicio.registrarLesion(
      jugador.idEstudiante, descripcion, this.fechaRetornoLesion() || undefined,
    ).subscribe({
      next: (lesion) => {
        jugador.lesionado = true;
        jugador.idLesion = lesion.idLesion;
        this.guardandoLesion.set(false);
        this.formularioLesionAbierto.set(null);
      },
      error: (err) => {
        this.guardandoLesion.set(false);
        this.errorLesion.set(mensajeDeError(err, 'No se pudo registrar la lesión.'));
      },
    });
  }

  darDeAlta(jugador: JugadorEvaluable): void {
    if (!jugador.idLesion || this.dandoDeAlta() === jugador.idEstudiante) return;
    this.errorLesion.set('');
    this.dandoDeAlta.set(jugador.idEstudiante);

    this.servicio.darDeAltaLesion(jugador.idLesion).subscribe({
      next: () => {
        jugador.lesionado = false;
        jugador.idLesion = null;
        this.dandoDeAlta.set(null);
      },
      error: (err) => {
        this.dandoDeAlta.set(null);
        this.errorLesion.set(mensajeDeError(err, 'No se pudo dar de alta la lesión.'));
      },
    });
  }

  iniciales(nombre: string): string {
    return inicialesDe(nombre);
  }

  etiquetaEstado(estado: string | null): string {
    if (estado === null) return 'SIN MARCAR';
    return ESTADO_ETIQUETA[estado] ?? estado;
  }
}
