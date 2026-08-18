import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { SesionesService } from './sesiones.service';
import { CategoriaOpcion, DIAS_SEMANA, Horario, Sesion } from './sesiones.models';
import { horaCorta, inicialesDe } from './plantilla.models';
import { mensajeDeError } from '../../core/mensaje-error';

/** Fecha local de hoy en formato "yyyy-MM-dd", la que espera un <input type="date">. */
function fechaHoyIso(): string {
  const hoy = new Date();
  const mes = String(hoy.getMonth() + 1).padStart(2, '0');
  const dia = String(hoy.getDate()).padStart(2, '0');
  return `${hoy.getFullYear()}-${mes}-${dia}`;
}

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

      <section class="card horario-semanal">
        <div class="cabecera-seccion">
          <h2 class="subtitulo">Mi horario semanal</h2>
          <button class="btn btn--secondary btn--sm" type="button" (click)="alternarFormularioHorario()">
            {{ mostrarFormularioHorario() ? 'Cancelar' : '+ Agregar horario' }}
          </button>
        </div>
        <p class="ayuda">Las sesiones de estos días se generan solas. Usa "+ Nueva sesión" solo para una jornada extra.</p>

        @if (mostrarFormularioHorario()) {
          <form class="formulario-horario" (ngSubmit)="onCrearHorario()">
            <div class="fila-2">
              <label class="field" for="categoriaHorario">
                <span class="field__label">Categoría</span>
                <span class="field__control">
                  <select id="categoriaHorario" [ngModel]="idCategoriaHorario" (ngModelChange)="idCategoriaHorario = $event" name="categoriaHorario" required>
                    <option [ngValue]="null" disabled>Selecciona...</option>
                    @for (c of categorias(); track c.idCategoria) {
                      <option [ngValue]="c.idCategoria">{{ c.nombre }}</option>
                    }
                  </select>
                </span>
              </label>
              <label class="field" for="diaSemana">
                <span class="field__label">Día</span>
                <span class="field__control">
                  <select id="diaSemana" [ngModel]="diaSemana" (ngModelChange)="diaSemana = $event" name="diaSemana" required>
                    <option [ngValue]="null" disabled>Selecciona...</option>
                    @for (d of diasSemana; track d.valor) {
                      <option [ngValue]="d.valor">{{ d.nombre }}</option>
                    }
                  </select>
                </span>
              </label>
            </div>

            <div class="fila-2">
              <label class="field" for="horaInicioHorario">
                <span class="field__label">Hora de inicio</span>
                <span class="field__control"><input id="horaInicioHorario" type="time" [(ngModel)]="horaInicioHorario" name="horaInicioHorario" required /></span>
              </label>
              <label class="field" for="horaFinHorario">
                <span class="field__label">Hora de fin</span>
                <span class="field__control"><input id="horaFinHorario" type="time" [(ngModel)]="horaFinHorario" name="horaFinHorario" required /></span>
              </label>
            </div>

            <label class="field" for="campoHorario">
              <span class="field__label">Campo / cancha (opcional)</span>
              <span class="field__control"><input id="campoHorario" type="text" [(ngModel)]="campoHorario" name="campoHorario" /></span>
            </label>

            @if (errorHorario()) { <div class="alert alert--danger" role="alert">{{ errorHorario() }}</div> }

            <button class="btn btn--primary btn--block" type="submit" [disabled]="guardandoHorario()">
              {{ guardandoHorario() ? 'Guardando…' : 'Guardar horario' }}
            </button>
          </form>
        }

        @if (horarios().length > 0) {
          <div class="lista-horarios">
            @for (h of horarios(); track h.idHorario) {
              <div class="fila-horario">
                <span class="badge badge--info">{{ nombreDia(h.diaSemana) }}</span>
                <span class="horario-info">{{ h.categoria }} · {{ horaCorta(h.horaInicio) }}–{{ horaCorta(h.horaFin) }}{{ h.campo ? ' · ' + h.campo : '' }}</span>
                <button type="button" class="btn btn--ghost btn--sm" (click)="onDesactivarHorario(h.idHorario)">Quitar</button>
              </div>
            }
          </div>
        }
      </section>

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
                  @if (s.horaInicio) { {{ horaCorta(s.horaInicio) }} }
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

    .horario-semanal { padding: 1.25rem; margin-bottom: 1.5rem; }
    .cabecera-seccion { display: flex; align-items: center; justify-content: space-between; gap: 1rem; }
    .subtitulo { font-size: .95rem; }
    .ayuda { font-size: .8rem; color: var(--color-text-muted); margin: .35rem 0 0; }
    .btn--sm { padding: .35rem .7rem; font-size: .78rem; }
    .formulario-horario {
      display: flex; flex-direction: column; gap: .9rem;
      margin-top: 1rem; padding-top: 1rem; border-top: 1px solid var(--color-border-light);
    }
    .lista-horarios { display: flex; flex-direction: column; gap: .1rem; margin-top: 1rem; }
    .fila-horario { display: flex; align-items: center; gap: .6rem; padding: .5rem .1rem; border-bottom: 1px solid var(--color-border-light); }
    .fila-horario:last-child { border-bottom: none; }
    .horario-info { flex: 1; font-size: .85rem; }

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

  readonly horaCorta = horaCorta;

  readonly sesiones = signal<Sesion[]>([]);
  readonly categorias = signal<CategoriaOpcion[]>([]);
  readonly cargando = signal(false);
  readonly guardando = signal(false);
  readonly error = signal('');
  readonly mostrarFormulario = signal(false);

  /** Propiedades planas, no signals: [(ngModel)] las actualiza via su propio manejador de evento. */
  idCategoria: number | null = null;
  /** Por defecto hoy (fecha local del navegador): la jornada extra casi siempre es la de hoy mismo; se puede cambiar a mano. */
  fecha = fechaHoyIso();
  horaInicio = '';
  horaFin = '';
  campo = '';

  readonly horarios = signal<Horario[]>([]);
  readonly mostrarFormularioHorario = signal(false);
  readonly guardandoHorario = signal(false);
  readonly errorHorario = signal('');
  readonly diasSemana = DIAS_SEMANA;

  idCategoriaHorario: number | null = null;
  diaSemana: number | null = null;
  horaInicioHorario = '';
  horaFinHorario = '';
  campoHorario = '';

  ngOnInit(): void {
    this.cargarSesiones();
    this.cargarHorarios();
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
        this.fecha = fechaHoyIso();
        this.horaInicio = '';
        this.horaFin = '';
        this.campo = '';
        this.cargarSesiones();
      },
      error: (err) => {
        this.guardando.set(false);
        this.error.set(mensajeDeError(err, 'No se pudo crear la sesión.'));
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

  private cargarHorarios(): void {
    this.sesionesService.misHorarios().subscribe({
      next: (horarios) => this.horarios.set(horarios),
      error: () => {},
    });
  }

  alternarFormularioHorario(): void {
    this.mostrarFormularioHorario.set(!this.mostrarFormularioHorario());
    this.errorHorario.set('');
  }

  onCrearHorario(): void {
    if (!this.idCategoriaHorario || !this.diaSemana || !this.horaInicioHorario || !this.horaFinHorario) {
      this.errorHorario.set('Completa categoría, día y horas.');
      return;
    }
    if (this.horaFinHorario <= this.horaInicioHorario) {
      this.errorHorario.set('La hora de fin debe ser posterior a la de inicio.');
      return;
    }

    this.guardandoHorario.set(true);
    this.errorHorario.set('');
    this.sesionesService.crearHorario({
      idCategoria: this.idCategoriaHorario,
      diaSemana: this.diaSemana,
      horaInicio: this.horaInicioHorario,
      horaFin: this.horaFinHorario,
      campo: this.campoHorario || null,
      descripcion: null,
    }).subscribe({
      next: () => {
        this.guardandoHorario.set(false);
        this.mostrarFormularioHorario.set(false);
        this.idCategoriaHorario = null;
        this.diaSemana = null;
        this.horaInicioHorario = '';
        this.horaFinHorario = '';
        this.campoHorario = '';
        this.cargarHorarios();
      },
      error: (err) => {
        this.guardandoHorario.set(false);
        this.errorHorario.set(mensajeDeError(err, 'No se pudo guardar el horario.'));
      },
    });
  }

  onDesactivarHorario(idHorario: number): void {
    this.sesionesService.desactivarHorario(idHorario).subscribe({
      next: () => this.cargarHorarios(),
      error: () => {},
    });
  }

  nombreDia(dia: number): string {
    return this.diasSemana.find((d) => d.valor === dia)?.nombre ?? String(dia);
  }
}
