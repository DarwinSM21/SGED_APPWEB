import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { SesionesService } from './sesiones.service';
import { CategoriaOpcion, Sesion } from './sesiones.models';
import { inicialesDe } from './plantilla.models';

/**
 * Historial completo de sesiones del entrenador autenticado (pasadas y
 * futuras), con alta de una nueva. Antes de esto no existia ningun punto de
 * entrada para crear una sesion -el controlador original era deliberadamente
 * de solo lectura- ni para ver ninguna que no fuera la de hoy: un dia sin
 * sesion programada dejaba al entrenador sin forma de llegar a Evaluacion
 * Diaria o Plantilla desde la interfaz.
 */
@Component({
  selector: 'app-sesiones',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="pantalla">
      <div class="cabecera-pantalla">
        <h1 class="titulo-pantalla">Mis sesiones</h1>
        <button class="btn btn--primary" type="button" (click)="alternarFormulario()">
          {{ mostrarFormulario() ? 'Cancelar' : '+ Nueva sesión' }}
        </button>
      </div>

      @if (mostrarFormulario()) {
        <form class="card formulario" (ngSubmit)="onCrear()">
          <div class="fila-2">
            <label class="field" for="categoria">
              <span class="field__label">Categoría</span>
              <span class="field__control">
                <select id="categoria" [ngModel]="idCategoria" (ngModelChange)="idCategoria = $event" name="categoria" required>
                  <option [ngValue]="null" disabled>Selecciona...</option>
                  @for (c of categorias(); track c.idCategoria) {
                    <option [ngValue]="c.idCategoria">{{ c.nombre }}</option>
                  }
                </select>
              </span>
            </label>
            <label class="field" for="fecha">
              <span class="field__label">Fecha</span>
              <span class="field__control">
                <input id="fecha" type="date" [(ngModel)]="fecha" name="fecha" required />
              </span>
            </label>
          </div>

          <div class="fila-2">
            <label class="field" for="horaInicio">
              <span class="field__label">Hora de inicio</span>
              <span class="field__control">
                <input id="horaInicio" type="time" [(ngModel)]="horaInicio" name="horaInicio" required />
              </span>
            </label>
            <label class="field" for="horaFin">
              <span class="field__label">Hora de fin</span>
              <span class="field__control">
                <input id="horaFin" type="time" [(ngModel)]="horaFin" name="horaFin" required />
              </span>
            </label>
          </div>

          <label class="field" for="campo">
            <span class="field__label">Campo / cancha (opcional)</span>
            <span class="field__control">
              <input id="campo" type="text" [(ngModel)]="campo" name="campo" />
            </span>
          </label>

          @if (error()) {
            <div class="alert alert--danger" role="alert">{{ error() }}</div>
          }

          <button class="btn btn--primary btn--block" type="submit" [disabled]="guardando()">
            {{ guardando() ? 'Guardando…' : 'Crear sesión' }}
          </button>
        </form>
      }

      <section class="card lista">
        @if (cargando()) {
          <p class="aviso">Cargando…</p>
        } @else if (sesiones().length === 0) {
          <div class="vacio">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
            <p>Todavía no tienes sesiones registradas. Crea la primera con el botón de arriba.</p>
          </div>
        } @else {
          @for (s of sesiones(); track s.idSesion) {
            <a class="sesion" [routerLink]="['/entrenador/sesion', s.idSesion]">
              <span class="avatar avatar--muted">{{ iniciales(s.categoria) }}</span>
              <div class="sesion-info">
                <span class="categoria">{{ s.categoria }} · {{ s.fecha }}</span>
                <span class="detalle">
                  @if (s.horaInicio) { {{ s.horaInicio }} }
                  @if (s.campo) { · {{ s.campo }} }
                </span>
              </div>
              <span class="badge" [class.badge--warning]="s.tieneEvaluacion" [class.badge--info]="!s.tieneEvaluacion">
                {{ s.tieneEvaluacion ? 'En evaluación' : 'Sin iniciar' }}
              </span>
              <svg class="chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"></polyline></svg>
            </a>
          }
        }
      </section>
    </div>
  `,
  styles: [`
    .pantalla { max-width: 880px; margin: 0 auto; padding: 1.5rem 1.25rem 3rem; }
    .cabecera-pantalla { display: flex; align-items: center; justify-content: space-between; gap: 1rem; margin-bottom: 1.25rem; }
    .titulo-pantalla { font-size: 1.3rem; }

    .formulario { padding: 1.25rem; display: flex; flex-direction: column; gap: 1rem; margin-bottom: 1.5rem; }
    .fila-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
    @media (max-width: 560px) { .fila-2 { grid-template-columns: 1fr; } }

    .lista { padding: 1.25rem; }
    .aviso { color: var(--color-text-muted); font-size: .9rem; padding: .5rem 0; }
    .vacio {
      display: flex; flex-direction: column; align-items: center; gap: .75rem;
      color: var(--color-text-faint); text-align: center; padding: 2rem 1rem;
    }
    .vacio svg { width: 36px; height: 36px; opacity: .6; }
    .vacio p { font-size: .88rem; color: var(--color-text-muted); max-width: 32ch; }

    .sesion {
      display: flex; align-items: center; gap: .8rem;
      padding: .8rem .9rem; border: 1px solid var(--color-border-light); border-radius: var(--radius-sm);
      margin-bottom: .5rem; text-decoration: none; color: inherit;
      transition: background var(--transition), border-color var(--transition);
    }
    .sesion:last-child { margin-bottom: 0; }
    .sesion:hover { background: var(--color-primary-50); border-color: var(--color-primary-100); }
    .sesion-info { display: flex; flex-direction: column; flex: 1; min-width: 0; }
    .categoria { font-weight: 600; font-size: .92rem; }
    .detalle { font-size: .78rem; color: var(--color-text-muted); }
    .chevron { width: 18px; height: 18px; color: var(--color-text-faint); flex-shrink: 0; }
  `]
})
export class SesionesComponent implements OnInit {
  private readonly sesionesService = inject(SesionesService);

  readonly sesiones = signal<Sesion[]>([]);
  readonly categorias = signal<CategoriaOpcion[]>([]);
  readonly cargando = signal(false);
  readonly guardando = signal(false);
  readonly error = signal('');
  readonly mostrarFormulario = signal(false);

  /** Propiedades planas, no signals: [(ngModel)] las actualiza via su propio manejador de evento. */
  idCategoria: number | null = null;
  fecha = '';
  horaInicio = '';
  horaFin = '';
  campo = '';

  ngOnInit(): void {
    this.cargarSesiones();
    this.sesionesService.listarCategoriasActivas().subscribe({
      next: (categorias) => this.categorias.set(categorias),
      error: () => {},
    });
  }

  alternarFormulario(): void {
    this.mostrarFormulario.set(!this.mostrarFormulario());
    this.error.set('');
  }

  onCrear(): void {
    if (!this.idCategoria || !this.fecha || !this.horaInicio || !this.horaFin) {
      this.error.set('Completa categoría, fecha y horas.');
      return;
    }
    if (this.horaFin <= this.horaInicio) {
      this.error.set('La hora de fin debe ser posterior a la de inicio.');
      return;
    }

    this.guardando.set(true);
    this.error.set('');
    this.sesionesService.crear({
      idCategoria: this.idCategoria,
      fecha: this.fecha,
      horaInicio: this.horaInicio,
      horaFin: this.horaFin,
      campo: this.campo || null,
    }).subscribe({
      next: () => {
        this.guardando.set(false);
        this.mostrarFormulario.set(false);
        this.idCategoria = null;
        this.fecha = '';
        this.horaInicio = '';
        this.horaFin = '';
        this.campo = '';
        this.cargarSesiones();
      },
      error: (err) => {
        this.guardando.set(false);
        this.error.set(err?.error?.detail ?? 'No se pudo crear la sesión.');
      },
    });
  }

  private cargarSesiones(): void {
    this.cargando.set(true);
    this.sesionesService.listarMias().subscribe({
      next: (sesiones) => {
        this.sesiones.set(sesiones);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }

  iniciales(nombre: string): string {
    return inicialesDe(nombre);
  }
}
