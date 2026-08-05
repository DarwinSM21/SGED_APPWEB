import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth.service';
import { homeRouteForRole } from '../home-route';

/**
 * `loading`/`error` viven en signals, no en propiedades sueltas: este
 * proyecto no incluye zone.js, asi que una propiedad plana mutada dentro de
 * un subscribe de HttpClient no garantiza un repintado. `username`/`password`
 * si pueden quedarse como propiedades normales porque `[(ngModel)]` las
 * actualiza a traves de su propio manejador de evento instrumentado.
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="auth-shell">
      <aside class="auth-brand">
        <div class="auth-brand__glow auth-brand__glow--a"></div>
        <div class="auth-brand__glow auth-brand__glow--b"></div>
        <div class="auth-brand__content">
          <div class="auth-brand__logo">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="5"></circle>
              <path d="M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9l2.1 2.1M17 17l2.1 2.1M19.1 4.9 17 7M7 17l-2.1 2.1"></path>
            </svg>
          </div>
          <h1>SGED</h1>
          <p class="auth-brand__tagline">Sistema de Gestión de la Escuela Deportiva — control diario del entrenador, un solo lugar.</p>

          <ul class="auth-brand__features">
            <li>
              <span class="auth-brand__check">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
              </span>
              Asistencia por código QR, sin fuga de datos
            </li>
            <li>
              <span class="auth-brand__check">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
              </span>
              Evaluación diaria por criterios
            </li>
            <li>
              <span class="auth-brand__check">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
              </span>
              Formación sugerida y feedback con IA
            </li>
            <li>
              <span class="auth-brand__check">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
              </span>
              Seguimiento de lesiones
            </li>
          </ul>
        </div>
      </aside>

      <main class="auth-panel">
        <form class="auth-card" (ngSubmit)="onSubmit()">
          <h2>Bienvenido de nuevo</h2>
          <p class="auth-card__subtitle">Inicia sesión con tu cuenta para continuar</p>

          <label class="field" for="username">
            <span class="field__label">Usuario</span>
            <span class="field__control">
              <svg class="field__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle>
              </svg>
              <input id="username" type="text" [(ngModel)]="username" name="username"
                     placeholder="tu.usuario@uteq.edu.ec"
                     required autocomplete="username" autofocus />
            </span>
          </label>

          <label class="field" for="password">
            <span class="field__label">Contraseña</span>
            <span class="field__control">
              <svg class="field__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
              </svg>
              <input id="password" [type]="mostrarPassword() ? 'text' : 'password'"
                     [(ngModel)]="password" name="password" placeholder="••••••••"
                     required autocomplete="current-password" />
              <button class="field__toggle" type="button" (click)="mostrarPassword.set(!mostrarPassword())"
                      [attr.aria-label]="mostrarPassword() ? 'Ocultar contraseña' : 'Mostrar contraseña'">
                @if (mostrarPassword()) {
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.94 10.94 0 0 1 12 20c-7 0-11-8-11-8a18.5 18.5 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path><line x1="1" y1="1" x2="23" y2="23"></line></svg>
                } @else {
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
                }
              </button>
            </span>
          </label>

          @if (error()) {
            <div class="alert alert--danger" role="alert" aria-live="assertive">{{ error() }}</div>
          }

          <button class="btn btn--primary btn--block" type="submit" [disabled]="loading()">
            @if (loading()) {
              <span class="spinner"></span> Ingresando…
            } @else {
              Iniciar sesión
            }
          </button>

          <p class="auth-card__footer">¿No tienes cuenta? <a routerLink="/registro">Regístrate</a></p>
        </form>
      </main>
    </div>
  `,
})
export class LoginComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  username = '';
  password = '';
  readonly loading = signal(false);
  readonly error = signal('');
  readonly mostrarPassword = signal(false);

  onSubmit() {
    if (this.loading()) {
      // El backend cuenta intentos fallidos por IP (LoginAttemptService,
      // OWASP A07); un doble clic no debe consumir dos de los seis
      // intentos disponibles antes del bloqueo.
      return;
    }
    this.loading.set(true);
    this.error.set('');

    const username = this.username.trim();
    this.authService.login({ username, password: this.password }).subscribe({
      next: (usuario) => {
        this.router.navigate([homeRouteForRole(usuario.rol)]);
      },
      error: (err) => {
        this.loading.set(false);
        this.password = '';
        this.error.set(this.mensajeDeError(err));
      }
    });
  }

  private mensajeDeError(err: { status: number; error?: { detail?: string } }): string {
    switch (err.status) {
      case 401:
        return 'Usuario o contraseña incorrectos';
      case 429:
        // El backend ya redacta el mensaje ("Intenta de nuevo en 15
        // minutos"); mostrarlo tal cual evita duplicar ese texto aquí.
        return err.error?.detail ?? 'Demasiados intentos. Intenta más tarde';
      case 0:
        return 'No hay conexión con el servidor';
      default:
        return 'Error del servidor. Intenta de nuevo';
    }
  }
}
