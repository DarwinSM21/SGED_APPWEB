import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  lastName: string;
  nationalId: string;
  email: string;
  birthDate: string;
  username: string;
  password: string;
  role?: string;
}

export interface AuthResponse {
  username: string;
  name: string;
  role: string;
  personId?: number;
  userId?: number;
}

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

  /**
   * Pide un enlace de restablecimiento (RF-37). El backend responde siempre
   * 202, exista o no la cuenta, así que el resultado no revela nada.
   */
  solicitarRecuperacion(identifier: string) {
    return this.http.post<{ message: string }>(`${this.apiUrl}/forgot`, { identifier });
  }

  /** Fija la contraseña nueva a partir del token del enlace (RF-37). */
  restablecerPassword(token: string, newPassword: string) {
    return this.http.post<void>(`${this.apiUrl}/reset`, { token, newPassword });
  }

  /**
   * Confirma el correo de contacto a partir del token del enlace (RNF-26 /
   * hallazgo H-09). Mientras un correo no esté confirmado, el backend no le
   * envía el enlace de restablecimiento de contraseña.
   */
  confirmarCorreo(token: string) {
    return this.http.post<void>(`${this.apiUrl}/confirmar-correo`, { token });
  }
}
