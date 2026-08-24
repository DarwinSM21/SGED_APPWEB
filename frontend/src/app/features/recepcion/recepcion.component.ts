import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { mensajeDeError } from '../../core/mensaje-error';
import { CargandoComponent } from '../../core/cargando.component';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { toDataURL } from 'qrcode';
import { RecepcionService } from './recepcion.service';
import { SesionHoy } from '../dashboard/dashboard.models';
import { horaCorta } from '../entrenador/plantilla.models';

/**
 * Pantalla de recepcion: elegir una sesion de hoy y mostrar su QR de
 * asistencia. El QR se genera 100% en el cliente (librer­ia `qrcode`,
 * sin llamada a ningun servicio externo de imagenes): el token que
 * codifica es una credencial vigente que marca asistencia, mandarlo a
 * un tercero para "dibujarlo" seria filtrar esa credencial.
 *
 * El token rota: se vuelve a pedir uno nuevo un poco antes de que el
 * anterior expire (80% de expiraEnSegundos), asi el codigo en pantalla
 * nunca queda invalido mientras alguien intenta escanearlo.
 */
@Component({
  selector: 'app-recepcion',
  standalone: true,
  imports: [CommonModule, FormsModule, CargandoComponent],
  template: `
    <div class="pantalla">
      <h1 class="titulo-pantalla">Recepción — Asistencia por QR</h1>

      @if (cargandoSesiones()) {
        <app-cargando mensaje="Cargando sesiones de hoy…" />
      } @else if (sesiones().length === 0) {
        <div class="card vacio">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
          <p>No hay entrenamientos programados para hoy.</p>
        </div>
      } @else {
        <div class="card selector">
          <span class="selector__etiqueta">Sesión</span>
          <select [ngModel]="idSesionSeleccionada()" (ngModelChange)="seleccionar($event)" name="sesion">
            @for (s of sesiones(); track s.idSesion) {
              <option [value]="s.idSesion">
                {{ s.categoria }} · {{ s.entrenador }}{{ s.horaInicio ? ' · ' + horaCorta(s.horaInicio) : '' }}
              </option>
            }
          </select>
        </div>

        @if (error()) {
          <div class="alert alert--danger">{{ error() }}</div>
        }

        @if (qrDataUrl(); as qr) {
          <div class="card qr-tarjeta">
            <img [src]="qr" alt="Código QR de asistencia" class="qr-imagen" />
            <p class="qr-vigencia">Se renueva automáticamente · válido {{ segundosRestantes() }}s</p>
          </div>
        } @else if (cargandoToken()) {
          <p class="aviso">Generando código…</p>
        }
      }
    </div>
  `,
  styles: [`
    .pantalla { max-width: 480px; margin: 0 auto; padding: 1.5rem 1.25rem 3rem; display: flex; flex-direction: column; gap: 1rem; }
    .titulo-pantalla { font-size: 1.2rem; }

    .aviso { color: var(--color-text-muted); font-size: .9rem; }

    .vacio { display: flex; flex-direction: column; align-items: center; gap: .65rem; text-align: center; color: var(--color-text-faint); padding: 2.5rem 1rem; }
    .vacio svg { width: 34px; height: 34px; opacity: .6; }
    .vacio p { font-size: .88rem; color: var(--color-text-muted); }

    .selector { padding: 1rem 1.1rem; display: flex; flex-direction: column; gap: .4rem; }
    .selector__etiqueta { font-size: .8rem; font-weight: 600; color: var(--color-text-muted); }
    .selector select {
      border: 1.5px solid var(--color-border); border-radius: var(--radius-sm);
      padding: .65rem .7rem; font-size: .92rem; background: var(--color-surface); color: var(--color-text);
    }

    .qr-tarjeta { padding: 1.75rem; display: flex; flex-direction: column; align-items: center; gap: 1rem; }
    .qr-imagen { width: 100%; max-width: 280px; height: auto; border-radius: var(--radius-sm); }
    .qr-vigencia { font-size: .8rem; color: var(--color-text-muted); }
  `]
})
export class RecepcionComponent implements OnInit, OnDestroy {

  private readonly servicio = inject(RecepcionService);

  readonly horaCorta = horaCorta;

  readonly sesiones = signal<SesionHoy[]>([]);
  readonly cargandoSesiones = signal(true);
  readonly idSesionSeleccionada = signal<number | null>(null);
  readonly cargandoToken = signal(false);
  readonly qrDataUrl = signal<string | null>(null);
  readonly segundosRestantes = signal(0);
  readonly error = signal<string | null>(null);

  private intervaloRenovacion?: ReturnType<typeof setInterval>;
  private intervaloCuentaRegresiva?: ReturnType<typeof setInterval>;

  ngOnInit(): void {
    this.servicio.sesionesDeHoy().subscribe({
      next: (sesiones) => {
        this.sesiones.set(sesiones);
        this.cargandoSesiones.set(false);
        if (sesiones.length > 0) {
          this.seleccionar(sesiones[0].idSesion);
        }
      },
      error: (e) => {
        this.cargandoSesiones.set(false);
        this.error.set(mensajeDeError(e, 'No se pudieron cargar las sesiones de hoy.'));
      },
    });
  }

  ngOnDestroy(): void {
    this.detenerRenovacion();
  }

  seleccionar(idSesion: number): void {
    this.idSesionSeleccionada.set(idSesion);
    this.detenerRenovacion();
    this.pedirYPintarToken(idSesion);
  }

  private pedirYPintarToken(idSesion: number): void {
    this.cargandoToken.set(true);
    this.error.set(null);

    this.servicio.emitirToken(idSesion).subscribe({
      next: async ({ token, expiraEnSegundos }) => {
        this.cargandoToken.set(false);
        this.qrDataUrl.set(await toDataURL(token, { margin: 1, width: 320 }));
        this.iniciarCuentaRegresiva(expiraEnSegundos);

        const renovarEnMs = Math.max(expiraEnSegundos * 0.8, 5) * 1000;
        this.intervaloRenovacion = setTimeout(() => this.pedirYPintarToken(idSesion), renovarEnMs);
      },
      error: (e) => {
        this.cargandoToken.set(false);
        this.qrDataUrl.set(null);
        this.error.set(mensajeDeError(e, 'No se pudo generar el código. Intenta de nuevo.'));
      },
    });
  }

  private iniciarCuentaRegresiva(segundos: number): void {
    clearInterval(this.intervaloCuentaRegresiva);
    this.segundosRestantes.set(Math.round(segundos));
    this.intervaloCuentaRegresiva = setInterval(() => {
      this.segundosRestantes.update((s) => Math.max(s - 1, 0));
    }, 1000);
  }

  private detenerRenovacion(): void {
    clearTimeout(this.intervaloRenovacion);
    clearInterval(this.intervaloCuentaRegresiva);
  }
}
