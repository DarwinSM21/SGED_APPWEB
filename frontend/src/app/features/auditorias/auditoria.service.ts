import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';

export interface AuditoriaResponse {
  id: number;
  date: string;
  user: string;
  role: string | null;
  action: string;
  entity: string | null;
  entityId: number | null;
  description: string;
}

export interface AuditoriaPagina {
  content: AuditoriaResponse[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface FiltrosAuditoria {
  user?: string;
  action?: string;
  entity?: string;
  fechaDesde?: string;
  fechaHasta?: string;
}

@Injectable({ providedIn: 'root' })
export class AuditoriaService {
  private readonly http = inject(HttpClient);

  listar(filtros: FiltrosAuditoria, page: number, size: number) {
    let params = new HttpParams().set('page', page).set('size', size);
    if (filtros.user) params = params.set('usuario', filtros.user);
    if (filtros.action) params = params.set('accion', filtros.action);
    if (filtros.entity) params = params.set('entidad', filtros.entity);
    if (filtros.fechaDesde) params = params.set('fechaDesde', filtros.fechaDesde);
    if (filtros.fechaHasta) params = params.set('fechaHasta', filtros.fechaHasta);

    return this.http.get<AuditoriaPagina>('/api/admin/auditorias', { params });
  }
}
