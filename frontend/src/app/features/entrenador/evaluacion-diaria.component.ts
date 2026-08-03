import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { EvaluacionService } from './evaluacion.service';
import { EvaluacionSesion, JugadorEvaluable } from './evaluacion.models';

/**
 * Pantalla de evaluacion diaria.
 *
 * Disenada para usarse con una mano, de pie en la cancha y con sol de frente:
 * una tarjeta por jugador, sliders grandes y ningun boton de guardar. Los
 * jugadores que no marcaron asistencia aparecen bloqueados con el motivo a la
 * vista, en vez de ocultarse, para que el entrenador entienda por que no puede
 * calificarlos.
 */
@Component({
  selector: 'app-evaluacion-diaria',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="pantalla">
      @if (cargando()) {
        <p class="aviso">Cargando la sesión…</p>
      } @else if (error()) {
        <p class="aviso error">{{ error() }}</p>
      } @else if (sesion(); as s) {

        <header class="cabecera">
          <div>
            <h1>{{ s.categoria }}</h1>
            <p class="fecha">{{ s.fecha }}</p>
          </div>
          <span class="estado" [class]="claseEstado()">{{ textoEstado() }}</span>
        </header>

        @if (s.estado === 'FINALIZADA') {
          <p class="aviso cerrada">Evaluación finalizada. No admite cambios.</p>
        }

        @for (j of s.jugadores; track j.idEstudiante) {
          <article class="jugador" [class.bloqueado]="!j.puedeEvaluarse">
            <div class="jugador-cabecera">
              <span class="nombre">{{ j.nombreCompleto }}</span>
              @if (j.lesionado) {
                <span class="etiqueta lesion">Lesionado</span>
              }
              @if (j.precargado && j.puedeEvaluarse) {
                <span class="etiqueta previo">Valores del día anterior</span>
              }
            </div>

            @if (!j.puedeEvaluarse) {
              <p class="motivo">{{ j.motivoBloqueo }}</p>
            } @else {
              @for (c of s.criterios; track c.idCriterio) {
                <label class="criterio">
                  <span class="criterio-nombre">
                    {{ c.nombre }}
                    <b>{{ valor(j, c.nombre) }}</b>
                  </span>
                  <input
                    type="range"
                    min="0" [max]="c.puntajeMaximo" step="0.5"
                    [ngModel]="valor(j, c.nombre)"
                    (ngModelChange)="cambiar(j, c.nombre, c.idCriterio, $event)"
                    [disabled]="s.estado === 'FINALIZADA'"
                    [attr.aria-label]="c.nombre + ' de ' + j.nombreCompleto" />
                </label>
              }
            }
          </article>
        } @empty {
          <p class="aviso">Nadie marcó asistencia en esta sesión todavía.</p>
        }
      }
    </div>
  `,
  styles: [`
    .pantalla { max-width: 640px; margin: 0 auto; padding: 1rem; }

    .cabecera { display: flex; justify-content: space-between; align-items: flex-start;
                gap: 1rem; margin-bottom: 1rem; }
    h1 { font-size: 1.25rem; margin: 0; }
    .fecha { margin: 0; color: #666; font-size: .875rem; }

    .estado { font-size: .75rem; padding: .25rem .6rem; border-radius: 999px; white-space: nowrap; }
    .estado.ok { background: #e6f4ea; color: #1e7e34; }
    .estado.trabajando { background: #fff4e5; color: #a35c00; }
    .estado.pendiente { background: #fdecea; color: #b3261e; }

    .aviso { padding: .75rem; border-radius: 6px; background: #f5f5f5; }
    .aviso.error { background: #fdecea; color: #b3261e; }
    .aviso.cerrada { background: #e8eaf6; color: #283593; }

    .jugador { border: 1px solid #e0e0e0; border-radius: 10px;
               padding: .875rem; margin-bottom: .75rem; }
    .jugador.bloqueado { background: #fafafa; border-style: dashed; }

    .jugador-cabecera { display: flex; align-items: center; flex-wrap: wrap; gap: .5rem;
                        margin-bottom: .5rem; }
    .nombre { font-weight: 600; }
    .etiqueta { font-size: .7rem; padding: .15rem .5rem; border-radius: 999px; }
    .etiqueta.lesion { background: #fdecea; color: #b3261e; }
    .etiqueta.previo { background: #fff8e1; color: #8d6e00; }
    .motivo { margin: 0; color: #777; font-size: .875rem; }

    .criterio { display: block; margin-top: .75rem; }
    .criterio-nombre { display: flex; justify-content: space-between;
                       font-size: .875rem; margin-bottom: .25rem; }

    /* Alto generoso: se manipula con el pulgar, no con un raton. */
    input[type=range] { width: 100%; height: 2.25rem; }
  `]
})
export class EvaluacionDiariaComponent implements OnInit {

  private readonly ruta = inject(ActivatedRoute);
  private readonly servicio = inject(EvaluacionService);

  readonly sesion = signal<EvaluacionSesion | null>(null);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  private idSesion!: number;

  readonly claseEstado = computed(() => {
    switch (this.servicio.estado()) {
      case 'guardado': return 'ok';
      case 'guardando': return 'trabajando';
      default: return 'pendiente';
    }
  });

  readonly textoEstado = computed(() => {
    switch (this.servicio.estado()) {
      case 'guardado': return 'Guardado';
      case 'guardando': return 'Guardando…';
      case 'pendiente': return `Sin conexión · ${this.servicio.pendientes()} por enviar`;
      case 'error': return 'No se pudo guardar';
    }
  });

  ngOnInit(): void {
    this.idSesion = Number(this.ruta.snapshot.paramMap.get('idSesion'));
    this.servicio.abrirSesion(this.idSesion).subscribe({
      next: (s) => { this.sesion.set(s); this.cargando.set(false); },
      error: (e) => {
        this.cargando.set(false);
        this.error.set(e.status === 404
          ? 'Esa sesión de entrenamiento no existe.'
          : 'No se pudo cargar la sesión.');
      },
    });
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
}
