import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CategoriaOpcion, Sesion, SesionCrearRequest } from './sesiones.models';

@Injectable({ providedIn: 'root' })
export class SesionesService {
  private readonly http = inject(HttpClient);

  /** Historial completo del entrenador autenticado, no solo las de hoy. */
  listarMias(page = 0, size = 20) {
    return this.http.get<Sesion[]>(`/api/sesiones/mias?page=${page}&size=${size}`);
  }

  crear(request: SesionCrearRequest) {
    return this.http.post<Sesion>('/api/sesiones', request);
  }

  listarCategoriasActivas() {
    return this.http.get<CategoriaOpcion[]>('/api/categorias/activas');
  }
}
