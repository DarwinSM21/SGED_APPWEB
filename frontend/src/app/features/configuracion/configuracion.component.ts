import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { descargarBlob } from '../../core/descargar-archivo';
import { Fuente, TamanoFuente, ThemeService } from '../../core/theme.service';

type Seccion = 'apariencia' | 'acerca-de' | 'privacidad' | 'mis-datos';

/** Configuración: apariencia (tema/fuente), acerca de, política de privacidad y exportar "mis datos". Visible para los 5 roles. */
@Component({
  selector: 'app-configuracion',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="pantalla">
      <div class="encabezado">
        <h1 class="titulo-pantalla">Configuración</h1>
        <p class="subtitulo-pantalla">Preferencias de tu cuenta y del sistema.</p>
      </div>

      <div class="layout">
        <nav class="tabs">
          <button type="button" class="tab" [class.tab--activa]="seccion() === 'apariencia'" (click)="seccion.set('apariencia')">Apariencia</button>
          <button type="button" class="tab" [class.tab--activa]="seccion() === 'acerca-de'" (click)="seccion.set('acerca-de')">Acerca de</button>
          <button type="button" class="tab" [class.tab--activa]="seccion() === 'privacidad'" (click)="seccion.set('privacidad')">Política de privacidad</button>
          <button type="button" class="tab" [class.tab--activa]="seccion() === 'mis-datos'" (click)="seccion.set('mis-datos')">Mis datos</button>
        </nav>

        <div class="card contenido">
          @switch (seccion()) {
            @case ('apariencia') {
              <h2 class="titulo-card">Apariencia</h2>

              <div class="grupo">
                <span class="grupo__etiqueta">Tema</span>
                <div class="segmentado">
                  <button type="button" class="segmento" [class.segmento--activo]="theme.tema() === 'claro'" (click)="theme.establecerTema('claro')">Claro</button>
                  <button type="button" class="segmento" [class.segmento--activo]="theme.tema() === 'oscuro'" (click)="theme.establecerTema('oscuro')">Oscuro</button>
                </div>
              </div>

              <div class="grupo">
                <span class="grupo__etiqueta">Tipo de fuente</span>
                <div class="segmentado">
                  <button type="button" class="segmento" [class.segmento--activo]="theme.fuente() === 'sans'" (click)="cambiarFuente('sans')">Predeterminada</button>
                  <button type="button" class="segmento" [class.segmento--activo]="theme.fuente() === 'serif'" (click)="cambiarFuente('serif')">Serif</button>
                  <button type="button" class="segmento" [class.segmento--activo]="theme.fuente() === 'mono'" (click)="cambiarFuente('mono')">Monoespaciada</button>
                </div>
              </div>

              <div class="grupo">
                <span class="grupo__etiqueta">Tamaño de fuente</span>
                <div class="segmentado">
                  <button type="button" class="segmento" [class.segmento--activo]="theme.tamanoFuente() === 'normal'" (click)="cambiarTamano('normal')">Normal</button>
                  <button type="button" class="segmento" [class.segmento--activo]="theme.tamanoFuente() === 'grande'" (click)="cambiarTamano('grande')">Grande</button>
                  <button type="button" class="segmento" [class.segmento--activo]="theme.tamanoFuente() === 'extra-grande'" (click)="cambiarTamano('extra-grande')">Extra grande</button>
                </div>
              </div>
            }

            @case ('acerca-de') {
              <h2 class="titulo-card">Acerca de SGED</h2>
              <p class="parrafo">
                SGED (Sistema de Gestión Deportiva) administra estudiantes, categorías, asistencia,
                evaluaciones, lesiones, pagos e inventario de una escuela deportiva.
              </p>
              <dl class="lista-datos">
                <div><dt>Versión</dt><dd>1.0.0</dd></div>
                <div><dt>Backend</dt><dd>Spring Boot 3 · Java 21 · PostgreSQL</dd></div>
                <div><dt>Frontend</dt><dd>Angular 21</dd></div>
                <div><dt>Soporte</dt><dd>Contacta a un administrador del sistema</dd></div>
              </dl>
            }

            @case ('privacidad') {
              <h2 class="titulo-card">Política de privacidad</h2>
              <div class="parrafo politica">
                <p>
                  SGED recopila y almacena datos personales de estudiantes, representantes,
                  entrenadores, recepcionistas y administradores con el único fin de gestionar
                  las actividades deportivas, académicas y administrativas de la institución:
                  identificación (nombre, cédula, correo, teléfono, fecha de nacimiento),
                  datos deportivos (categoría, asistencia, evaluaciones, lesiones) y datos
                  de pagos.
                </p>
                <p>
                  <strong>Datos de salud.</strong> El registro de lesiones constituye un dato
                  sensible. Solo entrenadores y administradores pueden registrarlo y consultarlo;
                  se usa exclusivamente para decisiones deportivas (aptitud para entrenar) y
                  notificación al representante correspondiente.
                </p>
                <p>
                  <strong>Conservación y seguridad.</strong> Los datos se conservan mientras la
                  persona mantenga una relación activa con la institución, protegidos con
                  autenticación, cifrado en tránsito y control de acceso por rol. Todo cambio
                  queda registrado en el módulo de Auditorías.
                </p>
                <p>
                  <strong>Derechos del titular.</strong> De acuerdo con la Ley Orgánica de
                  Protección de Datos Personales (LOPDP) de Ecuador, puedes solicitar acceso,
                  rectificación o eliminación de tus datos personales. Puedes descargar una copia
                  de tus propios datos desde la pestaña "Mis datos" de esta pantalla.
                </p>
                <p>
                  <strong>Contacto.</strong> Para ejercer tus derechos o realizar consultas sobre
                  el tratamiento de tus datos, contacta a un administrador del sistema.
                </p>
              </div>
            }

            @case ('mis-datos') {
              <h2 class="titulo-card">Mis datos</h2>
              <p class="parrafo">
                Descarga un PDF con los datos de tu cuenta y de tu ficha personal registrados en el sistema.
              </p>
              @if (error()) { <div class="alert alert--danger">{{ error() }}</div> }
              <button type="button" class="btn btn--primary" [disabled]="descargando()" (click)="descargarMisDatos()">
                @if (descargando()) { <span class="spinner"></span> Generando… } @else { Descargar mis datos en PDF }
              </button>
            }
          }
        </div>
      </div>
    </div>
  `,
  styles: [`
    .pantalla { max-width: 900px; margin: 0 auto; padding: 1.5rem 1.25rem 3rem; display: flex; flex-direction: column; gap: 1.25rem; }
    .encabezado { display: flex; flex-direction: column; gap: .3rem; }
    .titulo-pantalla { font-size: 1.5rem; }
    .subtitulo-pantalla { color: var(--color-text-muted); font-size: .92rem; }

    .layout { display: grid; grid-template-columns: 200px 1fr; gap: 1.25rem; align-items: start; }
    @media (max-width: 720px) { .layout { grid-template-columns: 1fr; } }

    .tabs { display: flex; flex-direction: column; gap: .25rem; }
    .tab {
      text-align: left; border: none; background: none; padding: .65rem .8rem; border-radius: var(--radius-sm);
      color: var(--color-text-muted); font-size: .88rem; font-weight: 600; cursor: pointer;
      transition: background var(--transition), color var(--transition);
    }
    .tab:hover { background: var(--color-border-light); color: var(--color-text); }
    .tab--activa { background: var(--color-primary-50); color: var(--color-primary-700); }
    @media (max-width: 720px) { .tabs { flex-direction: row; flex-wrap: wrap; } }

    .contenido { padding: 1.5rem; display: flex; flex-direction: column; gap: 1.1rem; }
    .titulo-card { font-size: 1.1rem; }

    .grupo { display: flex; flex-direction: column; gap: .5rem; }
    .grupo__etiqueta { font-size: .82rem; font-weight: 600; color: var(--color-text); }
    .segmentado { display: flex; gap: .3rem; background: var(--color-bg); border-radius: var(--radius-sm); padding: .3rem; flex-wrap: wrap; }
    .segmento {
      flex: 1; border: none; background: transparent; border-radius: calc(var(--radius-sm) - 4px);
      padding: .6rem .9rem; font-size: .85rem; font-weight: 600; color: var(--color-text-muted); cursor: pointer;
      transition: background var(--transition), color var(--transition), box-shadow var(--transition);
      white-space: nowrap;
    }
    .segmento--activo { background: var(--gradient-primary); color: #fff; box-shadow: var(--shadow-sm); }

    .parrafo { color: var(--color-text-muted); font-size: .9rem; line-height: 1.6; }
    .politica p { margin-bottom: .9rem; }
    .politica p:last-child { margin-bottom: 0; }
    .politica strong { color: var(--color-text); }

    .lista-datos { display: flex; flex-direction: column; gap: .5rem; }
    .lista-datos > div { display: flex; justify-content: space-between; gap: 1rem; padding: .5rem 0; border-bottom: 1px solid var(--color-border-light); font-size: .88rem; }
    .lista-datos dt { color: var(--color-text-muted); margin: 0; }
    .lista-datos dd { margin: 0; font-weight: 600; text-align: right; }
  `],
})
export class ConfiguracionComponent {
  protected readonly theme = inject(ThemeService);
  private readonly http = inject(HttpClient);

  readonly seccion = signal<Seccion>('apariencia');
  readonly descargando = signal(false);
  readonly error = signal('');

  cambiarFuente(fuente: Fuente): void {
    this.theme.establecerFuente(fuente);
  }

  cambiarTamano(tamano: TamanoFuente): void {
    this.theme.establecerTamanoFuente(tamano);
  }

  descargarMisDatos(): void {
    this.descargando.set(true);
    this.error.set('');
    this.http.get('/api/usuarios/me/datos-pdf', { responseType: 'blob' }).subscribe({
      next: (blob) => {
        descargarBlob(blob, 'mis-datos.pdf');
        this.descargando.set(false);
      },
      error: () => {
        this.descargando.set(false);
        this.error.set('No se pudo generar el PDF de tus datos');
      },
    });
  }
}
