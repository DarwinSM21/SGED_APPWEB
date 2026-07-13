import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environments';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  refreshToken?: string;
  username: string;
  nombre: string;
  rol: string;
}

@Injectable({
  providedIn: 'root',
})
export class Auth {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  readonly currentUser = signal<LoginResponse | null>(this.loadStoredUser());

  login(username: string, password: string): Observable<LoginResponse> {
    const body: LoginRequest = { username, password };
    return this.http.post<LoginResponse>(`${this.apiUrl}/auth/login`, body).pipe(
      tap((res) => {
        localStorage.setItem('token', res.token);
        if (res.refreshToken) {
          localStorage.setItem('refreshToken', res.refreshToken);
        }
        localStorage.setItem('user', JSON.stringify(res));
        this.currentUser.set(res);
      })
    );
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    this.currentUser.set(null);
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  getRol(): string | null {
    return this.currentUser()?.rol ?? null;
  }

  getUser(): LoginResponse | null {
    return this.currentUser();
  }

  private loadStoredUser(): LoginResponse | null {
    const raw = localStorage.getItem('user');
    return raw ? JSON.parse(raw) : null;
  }
}