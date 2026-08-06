import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs';

export interface LoginRequest {
  username: string;
  password: string;
}

/**
 * cedula, correo y fechaNacimiento son obligatorios: son columnas NOT NULL de
 * seguridad.personas desde la reestructuracion. Mientras el formulario no los
 * enviaba, el alta fallaba siempre.
 */
export interface RegisterRequest {
  nombre: string;
  apellido: string;
  cedula: string;
  correo: string;
  fechaNacimiento: string;
  username: string;
  password: string;
  /** Opcional: en blanco crea un USER, como siempre. Solo ADMINISTRADOR puede pedir otro. */
  rol?: string;
}

export interface AuthResponse {
  username: string;
  nombre: string;
  rol: string;
  /** Solo vienen poblados en la respuesta de register(); login()/getProfile() no los mandan. */
  idPersona?: number;
  idUsuario?: number;
}

/**
 * Servicio de autenticacion.
 * El access token y el refresh token viajan en cookies HttpOnly + Secure +
 * SameSite=Strict que establece el backend (Bloque A.1): este servicio
 * nunca los lee ni los guarda, solo mantiene en memoria los datos de
 * presentacion del usuario autenticado.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = '/api/auth';

  currentUser = signal<AuthResponse | null>(null);
  isLoggedIn = computed(() => this.currentUser() !== null);

  constructor(private http: HttpClient, private router: Router) {}

  login(request: LoginRequest) {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, request).pipe(
      tap((response) => this.currentUser.set(response)),
    );
  }

  register(request: RegisterRequest) {
    return this.http.post<AuthResponse>(`${this.apiUrl}/registro`, request);
  }

  logout() {
    return this.http.post<void>(`${this.apiUrl}/logout`, null).pipe(
      tap(() => {
        this.currentUser.set(null);
        this.router.navigate(['/login']);
      }),
    );
  }

  getProfile() {
    return this.http.get<AuthResponse>(`${this.apiUrl}/me`).pipe(
      tap((user) => this.currentUser.set(user)),
    );
  }
}
