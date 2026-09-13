import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../auth/auth.service';
import { descargarBlob } from '../../core/descargar-archivo';
import { mensajeDeError as traducirError } from '../../core/mensaje-error';
import { ReportesService } from './reportes.service';
import { CategoriaOpcionReporte, EstudianteOpcionReporte, FiltrosReporte } from './reportes.models';
import { BuscadorOpcionesComponent, OpcionBuscable } from '../../core/buscador-opciones.component';

type TipoReporte = 'estudiantes-fichas' | 'pagos' | 'asistencias' | 'evaluaciones' | 'lesiones';

interface TarjetaReporte {
  type: TipoReporte;
  titulo: string;
  description: string;
  archivo: string;
  conCategoria: boolean;
  conActivo: boolean;
  roles: string[];
}

const TARJETAS: TarjetaReporte[] = [
  { type: 'estudiantes-fichas', titulo: 'Fichas de estudiantes', description: 'Listado de estudiantes con categoría y estado.',
    archivo: 'fichas-estudiantes.pdf', conCategoria: true, conActivo: true, roles: ['ADMINISTRADOR', 'RECEPCIONISTA', 'ENTRENADOR'] },
  { type: 'pagos', titulo: 'Pagos', description: 'Historial de pagos por estudiante y período.',
    archivo: 'pagos.pdf', conCategoria: false, conActivo: false, roles: ['ADMINISTRADOR', 'RECEPCIONISTA'] },
  { type: 'asistencias', titulo: 'Asistencias', description: 'Asistencia a sesiones por estudiante o categoría.',
    archivo: 'asistencias.pdf', conCategoria: true, conActivo: false, roles: ['ADMINISTRADOR', 'ENTRENADOR'] },
  { type: 'evaluaciones', titulo: 'Evaluaciones', description: 'Resultados de evaluación diaria por estudiante o categoría.',
    archivo: 'evaluaciones.pdf', conCategoria: true, conActivo: false, roles: ['ADMINISTRADOR', 'ENTRENADOR'] },
  { type: 'lesiones', titulo: 'Lesiones', description: 'Lesiones registradas por estudiante o categoría.',
    archivo: 'lesiones.pdf', conCategoria: true, conActivo: false, roles: ['ADMINISTRADOR', 'ENTRENADOR'] },
];

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [CommonModule, FormsModule, BuscadorOpcionesComponent],
  template: `
    <div class="pantalla">
      <div class="encabezado">
        <h1 class="titulo-pantalla">Reportes</h1>
        <p class="subtitulo-pantalla">Genera reportes en PDF con los datos del sistema.</p>
      </div>

      <div class="grid">
        @for (t of tarjetas(); track t.type) {
          <div class="card tarjeta">
            <h2 class="tarjeta__titulo">{{ t.titulo }}</h2>
            <p class="tarjeta__descripcion">{{ t.description }}</p>

            <app-buscador-opciones
              etiqueta="Estudiante (opcional)"
              marcador="Todos — escribe un nombre para filtrar…"
              [opciones]="opcionesEstudiantes()"
              [textoSeleccionado]="nombreDe(filtros[t.type].estudianteId)"
              (seleccionada)="filtros[t.type].estudianteId = $event.id"
              (limpiada)="filtros[t.type].estudianteId = null" />

            @if (t.conCategoria) {
              <label class="field" [attr.for]="t.type + '-categoria'">
                <span class="field__label">Categoría (opcional)</span>
                <span class="field__control">
                  <select [id]="t.type + '-categoria'" [(ngModel)]="filtros[t.type].category" [name]="t.type + '-categoria'">
                    <option [ngValue]="null">Todas</option>
                    @for (c of categorias(); track c.categoryId) {
                      <option [ngValue]="c.categoryId">{{ c.name }}</option>
                    }
                  </select>
                </span>
              </label>
            }

            @if (t.conActivo) {
              <label class="field" [attr.for]="t.type + '-activo'">
                <span class="field__label">Estado (opcional)</span>
                <span class="field__control">
                  <select [id]="t.type + '-activo'" [(ngModel)]="filtros[t.type].active" [name]="t.type + '-activo'">
                    <option [ngValue]="null">Todos</option>
                    <option [ngValue]="true">Activos</option>
                    <option [ngValue]="false">Inactivos</option>
                  </select>
                </span>
              </label>
            }

            @if (t.type !== 'estudiantes-fichas') {
              <div class="fila-fechas">
                <label class="field" [attr.for]="t.type + '-desde'">
                  <span class="field__label">Desde</span>
                  <span class="field__control"><input [id]="t.type + '-desde'" type="date" [(ngModel)]="filtros[t.type].fechaDesde" [name]="t.type + '-desde'" /></span>
                </label>
                <label class="field" [attr.for]="t.type + '-hasta'">
                  <span class="field__label">Hasta</span>
                  <span class="field__control"><input [id]="t.type + '-hasta'" type="date" [(ngModel)]="filtros[t.type].fechaHasta" [name]="t.type + '-hasta'" /></span>
                </label>
              </div>
            }

            @if (error()[t.type]) { <div class="alert alert--danger">{{ error()[t.type] }}</div> }

            <button type="button" class="btn btn--primary btn--block" [disabled]="generando()[t.type]" (click)="generar(t)">
              @if (generando()[t.type]) {
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
    const rol = this.authService.currentUser()?.role;
    return TARJETAS.filter((t) => rol && t.roles.includes(rol));
  });

  readonly students = signal<EstudianteOpcionReporte[]>([]);

  readonly opcionesEstudiantes = computed<OpcionBuscable[]>(() =>
    this.students().map((e) => ({ id: e.studentId, titulo: e.fullName })));

  nombreDe(id: number | null | undefined): string | null {
    if (id === null || id === undefined) return null;
    return this.students().find((e) => e.studentId === id)?.fullName ?? null;
  }
  readonly categorias = signal<CategoriaOpcionReporte[]>([]);
  readonly generando = signal<Record<string, boolean>>({});
  readonly error = signal<Record<string, string>>({});

  filtros: Record<TipoReporte, FiltrosReporte> = {
    'estudiantes-fichas': { category: null, active: null },
    pagos: { estudianteId: null, fechaDesde: '', fechaHasta: '' },
    asistencias: { estudianteId: null, category: null, fechaDesde: '', fechaHasta: '' },
    evaluaciones: { estudianteId: null, category: null, fechaDesde: '', fechaHasta: '' },
    lesiones: { estudianteId: null, category: null, fechaDesde: '', fechaHasta: '' },
  };

  ngOnInit(): void {
    this.servicio.listarEstudiantes().subscribe({ next: (e) => this.students.set(e), error: () => {} });
    this.servicio.categoriasActivas().subscribe({ next: (c) => this.categorias.set(c), error: () => {} });
  }

  generar(tarjeta: TarjetaReporte): void {
    this.generando.update((g) => ({ ...g, [tarjeta.type]: true }));
    this.error.update((e) => ({ ...e, [tarjeta.type]: '' }));

    const filtros = this.filtros[tarjeta.type];
    const solicitud = {
      'estudiantes-fichas': () => this.servicio.estudiantesFichas(filtros),
      pagos: () => this.servicio.pagos(filtros),
      asistencias: () => this.servicio.asistencias(filtros),
      evaluaciones: () => this.servicio.evaluaciones(filtros),
      lesiones: () => this.servicio.lesiones(filtros),
    }[tarjeta.type]();

    solicitud.subscribe({
      next: (blob) => {
        descargarBlob(blob, tarjeta.archivo);
        this.generando.update((g) => ({ ...g, [tarjeta.type]: false }));
      },
      error: (err) => {
        this.generando.update((g) => ({ ...g, [tarjeta.type]: false }));
        this.mensajeDeError(err).then((mensaje) =>
          this.error.update((e) => ({ ...e, [tarjeta.type]: mensaje })));
      },
    });
  }

  private async mensajeDeError(err: any): Promise<string> {
    const porDefecto = 'No se pudo generar el reporte';

    if (err?.error instanceof Blob) {
      try {
        const texto = await err.error.text();
        return traducirError({ error: JSON.parse(texto) }, porDefecto);
      } catch {
        return porDefecto;
      }
    }
    return traducirError(err, porDefecto);
  }
}
