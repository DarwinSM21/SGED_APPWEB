import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  nombre: string;
  apellido: string;
  username: string;
  password: string;
}

export interface AuthResponse {
  username: string;
  nombre: string;
  rol: string;
}

/**
 * Servicio de autenticacion.
 * Almacena el accessToken en memoria (no en localStorage).
 * El refreshToken viaja en cookie HttpOnly (manejada por el backend).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = '/api/auth';
  private accessToken: string | null = null;

  currentUser = signal<AuthResponse | null>(null);
  isLoggedIn = computed(() => this.currentUser() !== null);

  constructor(private http: HttpClient, private router: Router) {}

  login(request: LoginRequest) {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, request, {
      observe: 'response',
    }).pipe(
      tap((response) => {
        const authHeader = response.headers.get('Authorization');
        if (authHeader) {
          this.accessToken = authHeader.replace('Bearer ', '');
        }
        this.currentUser.set(response.body);
      })
    );
  }

  register(request: RegisterRequest) {
    return this.http.post<AuthResponse>(`${this.apiUrl}/registro`, request);
  }

  logout() {
    return this.http.post<void>(`${this.apiUrl}/logout`, null, {
      headers: { Authorization: `Bearer ${this.accessToken}` },
    }).pipe(
      tap(() => {
        this.accessToken = null;
        this.currentUser.set(null);
        this.router.navigate(['/login']);
      })
    );
  }

  getAccessToken(): string | null {
    return this.accessToken;
  }

  getProfile() {
    return this.http.get<AuthResponse>(`${this.apiUrl}/me`, {
      headers: { Authorization: `Bearer ${this.accessToken}` },
    }).pipe(
      tap((user) => this.currentUser.set(user))
    );
  }
}
