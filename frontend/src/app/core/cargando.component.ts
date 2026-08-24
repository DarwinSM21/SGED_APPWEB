import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, input, signal } from '@angular/core';

/**
 * Indicador de carga compartido: anillo dorado girando con la sigla SGED fija
 * en el centro.
 *
 * <p>Sustituye a los catorce <code>&lt;p class="aviso"&gt;Cargando…&lt;/p&gt;</code>
 * sueltos que había repartidos por las pantallas: texto plano, sin nada que
 * indicara que el sistema seguía trabajando. En un celular con la red de la
 * cancha, medio segundo sin señal de vida ya parece que la aplicación se colgó.
 *
 * <p>La sigla no gira. Solo lo hace el anillo: un texto rotando se vuelve
 * ilegible y deja de ser una marca para convertirse en ruido.
 *
 * <p><b>No aparece de inmediato.</b> Espera 250 ms antes de mostrarse, porque
 * una petición que resuelve en 80 ms haría parpadear el indicador y eso molesta
 * más de lo que informa: el usuario percibe un salto, no una espera. Si la
 * respuesta llega antes del umbral, el contenido aparece directamente y aquí no
 * se dibuja nada.
 */
@Component({
  selector: 'app-cargando',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (visible()) {
      <div class="cargando" role="status" aria-live="polite">
        <div class="marca">
          <span class="anillo" aria-hidden="true"></span>
          <span class="sigla" aria-hidden="true">SGED</span>
        </div>
        <span class="texto">{{ mensaje() }}</span>
      </div>
    }
  `,
  styles: [`
    .cargando {
      display: flex; flex-direction: column; align-items: center; justify-content: center;
      gap: .95rem; padding: 2.75rem 1rem; text-align: center;
      animation: aparecer .2s ease-out;
    }

    .marca {
      position: relative;
      width: 78px; height: 78px;
      display: grid; place-items: center;
    }

    /* El anillo se dibuja con dos lados dorados y dos transparentes: al girar,
       el hueco es lo que hace legible el movimiento. Un círculo dorado completo
       giraría sin que se notara. */
    .anillo {
      position: absolute; inset: 0;
      border-radius: 50%;
      border: 3px solid transparent;
      border-top-color: var(--oro);
      border-right-color: var(--oro);
      animation: girar .95s cubic-bezier(.5, .1, .5, .9) infinite;
    }

    /* Aro de fondo tenue, para que el anillo no parezca flotar en el vacío. */
    .marca::before {
      content: ""; position: absolute; inset: 0;
      border-radius: 50%;
      border: 3px solid var(--oro-tenue);
    }

    .sigla {
      position: relative;
      font-size: .82rem; font-weight: 700; letter-spacing: .12em;
      color: var(--oro-texto);
      user-select: none;
    }

    .texto { font-size: .88rem; color: var(--color-text-muted); }

    /* Oro propio del indicador. Se define aquí y no en los tokens globales
       porque es el único sitio del sistema que usa este color: la paleta de la
       aplicación es índigo, y el dorado aparece solo como marca de espera. */
    :host {
      --oro: #C9A227;
      --oro-tenue: rgba(201, 162, 39, .18);
      --oro-texto: #8A6D12;
    }
    /* Sobre fondo oscuro el dorado quemado se apaga: se sube a un ámbar claro
       que conserva contraste sin encenderse de más. */
    :host-context([data-theme="oscuro"]) {
      --oro: #E8C25A;
      --oro-tenue: rgba(232, 194, 90, .16);
      --oro-texto: #E8C25A;
    }

    @keyframes girar { to { transform: rotate(360deg); } }
    @keyframes aparecer { from { opacity: 0; } to { opacity: 1; } }

    /* Quien pidió menos movimiento no debería recibir un anillo girando sin
       parar: se sustituye por un latido suave, que sigue comunicando actividad
       sin rotación. */
    @media (prefers-reduced-motion: reduce) {
      .cargando { animation: none; }
      .anillo {
        animation: latir 1.8s ease-in-out infinite;
        border-color: var(--oro);
      }
      @keyframes latir { 0%, 100% { opacity: .3; } 50% { opacity: 1; } }
    }
  `],
})
export class CargandoComponent implements OnInit, OnDestroy {

  readonly mensaje = input('Cargando…');

  /**
   * Milisegundos antes de aparecer. Configurable porque no todas las esperas
   * son iguales: generar un PDF o pedir un comentario a la IA tarda siempre lo
   * suficiente como para mostrarlo de inmediato.
   */
  readonly retardoMs = input(250);

  readonly visible = signal(false);
  private temporizador?: ReturnType<typeof setTimeout>;

  ngOnInit(): void {
    if (this.retardoMs() <= 0) {
      this.visible.set(true);
      return;
    }
    this.temporizador = setTimeout(() => this.visible.set(true), this.retardoMs());
  }

  ngOnDestroy(): void {
    // Si la respuesta llegó antes del umbral, el componente se destruye con el
    // temporizador aún pendiente: sin esto quedaría vivo y escribiría sobre un
    // signal de un componente que ya no existe.
    clearTimeout(this.temporizador);
  }
}
