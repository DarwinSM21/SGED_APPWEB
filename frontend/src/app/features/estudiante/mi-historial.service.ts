import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MiHistorial } from './mi-historial.models';

@Injectable({ providedIn: 'root' })
export class MiHistorialService {
  private readonly http = inject(HttpClient);

  miHistorial() {
    return this.http.get<MiHistorial>('/api/estudiante/mi-asistencia');
  }
}
