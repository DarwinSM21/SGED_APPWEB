import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../auth.service';
import { PelotaAnimadaComponent } from '../pelota-animada.component';
import { Diagnostico, diagnosticar } from '../../core/diagnostico-error';

/**
 * Pantalla de confirmación de correo (RNF-26 / hallazgo H-09). Toma el token
 * de {@code ?token=} de la URL y lo canjea contra el backend al abrir. Un
 * token ausente/expirado/usado se explica y ofrece volver al inicio de
 * sesión. Mientras el correo no se confirme, el sistema no envía a esa
 * dirección el enlace de restablecimiento de contraseña.
 */
@Component({
  selector: 'app-confirmar-correo',
  standalone: true,
  imports: [CommonModule, RouterLink, PelotaAnimadaComponent],
  template: `
    <div class="auth-shell">
      <aside class="auth-brand">
        <div class="auth-brand__glow auth-brand__glow--a"></div>
        <div class="auth-brand__glow auth-brand__glow--b"></div>
        <app-pelota-animada />
        <div class="auth-brand__content">
          <h1>SGED</h1>
          <p class="auth-brand__tagline">Sistema de Gestión de la Escuela Deportiva</p>
        </div>
      </aside>

      <main class="auth-panel">
        <div class="auth-card">
          @if (!token) {
            <h2>Enlace incompleto</h2>
            <p class="auth-card__subtitle">
              Este enlace no trae un token válido. Revisa el correo de confirmación
              o pide que se te reenvíe.
            </p>
            <a class="btn btn--primary btn--block" routerLink="/login">Ir al inicio de sesión</a>
          } @else if (estado() === 'confirmando') {
            <h2>Confirmando tu correo…</h2>
            <p class="auth-card__subtitle"><span class="spinner"></span> Un momento.</p>
          } @else if (estado() === 'ok') {
            <h2>Correo confirmado</h2>
            <p class="auth-card__subtitle">
              Tu dirección quedó verificada. Ya puedes usar el restablecimiento de
              contraseña si lo necesitas.
            </p>
            <a class="btn btn--primary btn--block" routerLink="/login">Ir al inicio de sesión</a>
          } @else {
            <h2>No se pudo confirmar</h2>
            <div class="alert alert--danger" role="alert" aria-live="assertive">
              <span class="fallo-que">{{ fallo()?.mensaje }}</span>
              @if (fallo()?.sugerencia) { <span class="fallo-como">{{ fallo()?.sugerencia }}</span> }
              <span class="fallo-como">El enlace pudo haber expirado o ya haberse usado.</span>
            </div>
            <a class="btn btn--primary btn--block" routerLink="/login">Ir al inicio de sesión</a>
          }
        </div>
      </main>
    </div>
  `,
})
export class ConfirmarCorreoComponent implements OnInit {
  private readonly authService = inject(AuthService);

  readonly token = inject(ActivatedRoute).snapshot.queryParamMap.get('token') ?? '';

  readonly estado = signal<'confirmando' | 'ok' | 'error'>('confirmando');
  readonly fallo = signal<Diagnostico | null>(null);

  ngOnInit() {
    if (!this.token) {
      return;
    }
    this.authService.confirmarCorreo(this.token).subscribe({
      next: () => this.estado.set('ok'),
      error: (err) => {
        this.fallo.set(diagnosticar(err));
        this.estado.set('error');
      },
    });
  }
}
