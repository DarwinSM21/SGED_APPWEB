import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CargandoComponent } from '../../core/cargando.component';
import { CommonModule } from '@angular/common';
import { RepresentanteService } from './representante.service';
import { EstudianteResumen, InformeEstudiante, Notificacion } from './representante.models';
import { inicialesDe } from '../entrenador/plantilla.models';
import { fechaHoraCorta } from '../../core/formato-fecha';

/**
 * Portal del representante: lista de sus representados y, al elegir uno,
 * su informe de solo lectura (promedio historico por criterio + historial
 * de lesiones). El backend ya verifica que cada representado mostrado es
 * realmente suyo -esta pantalla no decide nada de eso, solo lo pinta-.
 */
@Component({
  selector: 'app-representante',
  standalone: true,
  imports: [CommonModule, CargandoComponent],
  template: `
    <div class="pantalla">
      <h1 class="titulo-pantalla">Mis representados</h1>

      @if (notificaciones().length > 0) {
        <div class="card panel-notificaciones">
          <div class="panel-notificaciones__cabecera">
            <h2>Notificaciones</h2>
            @if (noLeidas() > 0) {
              <span class="badge badge--warning">{{ noLeidas() }} sin leer</span>
            }
          </div>
          @for (n of notificaciones(); track n.idNotificacion) {
            <button type="button" class="notificacion-fila" [class.no-leida]="!n.leida" (click)="marcarLeida(n)">
              <span class="notificacion-icono" [class.notificacion-icono--lesion]="n.tipo === 'LESION'">
                @if (n.tipo === 'LESION') {
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 12h-4l-3 9L9 3l-3 9H2"></path></svg>
                } @else {
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
                }
              </span>
              <span class="notificacion-texto">
                <span class="notificacion-mensaje">{{ n.mensaje }}</span>
                <span class="notificacion-fecha">{{ fechaHora(n.creadaEn) }}</span>
              </span>
              @if (!n.leida) { <span class="punto-no-leida" title="Sin leer"></span> }
            </button>
          }
        </div>
      }

      @if (cargando()) {
        <app-cargando />
      } @else if (error()) {
        <p class="alert alert--danger">{{ error() }}</p>
      } @else if (representados().length === 0) {
        <div class="card vacio">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle></svg>
          <p>Todavía no tienes representados vinculados. Contacta a un administrador.</p>
        </div>
      } @else {
        <div class="cuerpo">
          <div class="lista-representados">
            @for (r of representados(); track r.idEstudiante) {
              <button type="button" class="tarjeta-representado" [class.activa]="idSeleccionado() === r.idEstudiante"
                      (click)="seleccionar(r.idEstudiante)">
                <span class="avatar">{{ iniciales(r.nombreCompleto) }}</span>
                <span class="tarjeta-representado__info">
                  <span class="nombre">{{ r.nombreCompleto }}</span>
                  <span class="categoria">{{ r.categoria }}</span>
                </span>
              </button>
            }
          </div>

          <div class="card detalle">
            @if (cargandoInforme()) {
              <app-cargando mensaje="Cargando informe…" />
            } @else if (informe(); as inf) {
              <h2>{{ inf.nombreCompleto }}</h2>
              <p class="categoria-detalle">{{ inf.categoria }}</p>

              <div class="asistencia-resumen">
                <span class="asistencia-etiqueta">Asistencia (últimos 30 días)</span>
                @if (inf.porcentajeAsistencia === null) {
                  <span class="badge badge--info">Sin sesiones en el rango</span>
                } @else {
                  <span class="badge" [class.badge--success]="inf.porcentajeAsistencia >= 75" [class.badge--warning]="inf.porcentajeAsistencia < 75">
                    {{ inf.porcentajeAsistencia | number: '1.0-0' }}%
                  </span>
                }
              </div>

              <h3>Promedio histórico por criterio</h3>
              @if (inf.promediosPorCriterio.length === 0) {
                <p class="aviso">Todavía no hay evaluaciones registradas.</p>
              } @else {
                <div class="criterios">
                  @for (p of inf.promediosPorCriterio; track p.criterio) {
                    <div class="criterio-fila">
                      <span>{{ p.criterio }}</span>
                      <span class="badge badge--info">{{ p.promedio | number: '1.1-1' }}</span>
                    </div>
                  }
                </div>
              }

              <h3>Historial de lesiones</h3>
              @if (inf.historialLesiones.length === 0) {
                <p class="aviso">Sin lesiones registradas.</p>
              } @else {
                @for (l of inf.historialLesiones; track l.idLesion) {
                  <div class="lesion-fila">
                    <span class="badge" [class.badge--danger]="l.activa" [class.badge--success]="!l.activa">
                      {{ l.activa ? 'Activa' : 'De alta' }}
                    </span>
                    <span class="lesion-descripcion">{{ l.descripcion }}</span>
                    <span class="lesion-fecha">{{ l.fechaLesion }}</span>
                  </div>
                }
              }
            } @else {
              <p class="aviso">Selecciona un representado para ver su informe.</p>
            }
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .pantalla { max-width: 900px; margin: 0 auto; padding: 1.5rem 1.25rem 3rem; }
    .titulo-pantalla { font-size: 1.2rem; margin-bottom: 1.1rem; }

    .aviso { color: var(--color-text-muted); font-size: .9rem; }

    .panel-notificaciones { padding: 1.1rem 1.25rem; margin-bottom: 1.1rem; max-height: 280px; overflow-y: auto; }
    .panel-notificaciones__cabecera { display: flex; align-items: center; justify-content: space-between; margin-bottom: .6rem; }
    .panel-notificaciones__cabecera h2 { font-size: .95rem; }
    .notificacion-fila {
      display: flex; align-items: center; gap: .7rem; width: 100%; padding: .6rem .5rem;
      border: none; border-top: 1px solid var(--color-border-light); background: none; cursor: pointer;
      text-align: left; font: inherit; color: inherit; transition: background var(--transition);
    }
    .notificacion-fila:first-of-type { border-top: none; }
    .notificacion-fila:hover { background: var(--color-neutral-bg); }
    .notificacion-fila.no-leida { background: var(--color-primary-50); }
    .notificacion-icono {
      width: 34px; height: 34px; border-radius: 50%; flex-shrink: 0;
      display: flex; align-items: center; justify-content: center;
      background: var(--color-success-bg); color: var(--color-success);
    }
    .notificacion-icono--lesion { background: var(--color-danger-bg); color: var(--color-danger); }
    .notificacion-icono svg { width: 17px; height: 17px; }
    .notificacion-texto { display: flex; flex-direction: column; flex: 1; min-width: 0; gap: .1rem; }
    .notificacion-mensaje { font-size: .87rem; }
    .notificacion-fecha { font-size: .75rem; color: var(--color-text-faint); }
    .punto-no-leida { width: 8px; height: 8px; border-radius: 50%; background: var(--color-primary-500); flex-shrink: 0; }

    .vacio { display: flex; flex-direction: column; align-items: center; gap: .65rem; text-align: center; color: var(--color-text-faint); padding: 2.5rem 1rem; }
    .vacio svg { width: 34px; height: 34px; opacity: .6; }
    .vacio p { font-size: .88rem; color: var(--color-text-muted); }

    .cuerpo { display: flex; gap: 1.1rem; flex-wrap: wrap; }

    .lista-representados { display: flex; flex-direction: column; gap: .5rem; flex: 1 1 220px; min-width: 220px; }
    .tarjeta-representado {
      display: flex; align-items: center; gap: .65rem; padding: .75rem .85rem;
      border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-surface);
      cursor: pointer; text-align: left; font: inherit; color: inherit;
      transition: border-color var(--transition), background var(--transition);
    }
    .tarjeta-representado:hover { border-color: var(--color-primary-200); }
    .tarjeta-representado.activa { border-color: var(--color-primary-500); background: var(--color-primary-50); }
    .tarjeta-representado__info { display: flex; flex-direction: column; min-width: 0; }
    .tarjeta-representado__info .nombre { font-weight: 600; font-size: .9rem; }
    .tarjeta-representado__info .categoria { font-size: .78rem; color: var(--color-text-muted); }

    .detalle { flex: 2 1 380px; min-width: 300px; padding: 1.5rem; }
    .detalle h2 { font-size: 1.1rem; margin-bottom: .2rem; }
    .categoria-detalle { color: var(--color-text-muted); font-size: .85rem; margin-bottom: 1.25rem; }
    .asistencia-resumen {
      display: flex; align-items: center; justify-content: space-between; gap: .75rem;
      padding: .7rem .85rem; border: 1px solid var(--color-border-light); border-radius: var(--radius-sm);
      margin-bottom: 1.1rem; background: var(--color-neutral-bg);
    }
    .asistencia-etiqueta { font-size: .85rem; color: var(--color-text-muted); }
    .detalle h3 { font-size: .88rem; color: var(--color-text-muted); margin: 1.1rem 0 .6rem; text-transform: uppercase; letter-spacing: .03em; }
    .detalle h3:first-of-type { margin-top: 0; }

    .criterios { display: flex; flex-direction: column; gap: .4rem; }
    .criterio-fila {
      display: flex; justify-content: space-between; align-items: center;
      padding: .55rem .7rem; border: 1px solid var(--color-border-light); border-radius: var(--radius-sm); font-size: .9rem;
    }

    .lesion-fila {
      display: flex; align-items: center; gap: .6rem; padding: .55rem .7rem;
      border: 1px solid var(--color-border-light); border-radius: var(--radius-sm); margin-bottom: .4rem; font-size: .85rem;
    }
    .lesion-descripcion { flex: 1; min-width: 0; }
    .lesion-fecha { color: var(--color-text-faint); font-size: .78rem; white-space: nowrap; }
  `]
})
export class RepresentanteComponent implements OnInit {

  /** 12 horas con AM/PM, igual que el resto de la app. */
  readonly fechaHora = fechaHoraCorta;


  private readonly servicio = inject(RepresentanteService);

  readonly representados = signal<EstudianteResumen[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  readonly idSeleccionado = signal<number | null>(null);
  readonly informe = signal<InformeEstudiante | null>(null);
  readonly cargandoInforme = signal(false);

  readonly notificaciones = signal<Notificacion[]>([]);
  readonly noLeidas = computed(() => this.notificaciones().filter((n) => !n.leida).length);

  ngOnInit(): void {
    this.servicio.misRepresentados().subscribe({
      next: (representados) => {
        this.representados.set(representados);
        this.cargando.set(false);
        if (representados.length > 0) {
          this.seleccionar(representados[0].idEstudiante);
        }
      },
      error: () => {
        this.cargando.set(false);
        this.error.set('No se pudo cargar la lista de representados.');
      },
    });

    this.servicio.misNotificaciones().subscribe({
      next: (notificaciones) => this.notificaciones.set(notificaciones),
      error: () => {},
    });
  }

  /** Marcar como leida es "al tocarla": no hace falta un boton aparte para algo tan liviano. */
  marcarLeida(n: Notificacion): void {
    if (n.leida) return;
    this.notificaciones.update((actuales) =>
      actuales.map((x) => (x.idNotificacion === n.idNotificacion ? { ...x, leida: true } : x)));
    this.servicio.marcarLeida(n.idNotificacion).subscribe({ error: () => {} });
  }

  seleccionar(idEstudiante: number): void {
    this.idSeleccionado.set(idEstudiante);
    this.cargandoInforme.set(true);
    this.informe.set(null);

    this.servicio.informeDe(idEstudiante).subscribe({
      next: (informe) => {
        this.informe.set(informe);
        this.cargandoInforme.set(false);
      },
      error: () => {
        this.cargandoInforme.set(false);
      },
    });
  }

  iniciales(nombre: string): string {
    return inicialesDe(nombre);
  }
}
