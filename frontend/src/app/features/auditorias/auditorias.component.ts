import { Component, OnInit, inject, signal } from '@angular/core';
import { CargandoComponent } from '../../core/cargando.component';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuditoriaResponse, AuditoriaService, FiltrosAuditoria } from './auditoria.service';
import { fechaHoraCorta } from '../../core/formato-fecha';

const ACCIONES = ['CREAR', 'EDITAR', 'ELIMINAR', 'LOGIN', 'LOGIN_FALLIDO', 'LOGOUT'];
const TAMANO_PAGINA = 20;

@Component({
  selector: 'app-auditorias',
  standalone: true,
  imports: [CommonModule, FormsModule, CargandoComponent],
  template: `
    <div class="pantalla">
      <div class="encabezado">
        <h1 class="titulo-pantalla">Auditorías</h1>
        <p class="subtitulo-pantalla">Historial de cambios y accesos registrados en el sistema.</p>
      </div>

      <div class="card filtros">
        <label class="field" for="usuario">
          <span class="field__label">Usuario</span>
          <span class="field__control">
            <input id="usuario" type="text" placeholder="Buscar por usuario…" [(ngModel)]="filtros.usuario" name="usuario" (change)="buscar(0)" />
          </span>
        </label>
        <label class="field" for="accion">
          <span class="field__label">Acción</span>
          <span class="field__control">
            <select id="accion" [(ngModel)]="filtros.accion" name="accion" (change)="buscar(0)">
              <option value="">Todas</option>
              @for (a of acciones; track a) { <option [value]="a">{{ a }}</option> }
            </select>
          </span>
        </label>
        <label class="field" for="entidad">
          <span class="field__label">Entidad</span>
          <span class="field__control">
            <input id="entidad" type="text" placeholder="Ej. Lesion, Pago…" [(ngModel)]="filtros.entidad" name="entidad" (change)="buscar(0)" />
          </span>
        </label>
        <label class="field" for="fechaDesde">
          <span class="field__label">Desde</span>
          <span class="field__control">
            <input id="fechaDesde" type="date" [(ngModel)]="filtros.fechaDesde" name="fechaDesde" (change)="buscar(0)" />
          </span>
        </label>
        <label class="field" for="fechaHasta">
          <span class="field__label">Hasta</span>
          <span class="field__control">
            <input id="fechaHasta" type="date" [(ngModel)]="filtros.fechaHasta" name="fechaHasta" (change)="buscar(0)" />
          </span>
        </label>
        <button type="button" class="btn btn--ghost" (click)="limpiarFiltros()">Limpiar filtros</button>
      </div>

      <div class="card tabla-card">
        @if (cargando()) {
          <app-cargando />
        } @else if (filas().length === 0) {
          <div class="vacio">
            <p class="vacio__titulo">Sin resultados</p>
            <p class="vacio__texto">No hay auditorías que coincidan con los filtros seleccionados.</p>
          </div>
        } @else {
          <div class="tabla">
            <div class="fila fila--encabezado">
              <span>Fecha</span><span>Usuario</span><span>Rol</span><span>Acción</span><span>Entidad</span><span>Descripción</span>
            </div>
            @for (a of filas(); track a.id) {
              <div class="fila">
                <span class="fecha">{{ fechaHora(a.fecha) }}</span>
                <span>{{ a.usuario }}</span>
                <span>{{ a.rol ?? '-' }}</span>
                <span class="badge" [class.badge--success]="a.accion === 'CREAR'" [class.badge--info]="a.accion === 'EDITAR'"
                      [class.badge--danger]="a.accion === 'ELIMINAR' || a.accion === 'LOGIN_FALLIDO'">
                  {{ a.accion }}
                </span>
                <span>{{ a.entidad ?? '-' }}</span>
                <span class="descripcion">{{ a.descripcion }}</span>
              </div>
            }
          </div>

          <div class="paginacion">
            <button type="button" class="btn btn--ghost" [disabled]="pagina() === 0" (click)="buscar(pagina() - 1)">Anterior</button>
            <span class="paginacion__info">Página {{ pagina() + 1 }} de {{ totalPaginas() || 1 }} · {{ totalElementos() }} registros</span>
            <button type="button" class="btn btn--ghost" [disabled]="pagina() + 1 >= totalPaginas()" (click)="buscar(pagina() + 1)">Siguiente</button>
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
    .filtros { padding: 1.1rem 1.25rem; display: flex; flex-wrap: wrap; gap: .85rem; align-items: end; }
    .filtros .field { margin-bottom: 0; min-width: 150px; flex: 1; }
    .tabla-card { padding: 1.1rem 1.25rem; }
    .aviso { color: var(--color-text-muted); font-size: .9rem; padding: 1rem 0; }
    .tabla { display: flex; flex-direction: column; font-size: .85rem; }
    .fila { display: grid; grid-template-columns: 140px 130px 110px 100px 120px 1fr; gap: .75rem; padding: .65rem 0; border-bottom: 1px solid var(--color-border-light); align-items: center; }
    .fila:last-child { border-bottom: none; }
    .fila--encabezado { font-weight: 700; color: var(--color-text-faint); font-size: .72rem; text-transform: uppercase; letter-spacing: .03em; }
    .fecha { color: var(--color-text-muted); font-variant-numeric: tabular-nums; }
    .descripcion { color: var(--color-text-muted); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .vacio { text-align: center; padding: 2rem .5rem; }
    .vacio__titulo { font-weight: 700; font-size: .95rem; }
    .vacio__texto { color: var(--color-text-muted); font-size: .85rem; margin-top: .25rem; }
    .paginacion { display: flex; align-items: center; justify-content: center; gap: 1rem; padding-top: 1rem; }
    .paginacion__info { font-size: .82rem; color: var(--color-text-muted); }

    @media (max-width: 900px) {
      .fila { grid-template-columns: 1fr 1fr; }
      .fila--encabezado { display: none; }
    }
  `],
})
export class AuditoriasComponent implements OnInit {
  readonly fechaHora = fechaHoraCorta;

  private readonly servicio = inject(AuditoriaService);

  readonly acciones = ACCIONES;
  filtros: FiltrosAuditoria = {};

  readonly filas = signal<AuditoriaResponse[]>([]);
  readonly pagina = signal(0);
  readonly totalPaginas = signal(0);
  readonly totalElementos = signal(0);
  readonly cargando = signal(true);

  ngOnInit(): void {
    this.buscar(0);
  }

  buscar(pagina: number): void {
    this.cargando.set(true);
    this.servicio.listar(this.filtros, pagina, TAMANO_PAGINA).subscribe({
      next: (respuesta) => {
        this.filas.set(respuesta.content);
        this.pagina.set(respuesta.number);
        this.totalPaginas.set(respuesta.totalPages);
        this.totalElementos.set(respuesta.totalElements);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }

  limpiarFiltros(): void {
    this.filtros = {};
    this.buscar(0);
  }
}
