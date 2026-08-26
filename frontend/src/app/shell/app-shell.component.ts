import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { SwUpdate } from '@angular/service-worker';
import { inicialesDe } from '../core/formato-texto';
import { relojEn12 } from '../core/formato-fecha';

type Icono = 'inicio' | 'usuario-mas' | 'qr' | 'familia' | 'calendario' | 'pago' | 'inventario'
  | 'capas' | 'escudo' | 'auditoria' | 'reporte' | 'configuracion' | 'balon';

interface NavItem {
  etiqueta: string;
  ruta: string;
  icono: Icono;
}

const CLAVE_COLAPSADA = 'sged.sidebar.colapsada';
/** Debe coincidir con el @media (max-width: ...) del bloque de estilos. */
const BREAKPOINT_MOVIL_PX = 680;

/**
 * Entradas de navegacion por rol. Deliberadamente solo destinos reales:
 * nada de Calendario/Reportes/Partidos (sin backend todavia, ver memoria
 * del proyecto) ni un listado de representantes (el backend ya tiene el
 * CRUD, pero esta iteracion solo construyo la pantalla de alta,
 * encadenada desde "Crear usuario").
 */
const NAV_POR_ROL: Record<string, NavItem[]> = {
  ADMINISTRADOR: [
    { etiqueta: 'Inicio', ruta: '/dashboard', icono: 'inicio' },
    { etiqueta: 'Personas', ruta: '/personas', icono: 'usuario-mas' },
    { etiqueta: 'Categorías', ruta: '/categorias', icono: 'capas' },
    { etiqueta: 'Recepción', ruta: '/recepcion', icono: 'qr' },
    { etiqueta: 'Sesiones', ruta: '/entrenador/sesiones', icono: 'calendario' },
    { etiqueta: 'Partidos', ruta: '/partidos', icono: 'balon' },
    { etiqueta: 'Pagos', ruta: '/pagos', icono: 'pago' },
    { etiqueta: 'Inventario', ruta: '/inventario', icono: 'inventario' },
    { etiqueta: 'Reportes', ruta: '/reportes', icono: 'reporte' },
    { etiqueta: 'Consentimientos', ruta: '/admin/consentimientos', icono: 'escudo' },
    { etiqueta: 'Auditorías', ruta: '/admin/auditorias', icono: 'auditoria' },
  ],
  ENTRENADOR: [
    { etiqueta: 'Inicio', ruta: '/dashboard', icono: 'inicio' },
    { etiqueta: 'Mis sesiones', ruta: '/entrenador/sesiones', icono: 'calendario' },
    { etiqueta: 'Partidos', ruta: '/partidos', icono: 'balon' },
    { etiqueta: 'Categorías', ruta: '/categorias', icono: 'capas' },
    { etiqueta: 'Inventario', ruta: '/inventario', icono: 'inventario' },
    { etiqueta: 'Reportes', ruta: '/reportes', icono: 'reporte' },
  ],
  USER: [
    { etiqueta: 'Inicio', ruta: '/dashboard', icono: 'inicio' },
  ],
  RECEPCIONISTA: [
    { etiqueta: 'Mostrar QR', ruta: '/recepcion', icono: 'qr' },
    { etiqueta: 'Personas', ruta: '/personas', icono: 'familia' },
    { etiqueta: 'Pagos', ruta: '/pagos', icono: 'pago' },
    { etiqueta: 'Inventario', ruta: '/inventario', icono: 'inventario' },
    { etiqueta: 'Reportes', ruta: '/reportes', icono: 'reporte' },
  ],
  REPRESENTANTE: [
    { etiqueta: 'Mis representados', ruta: '/representante', icono: 'familia' },
  ],
  ESTUDIANTE: [
    { etiqueta: 'Marcar asistencia', ruta: '/estudiante/marcar-asistencia', icono: 'qr' },
    { etiqueta: 'Mi historial', ruta: '/estudiante/mi-historial', icono: 'calendario' },
    { etiqueta: 'Mi equipo', ruta: '/estudiante/mi-equipo', icono: 'familia' },
  ],
};

/**
 * Shell de toda pantalla autenticada: sidebar con marca/navegacion/usuario
 * a la izquierda, contenido de la ruta activa a la derecha via
 * router-outlet. Reemplaza las cabeceras sueltas que Dashboard y
 * EvaluacionDiaria dibujaban cada una por su lado.
 *
 * La sidebar se puede recoger a un carril de solo iconos con el boton de
 * flechas (util en pantallas anchas cuando se quiere mas espacio para el
 * contenido); la preferencia se recuerda en localStorage. Por debajo de
 * 680px se recoge sola via CSS sin importar esta preferencia -en un celular
 * no sobra espacio para negociar-.
 */
@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <div class="shell">
      @if (menuMovilAbierto()) {
        <div class="fondo-movil" (click)="cerrarMenuMovil()"></div>
      }
      <aside class="sidebar" [class.sidebar--colapsada]="colapsada()" [class.sidebar--movil-abierta]="menuMovilAbierto()">
        <div class="sidebar__marca">
          <span class="sidebar__logo">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="5"></circle>
              <path d="M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9l2.1 2.1M17 17l2.1 2.1M19.1 4.9 17 7M7 17l-2.1 2.1"></path>
            </svg>
          </span>
          <span class="sidebar__nombre">SGED</span>
        </div>

        <nav class="sidebar__nav">
          @for (item of navItems(); track item.ruta) {
            <a class="sidebar__item" [routerLink]="item.ruta" routerLinkActive="activo" [title]="item.etiqueta" (click)="cerrarMenuMovil()">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                @switch (item.icono) {
                  @case ('inicio') {
                    <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path><polyline points="9 22 9 12 15 12 15 22"></polyline>
                  }
                  @case ('usuario-mas') {
                    <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><line x1="19" y1="8" x2="19" y2="14"></line><line x1="22" y1="11" x2="16" y2="11"></line>
                  }
                  @case ('qr') {
                    <rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect>
                  }
                  @case ('familia') {
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                  }
                  @case ('calendario') {
                    <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line>
                  }
                  @case ('pago') {
                    <rect x="2" y="5" width="20" height="14" rx="2" ry="2"></rect><line x1="2" y1="10" x2="22" y2="10"></line>
                  }
                  @case ('inventario') {
                    <path d="M21 8 12 3 3 8l9 5 9-5Z"></path><path d="M3 8v8l9 5 9-5V8"></path><path d="M12 13v8"></path>
                  }
                  @case ('capas') {
                    <path d="m12 2 9 5-9 5-9-5 9-5Z"></path><path d="m3 12 9 5 9-5"></path><path d="m3 17 9 5 9-5"></path>
                  }
                  @case ('escudo') {
                    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10Z"></path><path d="m9 12 2 2 4-4"></path>
                  }
                  @case ('auditoria') {
                    <path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"></path><rect x="8" y="2" width="8" height="4" rx="1" ry="1"></rect><path d="M9 13l2 2 4-4"></path>
                  }
                  @case ('reporte') {
                    <line x1="12" y1="20" x2="12" y2="10"></line><line x1="18" y1="20" x2="18" y2="4"></line><line x1="6" y1="20" x2="6" y2="16"></line>
                  }
                  @case ('balon') {
                    <circle cx="12" cy="12" r="10"></circle><path d="m12 7 4.7 3.4-1.8 5.5H9.1l-1.8-5.5Z"></path><path d="M12 2v5M2.6 9.2l4.7 3.4M21.4 9.2l-4.7 3.4M6.3 20.3l2.8-4.4M17.7 20.3l-2.8-4.4"></path>
                  }
                  @case ('configuracion') {
                    <circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
                  }
                }
              </svg>
              <span class="sidebar__etiqueta">{{ item.etiqueta }}</span>
            </a>
          }

          <a class="sidebar__item" routerLink="/configuracion" routerLinkActive="activo" title="Configuración" (click)="cerrarMenuMovil()">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
            </svg>
            <span class="sidebar__etiqueta">Configuración</span>
          </a>
        </nav>

        <button class="sidebar__colapsar" type="button" (click)="alternarColapso()"
                [attr.aria-label]="menuMovilAbierto() ? 'Cerrar menú' : (colapsada() ? 'Expandir menú' : 'Recoger menú')"
                [title]="menuMovilAbierto() ? 'Cerrar menú' : (colapsada() ? 'Expandir menú' : 'Recoger menú')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            @if (menuMovilAbierto()) {
              <line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line>
            } @else if (colapsada()) {
              <polyline points="13 17 18 12 13 7"></polyline><polyline points="6 17 11 12 6 7"></polyline>
            } @else {
              <polyline points="11 17 6 12 11 7"></polyline><polyline points="18 17 13 12 18 7"></polyline>
            }
          </svg>
          <span class="sidebar__etiqueta">{{ menuMovilAbierto() ? 'Cerrar menú' : 'Recoger menú' }}</span>
        </button>

        @if (usuario(); as u) {
          <div class="sidebar__usuario">
            <span class="avatar">{{ iniciales(u.nombre) }}</span>
            <span class="sidebar__usuario-texto">
              <span class="nombre">{{ u.nombre }}</span>
              <span class="rol">{{ u.rol }}</span>
            </span>
            <button class="btn btn--ghost btn--icono" (click)="logout()" aria-label="Cerrar sesión" title="Cerrar sesión">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
            </button>
          </div>
        }
      </aside>

      <main class="shell__contenido">
        <header class="topbar">
          <div class="topbar__saludo">
            <span class="topbar__hola">{{ saludo() }}@if (primerNombre()) {, {{ primerNombre() }}}</span>
            <span class="topbar__fecha">{{ fechaHoy() }}</span>
          </div>
          <span class="topbar__reloj">{{ horaActual() }}</span>
        </header>
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
  styles: [`
    .shell { display: flex; min-height: 100vh; }

    .sidebar {
      width: 240px; flex-shrink: 0; display: flex; flex-direction: column;
      background: var(--color-surface); border-right: 1px solid var(--color-border);
      padding: 1.25rem 1rem; position: sticky; top: 0;
      /* 100vh en celular no descuenta la barra de direcciones del navegador,
         asi que el bloque de usuario/cerrar sesion -al final de la columna-
         quedaba fuera de la pantalla visible sin forma de hacer scroll hasta
         el. dvh sigue el viewport visible real; overflow-y auto es el
         respaldo para cuando el contenido igual no entra (rol con muchos
         items de navegacion + pantalla corta). */
      height: 100vh; height: 100dvh; overflow-y: auto;
      transition: width var(--transition);
    }
    .sidebar__marca { display: flex; align-items: center; gap: .65rem; margin-bottom: 1.75rem; padding: 0 .4rem; }
    .sidebar__logo {
      width: 36px; height: 36px; border-radius: var(--radius-sm); background: var(--gradient-primary);
      color: #fff; display: flex; align-items: center; justify-content: center; flex-shrink: 0;
    }
    .sidebar__logo svg { width: 20px; height: 20px; }
    .sidebar__nombre { font-weight: 700; letter-spacing: .03em; white-space: nowrap; }

    .sidebar__nav { display: flex; flex-direction: column; gap: .25rem; flex: 1; }
    .sidebar__item {
      display: flex; align-items: center; gap: .75rem; padding: .65rem .75rem; border-radius: var(--radius-sm);
      color: var(--color-text-muted); text-decoration: none; font-size: .9rem; font-weight: 600;
      transition: background var(--transition), color var(--transition);
    }
    .sidebar__item svg { width: 19px; height: 19px; flex-shrink: 0; }
    .sidebar__item:hover { background: var(--color-border-light); color: var(--color-text); }
    .sidebar__item.activo { background: var(--color-primary-50); color: var(--color-primary-700); }

    .sidebar__colapsar {
      display: flex; align-items: center; gap: .75rem; padding: .6rem .75rem; margin-top: .5rem;
      border: none; background: none; border-radius: var(--radius-sm); cursor: pointer;
      color: var(--color-text-faint); font-size: .82rem; font-weight: 600; width: 100%;
      transition: background var(--transition), color var(--transition);
    }
    .sidebar__colapsar:hover { background: var(--color-border-light); color: var(--color-text-muted); }
    .sidebar__colapsar svg { width: 17px; height: 17px; flex-shrink: 0; }

    .sidebar__usuario { display: flex; align-items: center; gap: .6rem; padding-top: 1rem; border-top: 1px solid var(--color-border-light); }
    .sidebar__usuario-texto { display: flex; flex-direction: column; flex: 1; min-width: 0; line-height: 1.25; }
    .sidebar__usuario-texto .nombre { font-size: .82rem; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .sidebar__usuario-texto .rol { font-size: .72rem; color: var(--color-text-faint); text-transform: capitalize; }
    .btn--icono { padding: .5rem; flex-shrink: 0; }
    .btn--icono svg { width: 17px; height: 17px; }

    .shell__contenido { flex: 1; min-width: 0; }

    .topbar {
      display: flex; align-items: center; justify-content: space-between; gap: 1rem;
      padding: 1.1rem 1.5rem; border-bottom: 1px solid var(--color-border-light);
    }
    .topbar__saludo { display: flex; flex-direction: column; gap: .1rem; min-width: 0; }
    .topbar__hola { font-weight: 700; font-size: 1.05rem; }
    .topbar__fecha { font-size: .8rem; color: var(--color-text-muted); }
    .topbar__fecha::first-letter { text-transform: uppercase; }
    .topbar__reloj {
      font-variant-numeric: tabular-nums; font-size: 1.1rem; font-weight: 600;
      color: var(--color-primary-700); background: var(--color-primary-50);
      padding: .4rem .8rem; border-radius: var(--radius-sm); flex-shrink: 0;
    }
    @media (max-width: 480px) {
      .topbar { padding: .9rem 1rem; flex-wrap: wrap; }
      .topbar__hola { font-size: .92rem; }
    }

    /* Recogida manual con el boton de flechas: mismo look que el breakpoint de abajo.
       Selector compuesto (.sidebar.sidebar--colapsada) a proposito: necesita mas
       especificidad que la regla base .sidebar para ganarle sin depender del orden
       de insercion de los <style> por componente, que Angular no garantiza estable. */
    .sidebar.sidebar--colapsada { width: 72px; padding: 1.1rem .5rem; align-items: center; }
    .sidebar--colapsada .sidebar__marca { justify-content: center; padding: 0; }
    .sidebar--colapsada .sidebar__nombre,
    .sidebar--colapsada .sidebar__etiqueta,
    .sidebar--colapsada .sidebar__usuario-texto { display: none; }
    .sidebar--colapsada .sidebar__item,
    .sidebar--colapsada .sidebar__colapsar { justify-content: center; padding: .7rem; }
    .sidebar--colapsada .sidebar__usuario { flex-direction: column; gap: .5rem; }

    /* Recogida automatica en pantallas angostas, sin importar la preferencia guardada. */
    @media (max-width: 680px) {
      .sidebar { width: 72px; padding: 1.1rem .5rem; align-items: center; }
      .sidebar__marca { justify-content: center; padding: 0; }
      .sidebar__nombre, .sidebar__etiqueta, .sidebar__usuario-texto { display: none; }
      .sidebar__item, .sidebar__colapsar { justify-content: center; padding: .7rem; }
      .sidebar__usuario { flex-direction: column; gap: .5rem; }

      /* El boton "Recoger menu" del riel se convierte en el disparador del
         cajon movil: en vez de solo iconos, muestra el sidebar completo
         -con "Cerrar sesion" legible- superpuesto sobre el contenido, con
         un fondo oscuro para cerrarlo tocando afuera. Es la unica forma de
         llegar al logout en un celular sin adivinar que icono es cual.
         Selector compuesto ".sidebar.sidebar--movil-abierta" a proposito
         (mismo motivo que ".sidebar.sidebar--colapsada" mas arriba): mas
         especificidad que la regla ".sidebar" de este mismo media query
         para ganarle sin depender del orden de insercion. */
      .sidebar.sidebar--movil-abierta {
        position: fixed !important; inset: 0 auto 0 0; width: 240px !important; max-width: 80vw;
        align-items: stretch; padding: 1.25rem 1rem; z-index: 60;
        box-shadow: 8px 0 32px rgba(0, 0, 0, .35);
      }
      .sidebar.sidebar--movil-abierta .sidebar__marca { justify-content: flex-start; padding: 0 .4rem; }
      .sidebar.sidebar--movil-abierta .sidebar__nombre,
      .sidebar.sidebar--movil-abierta .sidebar__etiqueta,
      .sidebar.sidebar--movil-abierta .sidebar__usuario-texto { display: initial; }
      .sidebar.sidebar--movil-abierta .sidebar__item,
      .sidebar.sidebar--movil-abierta .sidebar__colapsar { justify-content: flex-start; padding: .65rem .75rem; }
      .sidebar.sidebar--movil-abierta .sidebar__usuario { flex-direction: row; }

      .fondo-movil {
        position: fixed; inset: 0; background: rgba(0, 0, 0, .4); z-index: 55;
      }
    }
  `]
})
export class AppShellComponent implements OnInit, OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly actualizaciones = inject(SwUpdate);

  readonly usuario = this.authService.currentUser;
  readonly navItems = computed<NavItem[]>(() => NAV_POR_ROL[this.usuario()?.rol ?? ''] ?? []);

  readonly colapsada = signal(localStorage.getItem(CLAVE_COLAPSADA) === 'true');
  /**
   * Cajon movil: independiente de `colapsada` (preferencia de escritorio,
   * persistida). Este es transitorio -no se guarda- y solo aplica bajo
   * BREAKPOINT_MOVIL_PX; en escritorio el mismo boton sigue alternando
   * `colapsada` como siempre.
   */
  readonly menuMovilAbierto = signal(false);

  /** Reloj en vivo del topbar: un signal con la hora actual, refrescado cada segundo. */
  readonly ahora = signal(new Date());
  private intervaloReloj?: ReturnType<typeof setInterval>;

  readonly primerNombre = computed(() => this.usuario()?.nombre?.split(' ')[0] ?? '');
  readonly horaActual = computed(() =>
    relojEn12(this.ahora()));
  readonly fechaHoy = computed(() =>
    this.ahora().toLocaleDateString('es-EC', { weekday: 'long', day: 'numeric', month: 'long' }));
  readonly saludo = computed(() => {
    const hora = this.ahora().getHours();
    if (hora < 12) return 'Buenos días';
    if (hora < 19) return 'Buenas tardes';
    return 'Buenas noches';
  });

  ngOnInit(): void {
    this.vigilarActualizaciones();
    this.intervaloReloj = setInterval(() => this.ahora.set(new Date()), 1000);
  }

  /**
   * Recarga sola cuando hay una version nueva desplegada.
   *
   * <p>Sin esto, una pestaña abierta se queda con el index.html que cacheo el
   * service worker y sigue pidiendo archivos de codigo que el servidor ya no
   * tiene: fallan con 404 (NG05604) y partes de la pantalla dejan de
   * responder sin ningun aviso, que es de lo peor que puede pasar en una
   * demostracion. La actualizacion se aplica al momento en vez de esperar a
   * que se cierren todas las pestañas, que es el comportamiento por defecto.
   */
  private vigilarActualizaciones(): void {
    if (!this.actualizaciones.isEnabled) {
      return;
    }
    this.actualizaciones.versionUpdates.subscribe((evento) => {
      if (evento.type === 'VERSION_READY') {
        this.actualizaciones.activateUpdate().then(() => document.location.reload());
      }
    });
  }

  ngOnDestroy(): void {
    clearInterval(this.intervaloReloj);
  }

  iniciales(nombre: string): string {
    return inicialesDe(nombre);
  }

  alternarColapso(): void {
    if (window.innerWidth <= BREAKPOINT_MOVIL_PX) {
      this.menuMovilAbierto.update(v => !v);
      return;
    }
    const nuevoValor = !this.colapsada();
    this.colapsada.set(nuevoValor);
    localStorage.setItem(CLAVE_COLAPSADA, String(nuevoValor));
  }

  cerrarMenuMovil(): void {
    this.menuMovilAbierto.set(false);
  }

  logout() {
    this.authService.logout().subscribe();
  }
}
