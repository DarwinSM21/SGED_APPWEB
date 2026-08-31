import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';

@Component({
  selector: 'app-confirmar-accion',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (abierto()) {
      <span class="confirmar" role="group" [attr.aria-label]="pregunta()">
        <span class="confirmar__pregunta">{{ pregunta() }}</span>
        <button [class]="claseConfirmar()" type="button" [disabled]="ocupado()"
                (click)="aceptar()">
          @if (ocupado()) {
            <span class="spinner"></span> {{ enCurso() }}
          } @else {
            {{ textoConfirmar() }}
          }
        </button>
        <button class="btn btn--ghost btn--sm" type="button" [disabled]="ocupado()"
                (click)="cancelar()">No</button>
      </span>
    } @else {
      <button [class]="claseDisparador()" type="button" [disabled]="ocupado()"
              (click)="abrir()">{{ etiqueta() }}</button>
    }
  `,
  styles: [`
    :host { display: inline-flex; }
    .confirmar {
      display: inline-flex; align-items: center; gap: .4rem; flex-wrap: wrap;
    }
    .confirmar__pregunta {
      font-size: .82rem; color: var(--color-text-muted);
    }
  `],
})
export class ConfirmarAccionComponent {
  readonly etiqueta = input.required<string>();

  readonly pregunta = input('¿Seguro?');

  readonly textoConfirmar = input('Sí');

  readonly enCurso = input('Un momento…');

  readonly ocupado = input(false);

  readonly peligrosa = input(true);

  readonly confirmado = output<void>();

  readonly abierto = signal(false);

  readonly claseConfirmar = computed(() =>
    this.peligrosa() ? 'btn btn--danger btn--sm' : 'btn btn--primary btn--sm');

  readonly claseDisparador = computed(() => 'btn btn--ghost btn--sm');

  abrir(): void {
    this.abierto.set(true);
  }

  cancelar(): void {
    this.abierto.set(false);
  }

  aceptar(): void {
    this.abierto.set(false);
    this.confirmado.emit();
  }
}
