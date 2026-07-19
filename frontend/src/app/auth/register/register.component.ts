import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="register-container">
      <h2>Registro</h2>
      <form (ngSubmit)="onSubmit()">
        <div>
          <label for="nombre">Nombre</label>
          <input id="nombre" type="text" [(ngModel)]="nombre" name="nombre" required />
        </div>
        <div>
          <label for="apellido">Apellido</label>
          <input id="apellido" type="text" [(ngModel)]="apellido" name="apellido" required />
        </div>
        <div>
          <label for="username">Email</label>
          <input id="username" type="email" [(ngModel)]="username" name="username" required />
        </div>
        <div>
          <label for="password">Contrasena</label>
          <input id="password" type="password" [(ngModel)]="password" name="password" required minlength="6" />
        </div>
        <div *ngIf="error" class="error">{{ error }}</div>
        <button type="submit" [disabled]="loading">
          {{ loading ? 'Registrando...' : 'Registrarse' }}
        </button>
      </form>
      <p>Ya tienes cuenta? <a routerLink="/login">Iniciar sesion</a></p>
    </div>
  `,
  styles: [`
    .register-container { max-width: 400px; margin: 80px auto; padding: 2rem; border: 1px solid #ddd; border-radius: 8px; }
    div { margin-bottom: 1rem; }
    label { display: block; margin-bottom: 0.25rem; font-weight: bold; }
    input { width: 100%; padding: 0.5rem; border: 1px solid #ccc; border-radius: 4px; }
    button { width: 100%; padding: 0.75rem; background: #1976d2; color: white; border: none; border-radius: 4px; cursor: pointer; }
    button:disabled { background: #ccc; }
    .error { color: #d32f2f; font-size: 0.875rem; }
  `]
})
export class RegisterComponent {
  nombre = '';
  apellido = '';
  username = '';
  password = '';
  loading = false;
  error = '';

  constructor(private authService: AuthService, private router: Router) {}

  onSubmit() {
    this.loading = true;
    this.error = '';
    this.authService.register({
      nombre: this.nombre,
      apellido: this.apellido,
      username: this.username,
      password: this.password,
    }).subscribe({
      next: () => {
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.loading = false;
        this.error = err.status === 409 ? 'El email ya esta registrado' : 'Error del servidor';
      }
    });
  }
}
