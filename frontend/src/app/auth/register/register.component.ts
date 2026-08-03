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
          <label for="cedula">Cédula</label>
          <input id="cedula" type="text" [(ngModel)]="cedula" name="cedula"
                 required pattern="\\d{10}" inputmode="numeric" maxlength="10" />
        </div>
        <div>
          <label for="correo">Correo de contacto</label>
          <input id="correo" type="email" [(ngModel)]="correo" name="correo" required />
        </div>
        <div>
          <label for="fechaNacimiento">Fecha de nacimiento</label>
          <input id="fechaNacimiento" type="date" [(ngModel)]="fechaNacimiento"
                 name="fechaNacimiento" required />
        </div>
        <div>
          <label for="username">Usuario (correo de acceso)</label>
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
  cedula = '';
  correo = '';
  fechaNacimiento = '';
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
        this.loading = false;
        this.error = this.mensajeDeError(err.status);
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
