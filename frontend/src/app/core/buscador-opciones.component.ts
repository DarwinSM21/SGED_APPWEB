import { Component, ElementRef, computed, input, output, signal, viewChild } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface OpcionBuscable {
  id: number;
  titulo: string;
  subtitulo?: string;
}

function normalizar(texto: string): string {
  return texto.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
}

const MAXIMO_VISIBLE = 50;

@Component({
  selector: 'app-buscador-opciones',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="buscador" (focusout)="alSalir($event)">
      <label class="field" [for]="idInput">
        <span class="field__label">{{ etiqueta() }}</span>
        <span class="field__control">
          <svg class="field__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
          <input
            #campo
            [id]="idInput"
            type="text"
            role="combobox"
            autocomplete="off"
            [attr.aria-expanded]="abierto()"
            [attr.aria-controls]="idLista"
            [attr.aria-activedescendant]="abierto() && activa() >= 0 ? idOpcion(activa()) : null"
            [placeholder]="marcador()"
            [value]="abierto() ? texto() : (textoSeleccionado() ?? '')"
            [disabled]="cargando()"
            (input)="alEscribir($event)"
            (focus)="abrir()"
            (keydown)="alTeclear($event)" />
          @if (texto() || textoSeleccionado()) {
            <button type="button" class="field__toggle" aria-label="Limpiar selección"
                    (click)="limpiar()">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                   stroke-linecap="round" stroke-linejoin="round">
                <line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
            </button>
          }
        </span>
      </label>

      @if (abierto()) {
        <!-- mousedown se cancela para que el foco no salga del input antes de
             que llegue el click: si saliera, la lista se cerraria y el click
             caeria en el vacio. -->
        <ul class="lista" [id]="idLista" role="listbox" (mousedown)="$event.preventDefault()">
          @if (filtradas().length === 0) {
            <li class="lista__vacio">Sin resultados para “{{ texto() }}”</li>
          } @else {
            @for (o of filtradas(); track o.id; let i = $index) {
              <li class="lista__opcion"
                  [id]="idOpcion(i)"
                  role="option"
                  [attr.aria-selected]="i === activa()"
                  [class.lista__opcion--activa]="i === activa()"
                  (click)="elegir(o)"
                  (pointerenter)="activa.set(i)">
                <span class="lista__titulo">{{ o.titulo }}</span>
                @if (o.subtitulo) { <span class="lista__subtitulo">{{ o.subtitulo }}</span> }
              </li>
            }
            @if (ocultas() > 0) {
              <li class="lista__pie">y {{ ocultas() }} más — escribe para afinar</li>
            }
          }
        </ul>
      }
    </div>
  `,
  styles: [`
    .buscador { position: relative; }
    .field { margin-bottom: 0; }
    .lista {
      position: absolute; top: calc(100% + 4px); left: 0; right: 0; z-index: 30;
      margin: 0; padding: .3rem; list-style: none;
      max-height: 264px; overflow-y: auto;
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-sm);
      box-shadow: var(--shadow-md);
    }
    .lista__opcion {
      display: flex; align-items: baseline; gap: .5rem;
      padding: .5rem .6rem; border-radius: 6px; cursor: pointer;
    }
    .lista__opcion--activa { background: var(--color-primary-50); }
    .lista__titulo { font-size: .9rem; color: var(--color-text); }
    .lista__subtitulo { font-size: .76rem; color: var(--color-text-muted); margin-left: auto; }
    .lista__vacio, .lista__pie {
      padding: .55rem .6rem; font-size: .8rem; color: var(--color-text-muted);
    }
    .lista__pie { border-top: 1px solid var(--color-border-light); margin-top: .2rem; }

    :host-context([data-theme="oscuro"]) .lista__opcion--activa { background: #312e81; }
  `],
})
export class BuscadorOpcionesComponent {
  readonly opciones = input.required<OpcionBuscable[]>();
  readonly etiqueta = input('Buscar');
  readonly marcador = input('Escribe para buscar…');
  readonly cargando = input(false);
  readonly textoSeleccionado = input<string | null>(null);

  readonly seleccionada = output<OpcionBuscable>();
  readonly limpiada = output<void>();

  private readonly campo = viewChild<ElementRef<HTMLInputElement>>('campo');

  readonly texto = signal('');
  readonly abierto = signal(false);
  readonly activa = signal(0);

  private readonly sufijo = Math.random().toString(36).slice(2, 8);
  readonly idInput = 'buscador-' + this.sufijo;
  readonly idLista = 'lista-' + this.sufijo;

  idOpcion(i: number): string {
    return 'opcion-' + this.sufijo + '-' + i;
  }

  private readonly coincidentes = computed(() => {
    const consulta = normalizar(this.texto().trim());
    if (!consulta) return this.opciones();
    return this.opciones().filter((o) =>
      normalizar(o.titulo + ' ' + (o.subtitulo ?? '')).includes(consulta));
  });

  readonly filtradas = computed(() => this.coincidentes().slice(0, MAXIMO_VISIBLE));
  readonly ocultas = computed(() => Math.max(0, this.coincidentes().length - MAXIMO_VISIBLE));

  abrir(): void {
    this.texto.set('');
    this.abierto.set(true);
    this.activa.set(0);
  }

  alEscribir(evento: Event): void {
    this.texto.set((evento.target as HTMLInputElement).value);
    this.abierto.set(true);

    this.activa.set(0);
  }

  alTeclear(evento: KeyboardEvent): void {
    const total = this.filtradas().length;

    if (evento.key === 'Escape') {
      this.abierto.set(false);
      return;
    }
    if (evento.key === 'ArrowDown' || evento.key === 'ArrowUp') {
      evento.preventDefault();
      if (!this.abierto()) { this.abrir(); return; }
      if (total === 0) return;
      const paso = evento.key === 'ArrowDown' ? 1 : -1;
      this.activa.set((this.activa() + paso + total) % total);
      return;
    }
    if (evento.key === 'Enter') {
      const elegida = this.filtradas()[this.activa()];
      if (this.abierto() && elegida) {
        evento.preventDefault();
        this.elegir(elegida);
      }
    }
  }

  elegir(opcion: OpcionBuscable): void {
    this.abierto.set(false);
    this.texto.set('');
    this.seleccionada.emit(opcion);
  }

  limpiar(): void {
    this.texto.set('');
    this.limpiada.emit();
    this.abierto.set(true);
    this.campo()?.nativeElement.focus();
  }

  alSalir(evento: FocusEvent): void {
    const destino = evento.relatedTarget as Node | null;
    const raiz = (evento.currentTarget as HTMLElement);
    if (!destino || !raiz.contains(destino)) this.abierto.set(false);
  }
}
