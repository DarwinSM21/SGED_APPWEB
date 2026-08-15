import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';

export interface AuditoriaResponse {
  id: number;
  fecha: string;
  usuario: string;
  rol: string | null;
  accion: string;
  entidad: string | null;
  entidadId: number | null;
  descripcion: string;
}

export interface AuditoriaPagina {
  content: AuditoriaResponse[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface FiltrosAuditoria {
  usuario?: string;
  accion?: string;
  entidad?: string;
  fechaDesde?: string;
  fechaHasta?: string;
}

@Injectable({ providedIn: 'root' })
export class AuditoriaService {
  private readonly http = inject(HttpClient);

  listar(filtros: FiltrosAuditoria, page: number, size: number) {
    let params = new HttpParams().set('page', page).set('size', size);
    if (filtros.usuario) params = params.set('usuario', filtros.usuario);
    if (filtros.accion) params = params.set('accion', filtros.accion);
    if (filtros.entidad) params = params.set('entidad', filtros.entidad);
    if (filtros.fechaDesde) params = params.set('fechaDesde', filtros.fechaDesde);
    if (filtros.fechaHasta) params = params.set('fechaHasta', filtros.fechaHasta);

    return this.http.get<AuditoriaPagina>('/api/admin/auditorias', { params });
  }
}
