import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  user: {
    id: string;
    email: string;
    name: string;
    role: string;
  };
}

export const DEMO_CREDENTIALS = {
  admin: {
    email: 'admin@profutbol.ec',
    password: 'admin123',
  },
  entrenador: {
    email: 'entrenador@profutbol.ec',
    password: 'entrenador123',
  },
};

const VALID_USERS = [
  {
    id: '1',
    email: 'admin@profutbol.ec',
    password: 'admin123',
    name: 'Administrador',
    role: 'ADMINISTRADOR',
  },
  {
    id: '2',
    email: 'entrenador@profutbol.ec',
    password: 'entrenador123',
    name: 'Entrenador',
    role: 'ENTRENADOR',
  },
];

@Injectable({
  providedIn: 'root',
})
export class Auth {

  login(email: string, password: string): Observable<LoginResponse> {
    // Simular una llamada al backend con delay
    return new Observable(subscriber => {
      setTimeout(() => {
        const user = VALID_USERS.find(u => u.email === email && u.password === password);
        
        if (user) {
          const token = btoa(`${email}:${Date.now()}`);
          localStorage.setItem('token', token);
          localStorage.setItem('user', JSON.stringify(user));
          
          subscriber.next({
            token,
            user: {
              id: user.id,
              email: user.email,
              name: user.name,
              role: user.role,
            },
          });
          subscriber.complete();
        } else {
          subscriber.error(new Error('Credenciales inválidas'));
        }
      }, 800);
    });
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  getUser() {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  }
}
