import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../auth.service';
import { PelotaAnimadaComponent } from '../pelota-animada.component';
import { Diagnostico, diagnosticar } from '../../core/diagnostico-error';

/**
 * Pantalla "¿Olvidaste tu contraseña?" (RF-37). Un solo campo —usuario o
 * correo— y, al enviar, una confirmación genérica siempre igual: el backend
 * responde 202 exista o no la cuenta, así que esta pantalla tampoco puede
 * decir si el correo se envió.
 */
@Component({
  selector: 'app-recuperar',
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
        @if (enviado()) {
          <div class="auth-card">
            <h2>Revisa tu correo</h2>
            <p class="auth-card__subtitle">
              Si existe una cuenta asociada, se enviaron instrucciones al correo
              registrado. El enlace vence en 30 minutos y solo puede usarse una vez.
            </p>
            <a class="btn btn--primary btn--block" routerLink="/login">Volver al inicio de sesión</a>
          </div>
        } @else {
          <form class="auth-card" (ngSubmit)="onSubmit()">
            <h2>Recuperar contraseña</h2>
            <p class="auth-card__subtitle">
              Escribe tu usuario o tu correo y te enviaremos un enlace para elegir
              una contraseña nueva.
            </p>

            <label class="field" for="identificador">
              <span class="field__label">Usuario o correo</span>
              <span class="field__control">
                <input id="identificador" type="text" name="identificador"
                       [(ngModel)]="identificador" required
                       autocapitalize="off" autocorrect="off" spellcheck="false"
                       placeholder="tu.usuario@uteq.edu.ec" />
              </span>
            </label>

            @if (fallo(); as f) {
              <div class="alert alert--danger" role="alert" aria-live="assertive">
                <span class="fallo-que">{{ f.mensaje }}</span>
                @if (f.sugerencia) { <span class="fallo-como">{{ f.sugerencia }}</span> }
              </div>
            }

            <button class="btn btn--primary btn--block" type="submit" [disabled]="loading()">
              @if (loading()) { <span class="spinner"></span> Enviando… }
              @else { Enviar enlace }
            </button>

            <p class="auth-card__subtitle" style="text-align:center;margin-top:1rem">
              <a routerLink="/login">Volver al inicio de sesión</a>
            </p>
          </form>
        }
      </main>
    </div>
  `,
})
export class RecuperarComponent {
  private readonly authService = inject(AuthService);

  identificador = '';
  readonly loading = signal(false);
  readonly enviado = signal(false);
  readonly fallo = signal<Diagnostico | null>(null);

  onSubmit() {
    if (this.loading()) {
      return;
    }
    const identificador = this.identificador.trim();
    if (!identificador) {
      this.fallo.set({ origen: 'peticion', mensaje: 'Escribe tu usuario o tu correo' });
      return;
    }

    this.loading.set(true);
    this.fallo.set(null);
    this.authService.solicitarRecuperacion(identificador).subscribe({
      next: () => this.enviado.set(true),
      error: (err) => {
        this.loading.set(false);
        this.fallo.set(diagnosticar(err));
      },
    });
  }
}
