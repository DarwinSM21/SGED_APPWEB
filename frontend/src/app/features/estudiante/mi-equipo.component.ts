import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MiEquipoService } from './mi-equipo.service';
import { MiEquipo } from './mi-equipo.models';
import { InformeEstudiante } from '../representante/representante.models';
import { inicialesDe } from '../entrenador/plantilla.models';

/**
 * "Mi equipo": lo que el estudiante ve sobre si mismo mas alla de su
 * asistencia (esa vive en mi-historial) -su categoria, su posicion, quien
 * es su entrenador y sus companeros- mas sus estadisticas de evaluacion,
 * con el mismo layout que ya usa el representante para el informe de un
 * representado (misma forma de datos, misma pantalla validada).
 *
 * Companeros muestra deliberadamente solo nombre y posicion: son menores
 * de edad, sin datos de contacto ni promedios entre compañeros.
 */
@Component({
  selector: 'app-mi-equipo',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="pantalla">
      <h1 class="titulo-pantalla">Mi equipo</h1>

      @if (cargando()) {
        <p class="aviso">Cargando…</p>
      } @else if (error()) {
        <p class="alert alert--danger">{{ error() }}</p>
      } @else {
        @if (equipo(); as eq) {
          <div class="tarjetas-resumen">
            <div class="card tarjeta-resumen">
              <span class="tarjeta-resumen__etiqueta">Mi categoría</span>
              <span class="tarjeta-resumen__valor">{{ eq.categoria.nombre }}</span>
              @if (eq.categoria.edadMin !== null && eq.categoria.edadMax !== null) {
                <span class="tarjeta-resumen__detalle">{{ eq.categoria.edadMin }}–{{ eq.categoria.edadMax }} años</span>
              }
              @if (eq.categoria.descripcion) {
                <span class="tarjeta-resumen__detalle">{{ eq.categoria.descripcion }}</span>
              }
            </div>

            <div class="card tarjeta-resumen">
              <span class="tarjeta-resumen__etiqueta">Mi posición</span>
              @if (eq.posicion) {
                <span class="tarjeta-resumen__valor">{{ eq.posicion.nombre }}</span>
              } @else {
                <span class="tarjeta-resumen__vacio">Sin posición asignada todavía</span>
              }
            </div>

            <div class="card tarjeta-resumen">
              <span class="tarjeta-resumen__etiqueta">Mi entrenador</span>
              @if (eq.entrenador) {
                <span class="tarjeta-resumen__valor">{{ eq.entrenador.nombre }}</span>
                @if (eq.entrenador.especialidad) {
                  <span class="tarjeta-resumen__detalle">{{ eq.entrenador.especialidad }}</span>
                }
              } @else {
                <span class="tarjeta-resumen__vacio">Sin sesiones programadas todavía</span>
              }
            </div>
          </div>
        }

        @if (informe(); as inf) {
          <div class="card detalle">
            <h2>Mis estadísticas</h2>

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
          </div>
        }

        <div class="card companeros">
          <h2>Compañeros de equipo</h2>
          @if (equipo(); as eq) {
            @if (eq.companeros.length === 0) {
              <p class="aviso">Todavía no hay otros compañeros en tu categoría.</p>
            } @else {
              @for (c of eq.companeros; track c.idEstudiante) {
                <div class="companero-fila">
                  <span class="avatar">{{ iniciales(c.nombre) }}</span>
                  <span class="companero-info">
                    <span class="nombre">{{ c.nombre }}</span>
                    @if (c.posicion) { <span class="posicion">{{ c.posicion }}</span> }
                  </span>
                </div>
              }
            }
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .pantalla { max-width: 760px; margin: 0 auto; padding: 1.5rem 1.25rem 3rem; }
    .titulo-pantalla { font-size: 1.2rem; margin-bottom: 1.1rem; }
    .aviso { color: var(--color-text-muted); font-size: .9rem; }

    .tarjetas-resumen {
      display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: .9rem; margin-bottom: 1.1rem;
    }
    .tarjeta-resumen { padding: 1rem 1.1rem; display: flex; flex-direction: column; gap: .2rem; }
    .tarjeta-resumen__etiqueta { font-size: .76rem; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: .03em; }
    .tarjeta-resumen__valor { font-size: 1.05rem; font-weight: 700; margin-top: .15rem; }
    .tarjeta-resumen__detalle { font-size: .8rem; color: var(--color-text-muted); }
    .tarjeta-resumen__vacio { font-size: .85rem; color: var(--color-text-faint); margin-top: .15rem; }

    .detalle { padding: 1.5rem; margin-bottom: 1.1rem; }
    .detalle h2 { font-size: 1.05rem; margin-bottom: 1rem; }
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

    .companeros { padding: 1.5rem; }
    .companeros h2 { font-size: 1.05rem; margin-bottom: 1rem; }
    .companero-fila { display: flex; align-items: center; gap: .7rem; padding: .6rem 0; border-top: 1px solid var(--color-border-light); }
    .companero-fila:first-of-type { border-top: none; }
    .companero-info { display: flex; flex-direction: column; min-width: 0; }
    .companero-info .nombre { font-weight: 600; font-size: .9rem; }
    .companero-info .posicion { font-size: .78rem; color: var(--color-text-muted); }
  `]
})
export class MiEquipoComponent implements OnInit {

  private readonly servicio = inject(MiEquipoService);

  readonly equipo = signal<MiEquipo | null>(null);
  readonly informe = signal<InformeEstudiante | null>(null);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.servicio.miEquipo().subscribe({
      next: (eq) => { this.equipo.set(eq); this.cargando.set(false); },
      error: () => {
        this.cargando.set(false);
        this.error.set('No se pudo cargar la información de tu equipo.');
      },
    });

    this.servicio.miInforme().subscribe({
      next: (inf) => this.informe.set(inf),
      error: () => {},
    });
  }

  iniciales(nombre: string): string {
    return inicialesDe(nombre);
  }
}
