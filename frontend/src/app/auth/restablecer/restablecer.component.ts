import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth.service';
import { PelotaAnimadaComponent } from '../pelota-animada.component';
import { Diagnostico, diagnosticar } from '../../core/diagnostico-error';

/**
 * Pantalla de restablecimiento (RF-37). Toma el token de {@code ?token=} de
 * la URL, pide la contraseña nueva dos veces y valida en vivo la política
 * (RNF-14: 8+ caracteres, con letra y dígito). Al terminar redirige a
 * /login. Un token ausente/expirado/usado devuelve al flujo de recuperación.
 */
@Component({
  selector: 'app-restablecer',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, PelotaAnimadaComponent],
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
        @if (!token) {
          <div class="auth-card">
            <h2>Enlace incompleto</h2>
            <p class="auth-card__subtitle">
              Este enlace no trae un token válido. Solicita uno nuevo.
            </p>
            <a class="btn btn--primary btn--block" routerLink="/recuperar">Solicitar un enlace nuevo</a>
          </div>
        } @else {
          <form class="auth-card" (ngSubmit)="onSubmit()">
            <h2>Elige una contraseña nueva</h2>
            <p class="auth-card__subtitle">Debe tener al menos 8 caracteres, con una letra y un dígito.</p>

            <label class="field" for="password">
              <span class="field__label">Contraseña nueva</span>
              <span class="field__control">
                <input id="password" [type]="mostrar() ? 'text' : 'password'" name="password"
                       [(ngModel)]="password" required autocomplete="new-password" />
                <button class="field__toggle" type="button" (click)="mostrar.set(!mostrar())"
                        [attr.aria-label]="mostrar() ? 'Ocultar contraseña' : 'Mostrar contraseña'">
                  {{ mostrar() ? '🙈' : '👁' }}
                </button>
              </span>
            </label>

            <label class="field" for="confirmar">
              <span class="field__label">Repite la contraseña</span>
              <span class="field__control">
                <input id="confirmar" [type]="mostrar() ? 'text' : 'password'" name="confirmar"
                       [(ngModel)]="confirmar" required autocomplete="new-password" />
              </span>
            </label>

            <ul class="auth-card__reglas">
              <li [class.ok]="reglaLongitud()">Al menos 8 caracteres</li>
              <li [class.ok]="reglaLetra()">Contiene una letra</li>
              <li [class.ok]="reglaDigito()">Contiene un dígito</li>
              <li [class.ok]="reglaCoincide()">Las dos contraseñas coinciden</li>
            </ul>

            @if (fallo(); as f) {
              <div class="alert alert--danger" role="alert" aria-live="assertive">
                <span class="fallo-que">{{ f.mensaje }}</span>
                @if (f.sugerencia) { <span class="fallo-como">{{ f.sugerencia }}</span> }
                @if (tokenRoto()) {
                  <span class="fallo-como"><a routerLink="/recuperar">Solicitar un enlace nuevo</a></span>
                }
              </div>
            }

            <button class="btn btn--primary btn--block" type="submit"
                    [disabled]="loading() || !formularioValido()">
              @if (loading()) { <span class="spinner"></span> Guardando… }
              @else { Guardar contraseña }
            </button>
          </form>
        }
      </main>
    </div>
  `,
  styles: [`
    .auth-card__reglas { list-style: none; padding: 0; margin: .25rem 0 1rem; font-size: .85rem; color: var(--text-muted, #6b7280); }
    .auth-card__reglas li::before { content: '○ '; }
    .auth-card__reglas li.ok { color: var(--success, #16a34a); }
    .auth-card__reglas li.ok::before { content: '● '; }
  `],
})
export class RestablecerComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly token = inject(ActivatedRoute).snapshot.queryParamMap.get('token') ?? '';

  password = '';
  confirmar = '';
  readonly loading = signal(false);
  readonly mostrar = signal(false);
  readonly fallo = signal<Diagnostico | null>(null);
  readonly tokenRoto = signal(false);

  reglaLongitud() { return this.password.length >= 8; }
  reglaLetra() { return /[a-zA-Z]/.test(this.password); }
  reglaDigito() { return /\d/.test(this.password); }
  reglaCoincide() { return this.password.length > 0 && this.password === this.confirmar; }
  formularioValido() {
    return this.reglaLongitud() && this.reglaLetra() && this.reglaDigito() && this.reglaCoincide();
  }

  onSubmit() {
    if (this.loading() || !this.formularioValido()) {
      return;
    }
    this.loading.set(true);
    this.fallo.set(null);
    this.authService.restablecerPassword(this.token, this.password).subscribe({
      next: () => this.router.navigate(['/login'], {
        queryParams: { restablecida: '1' },
      }),
      error: (err) => {
        this.loading.set(false);
        this.fallo.set(diagnosticar(err));
        this.tokenRoto.set(err instanceof HttpErrorResponse && err.status === 400);
      },
    });
  }
}
