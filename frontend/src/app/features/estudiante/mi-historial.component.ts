import { Component, OnInit, inject, signal } from '@angular/core';
import { mensajeDeError } from '../../core/mensaje-error';
import { CargandoComponent } from '../../core/cargando.component';
import { CommonModule } from '@angular/common';
import { MiHistorialService } from './mi-historial.service';
import { MiHistorial } from './mi-historial.models';
import { horaCorta } from '../entrenador/plantilla.models';

/**
 * Historial propio del ESTUDIANTE autenticado: antes solo podia marcar
 * asistencia (jsQR), no consultar lo que ya habia marcado.
 */
@Component({
  selector: 'app-mi-historial',
  standalone: true,
  imports: [CommonModule, CargandoComponent],
  template: `
    <div class="pantalla">
      <h1 class="titulo-pantalla">Mi historial de asistencia</h1>

      @if (cargando()) {
        <app-cargando />
      } @else if (error()) {
        <p class="alert alert--danger">{{ error() }}</p>
      } @else if (historial(); as h) {
        <div class="card resumen">
          <span class="resumen__etiqueta">Asistencia últimos 30 días</span>
          <strong class="resumen__porcentaje">
            {{ h.porcentajeUltimos30Dias === null ? 'Sin datos todavía' : redondear(h.porcentajeUltimos30Dias) + '%' }}
          </strong>
        </div>

        <div class="card lista">
          @if (h.asistencias.length === 0) {
            <div class="vacio">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
              <p>Todavía no tienes asistencias registradas. Escanea el QR de recepción para marcar la primera.</p>
            </div>
          } @else {
            @for (a of h.asistencias; track a.idAsistencia) {
              <div class="fila">
                <span class="fila__icono" [class.fila__icono--tarde]="a.estado === 'TARDE'">
                  @if (a.estado === 'PRESENTE') {
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
                  } @else {
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
                  }
                </span>
                <div class="fila__info">
                  <span class="fila__categoria">{{ a.categoria }}</span>
                  <span class="fila__fecha">{{ a.fecha }}{{ a.horaEntrada ? ' · ' + horaCorta(a.horaEntrada) : '' }}</span>
                </div>
                <span class="badge" [class.badge--success]="a.estado === 'PRESENTE'" [class.badge--warning]="a.estado === 'TARDE'">
                  {{ a.estado === 'PRESENTE' ? 'Presente' : 'Tarde' }}
                </span>
              </div>
            }
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .pantalla { max-width: 560px; margin: 0 auto; padding: 1.5rem 1.25rem 3rem; display: flex; flex-direction: column; gap: 1.25rem; }
    .titulo-pantalla { font-size: 1.2rem; }
    .aviso { color: var(--color-text-muted); font-size: .9rem; }

    .resumen { padding: 1.1rem 1.3rem; display: flex; flex-direction: column; gap: .2rem; }
    .resumen__etiqueta { font-size: .78rem; color: var(--color-text-muted); }
    .resumen__porcentaje { font-size: 1.6rem; color: var(--color-text); }

    .lista { padding: .5rem 1.25rem; }
    .vacio { display: flex; flex-direction: column; align-items: center; gap: .75rem; text-align: center; padding: 2rem 1rem; }
    .vacio svg { width: 34px; height: 34px; color: var(--color-text-faint); }
    .vacio p { font-size: .88rem; color: var(--color-text-muted); max-width: 32ch; }

    .fila { display: flex; align-items: center; gap: .75rem; padding: .85rem 0; border-bottom: 1px solid var(--color-border-light); }
    .fila:last-child { border-bottom: none; }
    .fila__icono {
      width: 36px; height: 36px; border-radius: 50%; background: var(--color-success-bg); color: var(--color-success);
      display: flex; align-items: center; justify-content: center; flex-shrink: 0;
    }
    .fila__icono svg { width: 18px; height: 18px; }
    .fila__icono--tarde { background: var(--color-warning-bg); color: var(--color-warning); }
    .fila__info { display: flex; flex-direction: column; flex: 1; min-width: 0; }
    .fila__categoria { font-weight: 600; font-size: .9rem; }
    .fila__fecha { font-size: .78rem; color: var(--color-text-muted); }
  `]
})
export class MiHistorialComponent implements OnInit {
  readonly horaCorta = horaCorta;

  private readonly servicio = inject(MiHistorialService);

  readonly historial = signal<MiHistorial | null>(null);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.servicio.miHistorial().subscribe({
      next: (h) => { this.historial.set(h); this.cargando.set(false); },
      error: (e) => {
        this.cargando.set(false);
        this.error.set(mensajeDeError(e, 'No se pudo cargar tu historial de asistencia. Contacta a un administrador si el problema continúa.'));
      },
    });
  }

  redondear(valor: number): number {
    return Math.round(valor);
  }
}
