import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, input, signal } from '@angular/core';

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
    .anillo {
      position: absolute; inset: 0;
      border-radius: 50%;
      border: 3px solid transparent;
      border-top-color: var(--oro);
      border-right-color: var(--oro);
      animation: girar .95s cubic-bezier(.5, .1, .5, .9) infinite;
    }
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

    :host {
      --oro: #C9A227;
      --oro-tenue: rgba(201, 162, 39, .18);
      --oro-texto: #8A6D12;
    }
    :host-context([data-theme="oscuro"]) {
      --oro: #E8C25A;
      --oro-tenue: rgba(232, 194, 90, .16);
      --oro-texto: #E8C25A;
    }

    @keyframes girar { to { transform: rotate(360deg); } }
    @keyframes aparecer { from { opacity: 0; } to { opacity: 1; } }

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
    clearTimeout(this.temporizador);
  }
}
