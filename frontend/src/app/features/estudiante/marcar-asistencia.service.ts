import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MarcarAsistenciaResponse } from './marcar-asistencia.models';

@Injectable({ providedIn: 'root' })
export class MarcarAsistenciaService {
  private readonly http = inject(HttpClient);

  marcar(token: string) {
    return this.http.post<MarcarAsistenciaResponse>('/api/asistencias/qr/marcar', { token });
  }
}
