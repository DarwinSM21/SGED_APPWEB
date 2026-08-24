import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CategoriaOpcion, Horario, HorarioCrearRequest, Sesion, SesionCrearRequest } from './sesiones.models';

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

  misHorarios() {
    return this.http.get<Horario[]>('/api/horarios/mios');
  }

  crearHorario(request: HorarioCrearRequest) {
    return this.http.post<Horario>('/api/horarios', request);
  }

  /**
   * Cambia un horario existente. El backend rehace las sesiones futuras de
   * ese horario que todavia no tienen nada registrado; las que ya se
   * dictaron se quedan como estan.
   */
  editarHorario(idHorario: number, request: HorarioCrearRequest) {
    return this.http.put<Horario>(`/api/horarios/${idHorario}`, request);
  }

  desactivarHorario(idHorario: number) {
    return this.http.delete<void>(`/api/horarios/${idHorario}`);
  }
}
