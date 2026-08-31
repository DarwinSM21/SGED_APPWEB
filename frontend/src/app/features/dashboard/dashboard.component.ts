import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CargandoComponent } from '../../core/cargando.component';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { homeRouteForRole } from '../../auth/home-route';
import { HistoricoIngresos, MapaAsistencia, PanelAlertas, SesionHoy } from './dashboard.models';
import { horaCorta, inicialesDe } from '../../core/formato-texto';
import { GraficosIngresosComponent } from './graficos.component';
import { MapaAsistenciaComponent } from './mapa-asistencia.component';

interface PaginaLigera {
  totalElements: number;
}

const NOMBRES_MES = [
  'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
  'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre',
];

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, GraficosIngresosComponent, MapaAsistenciaComponent, CargandoComponent],
  template: `
    <div class="contenido">
      @if (esOperativo()) {
        <h1 class="titulo-panel">{{ esAdministrador() ? 'Panel general' : 'Mi día' }}</h1>

        <section class="kpis">
          @if (esAdministrador()) {
            <div class="kpi">
              <span class="kpi__icono kpi__icono--success">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
              </span>
              <div>
                <p class="kpi__valor">{{ estudiantesActivos() ?? '—' }}</p>
                <p class="kpi__etiqueta">Estudiantes activos</p>
              </div>
            </div>
          }
          <div class="kpi">
            <span class="kpi__icono kpi__icono--info">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
            </span>
            <div>
              <p class="kpi__valor">{{ totalSesiones() }}</p>
              <p class="kpi__etiqueta">Sesiones hoy</p>
            </div>
          </div>
          <div class="kpi">
            <span class="kpi__icono kpi__icono--warning">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
            </span>
            <div>
              <p class="kpi__valor">{{ sesionesEnEvaluacion() }}</p>
              <p class="kpi__etiqueta">En evaluación</p>
            </div>
          </div>
          <div class="kpi">
            <span class="kpi__icono kpi__icono--neutral">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle></svg>
            </span>
            <div>
              <p class="kpi__valor">{{ sesionesSinIniciar() }}</p>
              <p class="kpi__etiqueta">Sin iniciar</p>
            </div>
          </div>
          <div class="kpi">
            <span class="kpi__icono" [class.kpi__icono--danger]="(lesionesActivas() ?? 0) > 0" [class.kpi__icono--neutral]="!(lesionesActivas() ?? 0)">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline></svg>
            </span>
            <div>
              <p class="kpi__valor">{{ lesionesActivas() ?? '—' }}</p>
              <p class="kpi__etiqueta">Lesiones activas</p>
            </div>
          </div>
        </section>

        @if (esAdministrador()) {
          <section class="accesos">
            <a class="acceso" routerLink="/personas">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
              Personas
            </a>
            <a class="acceso" routerLink="/pagos">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="5" width="20" height="14" rx="2" ry="2"></rect><line x1="2" y1="10" x2="22" y2="10"></line></svg>
              Pagos
            </a>
            <a class="acceso" routerLink="/recepcion">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg>
              Recepción
            </a>
          </section>
        } @else if (esEntrenador()) {
          <section class="accesos">
            <a class="acceso" routerLink="/entrenador/sesiones">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
              Ver todas mis sesiones
            </a>
          </section>
        }

        @if (esAdministrador() && historico() && alertas(); as a) {
          <app-graficos-ingresos
            [datos]="historico()!"
            [estudiantesActivos]="a.estudiantesActivos"
            [pendientes]="a.conMensualidadPendiente" />
        }

        @if (esOperativo() && mapa(); as m) {
          <app-mapa-asistencia [datos]="m" />
        }

        @if (esAdministrador() && alertas(); as a) {
          <section class="card panel-alertas">
            <div class="panel-alertas__cabecera">
              <div>
                <h2>Requieren atención</h2>
                <p class="panel-alertas__sub">
                  {{ a.totalEnRiesgo }} de {{ a.estudiantesActivos }} estudiantes ·
                  cuota de {{ nombreMes(a.mes) }} {{ a.anio }}
                </p>
              </div>
              @if (a.estudiantes.length > 0) {
                <div class="resumen-alertas">
                  @if (a.conMensualidadPendiente > 0) {
                    <span class="chip chip--dinero">{{ a.conMensualidadPendiente }} deben cuota</span>
                  }
                  @if (a.conAsistenciaBaja > 0) {
                    <span class="chip chip--falta">{{ a.conAsistenciaBaja }} asistencia &lt; {{ a.umbralAsistencia }}%</span>
                  }
                  @if (a.conLesionActiva > 0) {
                    <span class="chip chip--lesion">{{ a.conLesionActiva }} lesionados</span>
                  }
                </div>
              }
            </div>

            @if (a.estudiantes.length === 0) {
              <div class="todo-en-orden">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
                <p>Nadie con cuota pendiente, asistencia baja ni lesiones activas.</p>
              </div>
            } @else {
              @for (e of a.estudiantes; track e.idEstudiante) {
                <div class="fila-alerta" [attr.data-severidad]="e.totalAlertas">
                  <span class="franja"></span>
                  <span class="avatar avatar--muted">{{ iniciales(e.nombreCompleto) }}</span>
                  <div class="alerta-info">
                    <span class="alerta-nombre">{{ e.nombreCompleto }}</span>
                    <span class="alerta-categoria">{{ e.categoria ?? 'sin categoría' }}</span>
                  </div>
                  <div class="alerta-motivos">
                    @if (e.mensualidadPendiente) { <span class="chip chip--dinero">Debe cuota</span> }
                    @if (e.asistenciaBaja) {
                      <span class="chip chip--falta">Asistencia {{ e.porcentajeAsistencia | number: '1.0-0' }}%</span>
                    }
                    @if (e.lesionActiva) { <span class="chip chip--lesion">Lesión activa</span> }
                  </div>
                </div>
              }

              @if (a.totalEnRiesgo > a.estudiantes.length) {
                <p class="panel-alertas__resto">
                  Se muestran los {{ a.estudiantes.length }} casos más urgentes de
                  {{ a.totalEnRiesgo }}. Los contadores de arriba sí cuentan a todos.
                  <a routerLink="/reportes">Ver el listado completo en Reportes</a>
                </p>
              }
            }
          </section>
        }

        <section class="card lista">
          <div class="lista__cabecera">
            <h2>{{ esAdministrador() ? 'Sesiones de hoy' : 'Mis sesiones de hoy' }}</h2>
          </div>

          @if (cargandoSesiones()) {
            <app-cargando />
          } @else if (sesiones().length === 0) {
            <div class="vacio">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
              <p>No hay entrenamientos programados para hoy.</p>
            </div>
          } @else {
            @for (s of sesiones(); track s.idSesion) {
              <a class="sesion" [routerLink]="['/entrenador/sesion', s.idSesion]">
                <span class="avatar avatar--muted">{{ iniciales(s.entrenador) }}</span>
                <div class="sesion-info">
                  <span class="categoria">{{ s.categoria }}</span>
                  <span class="detalle">
                    {{ s.entrenador }}
                    @if (s.horaInicio) { · {{ horaCorta(s.horaInicio) }} }
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
      } @else if (usuario()) {
        <div class="card vacio vacio--pagina">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
          <p>Tu cuenta de consulta no tiene pantallas propias todavía. Contacta a un administrador si necesitas más acceso.</p>
        </div>
      }
    </div>
  `,
  styles: [`
    .contenido { max-width: 880px; margin: 0 auto; padding: 1.5rem 1.25rem 3rem; }
    .titulo-panel { font-size: 1.2rem; margin-bottom: 1.1rem; }
    .accesos { display: flex; flex-wrap: wrap; gap: .7rem; margin-bottom: 1.5rem; }
    .acceso {
      display: flex; align-items: center; gap: .55rem; padding: .7rem 1rem;
      border: 1.5px solid var(--color-border); border-radius: var(--radius-sm);
      background: var(--color-surface); color: var(--color-text); text-decoration: none;
      font-size: .85rem; font-weight: 600; transition: border-color var(--transition), background var(--transition);
    }
    .acceso:hover { border-color: var(--color-primary-500); background: var(--color-primary-50); }
    .acceso svg { width: 17px; height: 17px; color: var(--color-primary-600); flex-shrink: 0; }
    .panel-alertas { padding: 1.25rem 1.4rem; margin-bottom: 1.5rem; }
    .panel-alertas__cabecera {
      display: flex; align-items: flex-start; justify-content: space-between;
      gap: 1rem; flex-wrap: wrap; margin-bottom: .9rem;
    }
    .panel-alertas__cabecera h2 { font-size: 1rem; }
    .panel-alertas__sub { margin: .2rem 0 0; font-size: .78rem; color: var(--color-text-muted); }
    .resumen-alertas { display: flex; flex-wrap: wrap; gap: .4rem; }
    .panel-alertas__resto {
      margin: .9rem 0 0; padding-top: .75rem; font-size: .78rem;
      color: var(--color-text-muted); line-height: 1.5;
      border-top: 1px solid var(--color-border-light);
    }
    .chip {
      display: inline-flex; align-items: center; gap: .3rem;
      padding: .22rem .6rem; border-radius: var(--radius-full);
      font-size: .74rem; font-weight: 700; white-space: nowrap;
    }
    .chip--dinero { background: var(--color-warning-bg); color: var(--color-warning-text); }
    .chip--falta { background: var(--color-info-bg); color: var(--color-info-text); }
    .chip--lesion { background: var(--color-danger-bg); color: var(--color-danger-text); }
    .fila-alerta {
      display: flex; align-items: center; gap: .7rem;
      padding: .6rem .2rem .6rem 0; border-bottom: 1px solid var(--color-border-light);
    }
    .fila-alerta:last-child { border-bottom: none; }
    .franja { width: 4px; align-self: stretch; border-radius: 2px; background: var(--color-warning); flex-shrink: 0; }
    .fila-alerta[data-severidad="2"] .franja { background: #e0872c; }
    .fila-alerta[data-severidad="3"] .franja { background: var(--color-danger); }
    .alerta-info { display: flex; flex-direction: column; flex: 1; min-width: 0; }
    .alerta-nombre { font-weight: 600; font-size: .9rem; }
    .alerta-categoria { font-size: .76rem; color: var(--color-text-faint); }
    .alerta-motivos { display: flex; flex-wrap: wrap; gap: .35rem; justify-content: flex-end; }
    .todo-en-orden {
      display: flex; flex-direction: column; align-items: center; gap: .55rem;
      text-align: center; padding: 1.75rem 1rem; color: var(--color-success-text);
    }
    .todo-en-orden svg { width: 30px; height: 30px; }
    .todo-en-orden p { font-size: .87rem; color: var(--color-text-muted); }

    @media (max-width: 620px) {
      .fila-alerta { flex-wrap: wrap; }
      .alerta-motivos { justify-content: flex-start; width: 100%; padding-left: 2.9rem; }
    }
    .kpis {
      display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
      gap: .9rem; margin-bottom: 1.5rem;
    }
    .kpi {
      display: flex; align-items: center; gap: .75rem;
      background: var(--color-surface); border: 1px solid var(--color-border);
      border-radius: var(--radius-md); padding: 1rem; box-shadow: var(--shadow-sm);
    }
    .kpi__icono {
      width: 42px; height: 42px; border-radius: var(--radius-sm); flex-shrink: 0;
      display: flex; align-items: center; justify-content: center;
    }
    .kpi__icono svg { width: 22px; height: 22px; }
    .kpi__icono--success { background: var(--color-success-bg); color: var(--color-success-text); }
    .kpi__icono--info { background: var(--color-info-bg); color: var(--color-info); }
    .kpi__icono--warning { background: var(--color-warning-bg); color: var(--color-warning); }
    .kpi__icono--danger { background: var(--color-danger-bg); color: var(--color-danger); }
    .kpi__icono--neutral { background: var(--color-neutral-bg); color: var(--color-text-faint); }
    .kpi__valor { font-size: 1.4rem; font-weight: 700; line-height: 1.1; }
    .kpi__etiqueta { font-size: .78rem; color: var(--color-text-muted); margin-top: .15rem; }
    .lista { padding: 1.25rem; }
    .lista__cabecera { margin-bottom: .9rem; }
    .lista__cabecera h2 { font-size: 1rem; }
    .aviso { color: var(--color-text-muted); font-size: .9rem; padding: .5rem 0; }
    .vacio {
      display: flex; flex-direction: column; align-items: center; gap: .75rem;
      color: var(--color-text-faint); text-align: center; padding: 2rem 1rem;
    }
    .vacio svg { width: 36px; height: 36px; opacity: .6; }
    .vacio p { font-size: .88rem; color: var(--color-text-muted); max-width: 32ch; }
    .vacio--pagina { margin-top: .5rem; padding: 3rem 1.5rem; }
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
export class DashboardComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  readonly usuario = this.authService.currentUser;

  readonly sesiones = signal<SesionHoy[]>([]);
  readonly cargandoSesiones = signal(false);
  readonly lesionesActivas = signal<number | null>(null);
  readonly estudiantesActivos = signal<number | null>(null);
  readonly alertas = signal<PanelAlertas | null>(null);
  readonly historico = signal<HistoricoIngresos | null>(null);
  readonly mapa = signal<MapaAsistencia | null>(null);

  readonly esOperativo = computed(() => {
    const rol = this.usuario()?.rol;
    return rol === 'ADMINISTRADOR' || rol === 'ENTRENADOR';
  });
  readonly esAdministrador = computed(() => this.usuario()?.rol === 'ADMINISTRADOR');
  readonly esEntrenador = computed(() => this.usuario()?.rol === 'ENTRENADOR');

  readonly horaCorta = horaCorta;

  readonly totalSesiones = computed(() => this.sesiones().length);
  readonly sesionesEnEvaluacion = computed(() => this.sesiones().filter((s) => s.tieneEvaluacion).length);
  readonly sesionesSinIniciar = computed(() => this.totalSesiones() - this.sesionesEnEvaluacion());

  ngOnInit() {
    this.authService.getProfile().subscribe({
      next: () => {
        const rolActual = this.usuario()?.rol;

        const destinoPropio = homeRouteForRole(rolActual);
        if (destinoPropio !== '/dashboard') {
          this.router.navigate([destinoPropio], { replaceUrl: true });
          return;
        }
        if (this.esOperativo()) {
          this.cargarSesionesDeHoy();
          this.cargarConteoDeLesiones();
          this.cargarMapaDeAsistencia();
          if (this.esAdministrador()) {
            this.cargarConteoDeEstudiantes();
            this.cargarAlertas();
            this.cargarHistoricoIngresos();
          }
        }
      },
      error: () => {},
    });
  }

  private cargarSesionesDeHoy(): void {
    this.cargandoSesiones.set(true);
    this.http.get<SesionHoy[]>('/api/sesiones/hoy').subscribe({
      next: (sesiones) => {
        this.sesiones.set(sesiones);
        this.cargandoSesiones.set(false);
      },
      error: () => {
        this.cargandoSesiones.set(false);
      },
    });
  }

  private cargarConteoDeLesiones(): void {
    this.http.get<PaginaLigera>('/api/lesiones?size=1').subscribe({
      next: (pagina) => { this.lesionesActivas.set(pagina.totalElements); },
      error: () => {},
    });
  }

  private cargarConteoDeEstudiantes(): void {
    this.http.get<PaginaLigera>('/api/estudiantes?size=1').subscribe({
      next: (pagina) => { this.estudiantesActivos.set(pagina.totalElements); },
      error: () => {},
    });
  }

  private cargarAlertas(): void {
    this.http.get<PanelAlertas>('/api/alertas').subscribe({
      next: (panel) => this.alertas.set(panel),
      error: () => {},
    });
  }

  private cargarHistoricoIngresos(): void {
    this.http.get<HistoricoIngresos>('/api/pagos/ingresos-historico?meses=6').subscribe({
      next: (serie) => this.historico.set(serie),
      error: () => {},
    });
  }

  private cargarMapaDeAsistencia(): void {
    this.http.get<MapaAsistencia>('/api/asistencias/mapa?dias=98').subscribe({
      next: (m) => this.mapa.set(m),
      error: () => {},
    });
  }

  iniciales(nombre: string): string {
    return inicialesDe(nombre);
  }

  nombreMes(mes: number): string {
    return NOMBRES_MES[mes - 1] ?? String(mes);
  }
}
