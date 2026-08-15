import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../auth/auth.service';
import { descargarBlob } from '../../core/descargar-archivo';
import { ReportesService } from './reportes.service';
import { CategoriaOpcionReporte, EstudianteOpcionReporte, FiltrosReporte } from './reportes.models';

type TipoReporte = 'estudiantes-fichas' | 'pagos' | 'asistencias' | 'evaluaciones' | 'lesiones';

interface TarjetaReporte {
  tipo: TipoReporte;
  titulo: string;
  descripcion: string;
  archivo: string;
  conCategoria: boolean;
  conActivo: boolean;
  roles: string[];
}

const TARJETAS: TarjetaReporte[] = [
  { tipo: 'estudiantes-fichas', titulo: 'Fichas de estudiantes', descripcion: 'Listado de estudiantes con categoría y estado.',
    archivo: 'fichas-estudiantes.pdf', conCategoria: true, conActivo: true, roles: ['ADMINISTRADOR', 'RECEPCIONISTA', 'ENTRENADOR'] },
  { tipo: 'pagos', titulo: 'Pagos', descripcion: 'Historial de pagos por estudiante y período.',
    archivo: 'pagos.pdf', conCategoria: false, conActivo: false, roles: ['ADMINISTRADOR', 'RECEPCIONISTA'] },
  { tipo: 'asistencias', titulo: 'Asistencias', descripcion: 'Asistencia a sesiones por estudiante o categoría.',
    archivo: 'asistencias.pdf', conCategoria: true, conActivo: false, roles: ['ADMINISTRADOR', 'ENTRENADOR'] },
  { tipo: 'evaluaciones', titulo: 'Evaluaciones', descripcion: 'Resultados de evaluación diaria por estudiante o categoría.',
    archivo: 'evaluaciones.pdf', conCategoria: true, conActivo: false, roles: ['ADMINISTRADOR', 'ENTRENADOR'] },
  { tipo: 'lesiones', titulo: 'Lesiones', descripcion: 'Lesiones registradas por estudiante o categoría.',
    archivo: 'lesiones.pdf', conCategoria: true, conActivo: false, roles: ['ADMINISTRADOR', 'ENTRENADOR'] },
];

/** Reportes PDF: tarjetas visibles según el rol, cada una con sus propios filtros. */
@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="pantalla">
      <div class="encabezado">
        <h1 class="titulo-pantalla">Reportes</h1>
        <p class="subtitulo-pantalla">Genera reportes en PDF con los datos del sistema.</p>
      </div>

      <div class="grid">
        @for (t of tarjetas(); track t.tipo) {
          <div class="card tarjeta">
            <h2 class="tarjeta__titulo">{{ t.titulo }}</h2>
            <p class="tarjeta__descripcion">{{ t.descripcion }}</p>

            <label class="field" [attr.for]="t.tipo + '-estudiante'">
              <span class="field__label">Estudiante (opcional)</span>
              <span class="field__control">
                <select [id]="t.tipo + '-estudiante'" [(ngModel)]="filtros[t.tipo].estudianteId" [name]="t.tipo + '-estudiante'">
                  <option [ngValue]="null">Todos</option>
                  @for (e of estudiantes(); track e.idEstudiante) {
                    <option [ngValue]="e.idEstudiante">{{ e.nombreCompleto }}</option>
                  }
                </select>
              </span>
            </label>

            @if (t.conCategoria) {
              <label class="field" [attr.for]="t.tipo + '-categoria'">
                <span class="field__label">Categoría (opcional)</span>
                <span class="field__control">
                  <select [id]="t.tipo + '-categoria'" [(ngModel)]="filtros[t.tipo].categoria" [name]="t.tipo + '-categoria'">
                    <option [ngValue]="null">Todas</option>
                    @for (c of categorias(); track c.idCategoria) {
                      <option [ngValue]="c.idCategoria">{{ c.nombre }}</option>
                    }
                  </select>
                </span>
              </label>
            }

            @if (t.conActivo) {
              <label class="field" [attr.for]="t.tipo + '-activo'">
                <span class="field__label">Estado (opcional)</span>
                <span class="field__control">
                  <select [id]="t.tipo + '-activo'" [(ngModel)]="filtros[t.tipo].activo" [name]="t.tipo + '-activo'">
                    <option [ngValue]="null">Todos</option>
                    <option [ngValue]="true">Activos</option>
                    <option [ngValue]="false">Inactivos</option>
                  </select>
                </span>
              </label>
            }

            @if (t.tipo !== 'estudiantes-fichas') {
              <div class="fila-fechas">
                <label class="field" [attr.for]="t.tipo + '-desde'">
                  <span class="field__label">Desde</span>
                  <span class="field__control"><input [id]="t.tipo + '-desde'" type="date" [(ngModel)]="filtros[t.tipo].fechaDesde" [name]="t.tipo + '-desde'" /></span>
                </label>
                <label class="field" [attr.for]="t.tipo + '-hasta'">
                  <span class="field__label">Hasta</span>
                  <span class="field__control"><input [id]="t.tipo + '-hasta'" type="date" [(ngModel)]="filtros[t.tipo].fechaHasta" [name]="t.tipo + '-hasta'" /></span>
                </label>
              </div>
            }

            @if (error()[t.tipo]) { <div class="alert alert--danger">{{ error()[t.tipo] }}</div> }

            <button type="button" class="btn btn--primary btn--block" [disabled]="generando()[t.tipo]" (click)="generar(t)">
              @if (generando()[t.tipo]) {
                <span class="spinner"></span> Generando…
              } @else {
                Generar PDF
              }
            </button>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .pantalla { max-width: 1200px; margin: 0 auto; padding: 1.5rem 1.25rem 3rem; display: flex; flex-direction: column; gap: 1.25rem; }
    .encabezado { display: flex; flex-direction: column; gap: .3rem; }
    .titulo-pantalla { font-size: 1.5rem; }
    .subtitulo-pantalla { color: var(--color-text-muted); font-size: .92rem; }

    .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 1.1rem; }
    .tarjeta { padding: 1.25rem; display: flex; flex-direction: column; gap: .6rem; }
    .tarjeta__titulo { font-size: 1.05rem; }
    .tarjeta__descripcion { color: var(--color-text-muted); font-size: .85rem; margin-bottom: .3rem; }
    .field { margin-bottom: 0; }
    .field select { flex: 1; border: none; outline: none; padding: .7rem 0; background: transparent; color: var(--color-text); width: 100%; }
    .field__control input[type="date"] { min-width: 0; }
    .fila-fechas { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: .6rem; }
    @media (max-width: 360px) { .fila-fechas { grid-template-columns: 1fr; } }
  `],
})
export class ReportesComponent implements OnInit {
  private readonly servicio = inject(ReportesService);
  private readonly authService = inject(AuthService);

  readonly tarjetas = computed(() => {
    const rol = this.authService.currentUser()?.rol;
    return TARJETAS.filter((t) => rol && t.roles.includes(rol));
  });

  readonly estudiantes = signal<EstudianteOpcionReporte[]>([]);
  readonly categorias = signal<CategoriaOpcionReporte[]>([]);
  readonly generando = signal<Record<string, boolean>>({});
  readonly error = signal<Record<string, string>>({});

  filtros: Record<TipoReporte, FiltrosReporte> = {
    'estudiantes-fichas': { categoria: null, activo: null },
    pagos: { estudianteId: null, fechaDesde: '', fechaHasta: '' },
    asistencias: { estudianteId: null, categoria: null, fechaDesde: '', fechaHasta: '' },
    evaluaciones: { estudianteId: null, categoria: null, fechaDesde: '', fechaHasta: '' },
    lesiones: { estudianteId: null, categoria: null, fechaDesde: '', fechaHasta: '' },
  };

  ngOnInit(): void {
    this.servicio.listarEstudiantes().subscribe({ next: (e) => this.estudiantes.set(e), error: () => {} });
    this.servicio.categoriasActivas().subscribe({ next: (c) => this.categorias.set(c), error: () => {} });
  }

  generar(tarjeta: TarjetaReporte): void {
    this.generando.update((g) => ({ ...g, [tarjeta.tipo]: true }));
    this.error.update((e) => ({ ...e, [tarjeta.tipo]: '' }));

    const filtros = this.filtros[tarjeta.tipo];
    const solicitud = {
      'estudiantes-fichas': () => this.servicio.estudiantesFichas(filtros),
      pagos: () => this.servicio.pagos(filtros),
      asistencias: () => this.servicio.asistencias(filtros),
      evaluaciones: () => this.servicio.evaluaciones(filtros),
      lesiones: () => this.servicio.lesiones(filtros),
    }[tarjeta.tipo]();

    solicitud.subscribe({
      next: (blob) => {
        descargarBlob(blob, tarjeta.archivo);
        this.generando.update((g) => ({ ...g, [tarjeta.tipo]: false }));
      },
      error: (err) => {
        this.generando.update((g) => ({ ...g, [tarjeta.tipo]: false }));
        this.mensajeDeError(err).then((mensaje) =>
          this.error.update((e) => ({ ...e, [tarjeta.tipo]: mensaje })));
      },
    });
  }

  private async mensajeDeError(err: any): Promise<string> {
    if (err?.error instanceof Blob) {
      try {
        const texto = await err.error.text();
        return JSON.parse(texto)?.detail ?? 'No se pudo generar el reporte';
      } catch {
        return 'No se pudo generar el reporte';
      }
    }
    return err?.error?.detail ?? 'No se pudo generar el reporte';
  }
}
