import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { inicialesDe } from '../features/entrenador/plantilla.models';

type Icono = 'inicio' | 'usuario-mas' | 'qr' | 'familia';

interface NavItem {
  etiqueta: string;
  ruta: string;
  icono: Icono;
}

const CLAVE_COLAPSADA = 'sged.sidebar.colapsada';

/**
 * Entradas de navegacion por rol. Deliberadamente solo destinos reales:
 * nada de Pagos/Calendario/Reportes/Partidos (sin backend todavia, ver
 * memoria del proyecto) ni un listado de representantes (el backend ya
 * tiene el CRUD, pero esta iteracion solo construyo la pantalla de alta,
 * encadenada desde "Crear usuario").
 */
const NAV_POR_ROL: Record<string, NavItem[]> = {
  ADMINISTRADOR: [
    { etiqueta: 'Inicio', ruta: '/dashboard', icono: 'inicio' },
    { etiqueta: 'Crear usuario', ruta: '/admin/crear-usuario', icono: 'usuario-mas' },
    { etiqueta: 'Recepción', ruta: '/recepcion', icono: 'qr' },
  ],
  ENTRENADOR: [
    { etiqueta: 'Inicio', ruta: '/dashboard', icono: 'inicio' },
  ],
  USER: [
    { etiqueta: 'Inicio', ruta: '/dashboard', icono: 'inicio' },
  ],
  RECEPCIONISTA: [
    { etiqueta: 'Mostrar QR', ruta: '/recepcion', icono: 'qr' },
  ],
  REPRESENTANTE: [
    { etiqueta: 'Mis representados', ruta: '/representante', icono: 'familia' },
  ],
  ESTUDIANTE: [
    { etiqueta: 'Marcar asistencia', ruta: '/estudiante/marcar-asistencia', icono: 'qr' },
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
      <aside class="sidebar" [class.sidebar--colapsada]="colapsada()">
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
            <a class="sidebar__item" [routerLink]="item.ruta" routerLinkActive="activo" [title]="item.etiqueta">
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
                }
              </svg>
              <span class="sidebar__etiqueta">{{ item.etiqueta }}</span>
            </a>
          }
        </nav>

        <button class="sidebar__colapsar" type="button" (click)="alternarColapso()"
                [attr.aria-label]="colapsada() ? 'Expandir menú' : 'Recoger menú'"
                [title]="colapsada() ? 'Expandir menú' : 'Recoger menú'">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            @if (colapsada()) {
              <polyline points="13 17 18 12 13 7"></polyline><polyline points="6 17 11 12 6 7"></polyline>
            } @else {
              <polyline points="11 17 6 12 11 7"></polyline><polyline points="18 17 13 12 18 7"></polyline>
            }
          </svg>
          <span class="sidebar__etiqueta">Recoger menú</span>
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
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
  styles: [`
    .shell { display: flex; min-height: 100vh; }

    .sidebar {
      width: 240px; flex-shrink: 0; display: flex; flex-direction: column;
      background: var(--color-surface); border-right: 1px solid var(--color-border);
      padding: 1.25rem 1rem; position: sticky; top: 0; height: 100vh;
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
    }
  `]
})
export class AppShellComponent {
  private readonly authService = inject(AuthService);

  readonly usuario = this.authService.currentUser;
  readonly navItems = computed<NavItem[]>(() => NAV_POR_ROL[this.usuario()?.rol ?? ''] ?? []);

  readonly colapsada = signal(localStorage.getItem(CLAVE_COLAPSADA) === 'true');

  iniciales(nombre: string): string {
    return inicialesDe(nombre);
  }

  alternarColapso(): void {
    const nuevoValor = !this.colapsada();
    this.colapsada.set(nuevoValor);
    localStorage.setItem(CLAVE_COLAPSADA, String(nuevoValor));
  }

  logout() {
    this.authService.logout().subscribe();
  }
}
