import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CargandoComponent } from '../../core/cargando.component';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ConsentimientosService } from './consentimientos.service';
import { Consentimiento, EstudianteOpcion, RepresentanteConVinculos } from './consentimientos.models';
import { BuscadorOpcionesComponent, OpcionBuscable } from '../../core/buscador-opciones.component';
import { fechaHoraCorta } from '../../core/formato-fecha';
import { mensajeDeError } from '../../core/mensaje-error';
import { ConfirmarAccionComponent } from '../../core/confirmar-accion.component';

const ALCANCES_SUGERIDOS = [
  'NOTIFICACIONES',
  'NOTIFICACIONES_ASISTENCIA',
  'NOTIFICACIONES_LESION',
];

interface RepresentanteDelEstudiante {
  guardianId: number;
  name: string;
  relationship: string | null;
  consentimientos: Consentimiento[];
  vigente: Consentimiento | null;
}

@Component({
  selector: 'app-consentimientos',
  standalone: true,
  imports: [CommonModule, FormsModule, BuscadorOpcionesComponent, CargandoComponent, ConfirmarAccionComponent],
  template: `
    <div class="contenido">
      <h1 class="titulo-panel">Consentimientos</h1>
      <p class="subtitulo-pantalla">
        Autorización del representante legal para que el sistema le envíe notificaciones
        sobre su representado.
      </p>

      <div class="nota">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
             stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line>
          <line x1="12" y1="8" x2="12.01" y2="8"></line>
        </svg>
        <p>
          El consentimiento habilita el <strong>envío de notificaciones</strong>. La consulta de
          informes por parte del representante no depende de esto: se autoriza por el vínculo con
          el estudiante.
        </p>
      </div>

      @if (error()) { <div class="alert alert--danger" role="alert">{{ error() }}</div> }
      @if (exito()) { <div class="alert alert--success" role="status">{{ exito() }}</div> }

      <section class="card">
        <app-buscador-opciones
          etiqueta="Estudiante"
          marcador="Busca por nombre o categoría…"
          [opciones]="opcionesEstudiantes()"
          [cargando]="cargandoCatalogos()"
          [textoSeleccionado]="nombreEstudianteElegido()"
          (seleccionada)="elegirEstudiante($event.id)"
          (limpiada)="limpiarEstudiante()" />
      </section>

      @if (studentId() !== null) {
        <section class="card bloque">
          @if (cargandoConsentimientos()) {
            <app-cargando />
          } @else if (representantes().length === 0) {
            <div class="vacio">
              <p><strong>Este estudiante no tiene representantes vinculados.</strong></p>
              <p class="vacio__pie">
                Primero hay que vincularle un representante desde Personas → ficha del estudiante.
                Sin vínculo no hay a quién pedirle consentimiento.
              </p>
            </div>
          } @else {
            <h2 class="titulo-card">Representantes de {{ nombreEstudianteElegido() }}</h2>

            @for (r of representantes(); track r.guardianId) {
              <div class="fila-rep">
                <div class="rep-info">
                  <span class="rep-nombre">{{ r.name }}</span>
                  <span class="rep-relacion">{{ r.relationship ?? 'sin relación registrada' }}</span>
                </div>

                <div class="rep-estado">
                  @if (r.vigente; as c) {
                    <span class="badge badge--success">{{ c.scope }} · vigente</span>
                    <span class="desde">desde {{ fechaHora(c.grantedAt) }}</span>
                  } @else {
                    <span class="badge badge--neutral">sin consentimiento vigente</span>
                  }
                </div>

                <div class="rep-acciones">
                  @if (r.vigente; as c) {
                    <app-confirmar-accion etiqueta="Revocar"
                                          [pregunta]="'¿Revocar el consentimiento de ' + r.name + '? No se deshace: habria que otorgar uno nuevo.'"
                                          textoConfirmar="Sí, revocar" enCurso="Revocando…"
                                          [ocupado]="guardando()" (confirmado)="revocar(c, r.name)" />
                  } @else {
                    <button class="btn btn--primary btn--sm" type="button"
                            [disabled]="guardando() || !scope.trim()"
                            (click)="otorgar(r)">Otorgar</button>
                  }
                </div>
              </div>

              @if (historial(r).length > 0) {
                <details class="historial">
                  <summary>Historial ({{ historial(r).length }} revocado{{ historial(r).length === 1 ? '' : 's' }})</summary>
                  @for (c of historial(r); track c.consentId) {
                    <div class="historial__fila">
                      <span>{{ c.scope }}</span>
                      <span class="historial__fechas">
                        {{ fechaHora(c.grantedAt) }} → revocado {{ fechaHora(c.revokedAt) }}
                      </span>
                    </div>
                  }
                </details>
              }
            }

            <div class="alcance">
              <label class="field" for="alcance">
                <span class="field__label">Alcance a otorgar</span>
                <span class="field__control">
                  <input id="alcance" [(ngModel)]="scope" name="alcance"
                         list="alcances-sugeridos" maxlength="50" />
                </span>
              </label>
              <datalist id="alcances-sugeridos">
                @for (a of alcancesSugeridos; track a) { <option [value]="a"></option> }
              </datalist>
              <p class="alcance__pie">
                Se aplica al pulsar “Otorgar”. Un mismo representante no puede tener dos
                consentimientos vigentes con el mismo alcance.
              </p>
            </div>
          }
        </section>
      }
    </div>
  `,
  styles: [`
    .content { max-width: 880px; margin: 0 auto; padding: 1.5rem 1.25rem; }
    .subtitulo-pantalla { margin: .2rem 0 1rem; font-size: .86rem; color: var(--color-text-muted); }
    .nota {
      display: flex; gap: .6rem; align-items: flex-start;
      background: var(--color-info-bg); color: var(--color-info-text);
      border-radius: var(--radius-sm); padding: .7rem .9rem; margin-bottom: 1.2rem;
    }
    .nota svg { width: 18px; height: 18px; flex-shrink: 0; margin-top: .1rem; }
    .nota p { margin: 0; font-size: .82rem; line-height: 1.45; }
    .card { padding: 1.15rem 1.35rem; margin-bottom: 1.1rem; }
    .titulo-card { font-size: 1rem; margin-bottom: .8rem; }
    .aviso { font-size: .86rem; color: var(--color-text-muted); margin: 0; }
    .vacio p { margin: 0 0 .3rem; font-size: .88rem; }
    .vacio__pie { color: var(--color-text-muted); font-size: .82rem !important; }
    .fila-rep {
      display: flex; align-items: center; gap: 1rem; flex-wrap: wrap;
      padding: .75rem 0; border-bottom: 1px solid var(--color-border-light);
    }
    .rep-info { display: flex; flex-direction: column; gap: .1rem; min-width: 0; flex: 1; }
    .rep-nombre { font-weight: 600; }
    .rep-relacion { font-size: .78rem; color: var(--color-text-muted); }
    .rep-estado { display: flex; flex-direction: column; align-items: flex-end; gap: .15rem; }
    .from { font-size: .74rem; color: var(--color-text-faint); }
    .rep-acciones { flex-shrink: 0; }
    .historial { margin: 0 0 .4rem; padding-left: .2rem; }
    .historial summary { font-size: .76rem; color: var(--color-text-muted); cursor: pointer; }
    .historial__fila {
      display: flex; gap: .8rem; justify-content: space-between;
      font-size: .78rem; padding: .3rem 0 .3rem .8rem;
    }
    .historial__fechas { color: var(--color-text-faint); }
    .scope { margin-top: 1rem; padding-top: .9rem; border-top: 1px solid var(--color-border-light); }
    .scope .field { margin-bottom: .4rem; max-width: 340px; }
    .alcance__pie { margin: 0; font-size: .76rem; color: var(--color-text-faint); }
  `],
})
export class ConsentimientosComponent implements OnInit {
  private readonly servicio = inject(ConsentimientosService);

  readonly alcancesSugeridos = ALCANCES_SUGERIDOS;
  readonly fechaHora = fechaHoraCorta;

  scope = ALCANCES_SUGERIDOS[0];

  readonly students = signal<EstudianteOpcion[]>([]);
  readonly todosLosRepresentantes = signal<RepresentanteConVinculos[]>([]);
  readonly consentimientos = signal<Consentimiento[]>([]);
  readonly studentId = signal<number | null>(null);

  readonly cargandoCatalogos = signal(true);
  readonly cargandoConsentimientos = signal(false);
  readonly guardando = signal(false);
  readonly error = signal('');
  readonly exito = signal('');

  readonly opcionesEstudiantes = computed<OpcionBuscable[]>(() =>
    this.students().map((e) => ({
      id: e.studentId,
      titulo: e.fullName,
      subtitulo: e.category ?? undefined,
    })));

  readonly nombreEstudianteElegido = computed(() => {
    const id = this.studentId();
    if (id === null) return null;
    return this.students().find((e) => e.studentId === id)?.fullName ?? null;
  });

  readonly representantes = computed<RepresentanteDelEstudiante[]>(() => {
    const id = this.studentId();
    if (id === null) return [];
    const deEsteEstudiante = this.consentimientos();

    return this.todosLosRepresentantes().flatMap((r) => {
      const vinculo = r.wards.find((e) => e.studentId === id);
      if (!vinculo) return [];
      const suyos = deEsteEstudiante.filter((c) => c.guardianId === r.guardianId);
      return [{
        guardianId: r.guardianId,
        name: `${r.name} ${r.lastName}`,
        relationship: vinculo.relationship,
        consentimientos: suyos,
        vigente: suyos.find((c) => c.vigente) ?? null,
      }];
    });
  });

  historial(r: RepresentanteDelEstudiante): Consentimiento[] {
    return r.consentimientos.filter((c) => !c.vigente);
  }

  ngOnInit(): void {
    this.servicio.estudiantes().subscribe({
      next: (e) => { this.students.set(e); this.cargandoCatalogos.set(false); },
      error: (e) => { this.error.set(mensajeDeError(e, 'No se pudo cargar la lista de estudiantes')); this.cargandoCatalogos.set(false); },
    });
    this.servicio.representantes().subscribe({
      next: (r) => this.todosLosRepresentantes.set(r),
      error: (e) => this.error.set(mensajeDeError(e, 'No se pudo cargar la lista de representantes')),
    });
  }

  elegirEstudiante(id: number): void {
    this.studentId.set(id);
    this.error.set('');
    this.exito.set('');
    this.cargarConsentimientos(id);
  }

  limpiarEstudiante(): void {
    this.studentId.set(null);
    this.consentimientos.set([]);
  }

  private cargarConsentimientos(id: number): void {
    this.cargandoConsentimientos.set(true);
    this.servicio.porEstudiante(id).subscribe({
      next: (c) => { this.consentimientos.set(c); this.cargandoConsentimientos.set(false); },
      error: (e) => {
        this.error.set(mensajeDeError(e, 'No se pudieron cargar los consentimientos'));
        this.cargandoConsentimientos.set(false);
      },
    });
  }

  otorgar(r: RepresentanteDelEstudiante): void {
    const id = this.studentId();
    if (id === null) return;

    this.guardando.set(true);
    this.error.set('');
    this.exito.set('');
    this.servicio.otorgar({
      guardianId: r.guardianId,
      studentId: id,
      scope: this.scope.trim(),
    }).subscribe({
      next: () => {
        this.guardando.set(false);
        this.exito.set(`${r.name} autorizó "${this.scope.trim()}" para ${this.nombreEstudianteElegido()}`);
        this.cargarConsentimientos(id);
      },
      error: (e) => {
        this.guardando.set(false);
        this.error.set(mensajeDeError(e, 'No se pudo registrar el consentimiento'));
      },
    });
  }

  revocar(c: Consentimiento, nombreRepresentante: string): void {
    const id = this.studentId();
    if (id === null) return;

    this.guardando.set(true);
    this.error.set('');
    this.exito.set('');
    this.servicio.revocar(c.consentId).subscribe({
      next: () => {
        this.guardando.set(false);

        this.exito.set(`Se revocó "${c.scope}" de ${nombreRepresentante}; queda en el historial`);
        this.cargarConsentimientos(id);
      },
      error: (e) => {
        this.guardando.set(false);
        this.error.set(mensajeDeError(e, 'No se pudo revocar el consentimiento'));
      },
    });
  }
}
