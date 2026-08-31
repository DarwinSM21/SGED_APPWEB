import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map } from 'rxjs';
import {
  ArticuloRequest, ArticuloResponse, AsignacionRequest, AsignacionResponse,
  DevolucionRequest, MovimientoRequest, MovimientoResponse, PersonaOpcion, StockBajoResponse,
} from './inventario.models';

interface Pagina<T> {
  content: T[];
}

@Injectable({ providedIn: 'root' })
export class InventarioService {
  private readonly http = inject(HttpClient);

  listarArticulosActivos() {
    return this.http.get<ArticuloResponse[]>('/api/inventario/articulos/activos');
  }

  stockBajo() {
    return this.http.get<StockBajoResponse>('/api/inventario/articulos/stock-bajo');
  }

  crearArticulo(request: ArticuloRequest) {
    return this.http.post<ArticuloResponse>('/api/inventario/articulos', request);
  }

  editarArticulo(id: number, request: ArticuloRequest) {
    return this.http.put<ArticuloResponse>(`/api/inventario/articulos/${id}`, request);
  }

  eliminarArticulo(id: number) {
    return this.http.delete<void>(`/api/inventario/articulos/${id}`);
  }

  listarMovimientos() {
    return this.http.get<Pagina<MovimientoResponse>>('/api/inventario/movimientos?size=100&sort=fechaMovimiento,desc')
      .pipe(map((pagina) => pagina.content));
  }

  registrarMovimiento(request: MovimientoRequest) {
    return this.http.post<MovimientoResponse>('/api/inventario/movimientos', request);
  }

  listarAsignaciones() {
    return this.http.get<Pagina<AsignacionResponse>>('/api/inventario/asignaciones?size=100&sort=fechaAsignacion,desc')
      .pipe(map((pagina) => pagina.content));
  }

  crearAsignacion(request: AsignacionRequest) {
    return this.http.post<AsignacionResponse>('/api/inventario/asignaciones', request);
  }

  devolverAsignacion(id: number, request: DevolucionRequest) {
    return this.http.patch<AsignacionResponse>(`/api/inventario/asignaciones/${id}/devolver`, request);
  }

  listarEstudiantesOpcion() {
    return this.http.get<Pagina<{ idEstudiante: number; nombrePersona: string; apellidoPersona: string }>>('/api/estudiantes?size=200').pipe(
      map((pagina): PersonaOpcion[] =>
        pagina.content.map((e) => ({ id: e.idEstudiante, nombreCompleto: `${e.nombrePersona} ${e.apellidoPersona}` }))),
    );
  }

  listarEntrenadoresOpcion() {
    return this.http.get<Pagina<{ idEntrenador: number; nombre: string; apellido: string }>>('/api/entrenadores?size=200').pipe(
      map((pagina): PersonaOpcion[] =>
        pagina.content.map((e) => ({ id: e.idEntrenador, nombreCompleto: `${e.nombre} ${e.apellido}` }))),
    );
  }
}
