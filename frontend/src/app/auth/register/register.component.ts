import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth.service';

/**
 * `loading`/`error` en signals por la misma razón que en LoginComponent:
 * sin zone.js, una propiedad plana mutada en un subscribe no repinta.
 */
@Component({
  selector: 'app-register',
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
          <p class="auth-brand__tagline">Registro de nuevas cuentas para el cuerpo técnico y administrativo de la escuela.</p>

          <ul class="auth-brand__features">
            <li>
              <span class="auth-brand__check">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
              </span>
              Datos verificados por cédula y correo
            </li>
            <li>
              <span class="auth-brand__check">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
              </span>
              Acceso según tu rol dentro del equipo
            </li>
            <li>
              <span class="auth-brand__check">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
              </span>
              Información protegida con sesión segura
            </li>
          </ul>
        </div>
      </aside>

      <main class="auth-panel">
        <form class="auth-card" (ngSubmit)="onSubmit()">
          <h2>Crear una cuenta</h2>
          <p class="auth-card__subtitle">Completa tus datos para registrarte</p>

          <div class="fila-2">
            <label class="field" for="nombre">
              <span class="field__label">Nombre</span>
              <span class="field__control">
                <svg class="field__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
                <input id="nombre" type="text" [(ngModel)]="nombre" name="nombre" placeholder="María" required />
              </span>
            </label>
            <label class="field" for="apellido">
              <span class="field__label">Apellido</span>
              <span class="field__control">
                <svg class="field__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
                <input id="apellido" type="text" [(ngModel)]="apellido" name="apellido" placeholder="Andrade" required />
              </span>
            </label>
          </div>

          <label class="field" for="cedula">
            <span class="field__label">Cédula</span>
            <span class="field__control">
              <svg class="field__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="4" width="22" height="16" rx="2" ry="2"></rect><line x1="1" y1="10" x2="23" y2="10"></line></svg>
              <input id="cedula" type="text" [(ngModel)]="cedula" name="cedula"
                     placeholder="1234567890"
                     required pattern="\\d{10}" inputmode="numeric" maxlength="10" />
            </span>
          </label>

          <label class="field" for="correo">
            <span class="field__label">Correo de contacto</span>
            <span class="field__control">
              <svg class="field__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path><polyline points="22 6 12 13 2 6"></polyline></svg>
              <input id="correo" type="email" [(ngModel)]="correo" name="correo" placeholder="maria@correo.com" required />
            </span>
          </label>

          <label class="field" for="fechaNacimiento">
            <span class="field__label">Fecha de nacimiento</span>
            <span class="field__control">
              <svg class="field__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
              <input id="fechaNacimiento" type="date" [(ngModel)]="fechaNacimiento" name="fechaNacimiento" required />
            </span>
          </label>

          <label class="field" for="username">
            <span class="field__label">Usuario (correo de acceso)</span>
            <span class="field__control">
              <svg class="field__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
              <input id="username" type="email" [(ngModel)]="username" name="username" placeholder="maria@uteq.edu.ec" required />
            </span>
          </label>

          <label class="field" for="password">
            <span class="field__label">Contraseña</span>
            <span class="field__control">
              <svg class="field__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
              <input id="password" [type]="mostrarPassword() ? 'text' : 'password'"
                     [(ngModel)]="password" name="password" placeholder="Mínimo 6 caracteres" required minlength="6" />
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
              <span class="spinner"></span> Registrando…
            } @else {
              Registrarse
            }
          </button>

          <p class="auth-card__footer">¿Ya tienes cuenta? <a routerLink="/login">Iniciar sesión</a></p>
        </form>
      </main>
    </div>
  `,
  styles: [`
    .fila-2 { display: grid; grid-template-columns: 1fr 1fr; gap: .85rem; }
    @media (max-width: 420px) { .fila-2 { grid-template-columns: 1fr; } }
  `]
})
export class RegisterComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  nombre = '';
  apellido = '';
  cedula = '';
  correo = '';
  fechaNacimiento = '';
  username = '';
  password = '';
  readonly loading = signal(false);
  readonly error = signal('');
  readonly mostrarPassword = signal(false);

  onSubmit() {
    if (this.loading()) return;
    this.loading.set(true);
    this.error.set('');
    this.authService.register({
      nombre: this.nombre,
      apellido: this.apellido,
      cedula: this.cedula,
      correo: this.correo,
      fechaNacimiento: this.fechaNacimiento,
      username: this.username,
      password: this.password,
    }).subscribe({
      next: () => {
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(this.mensajeDeError(err.status));
      }
    });
  }

  private mensajeDeError(status: number): string {
    switch (status) {
      case 409: return 'Ya existe una cuenta con ese usuario, cédula o correo';
      case 422: return 'Revisa los datos: la cédula debe tener 10 dígitos y la fecha de nacimiento ser pasada';
      case 403: return 'Solo un administrador puede registrar cuentas';
      default: return 'Error del servidor';
    }
  }
}
