import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MiEquipo } from './mi-equipo.models';
import { InformeEstudiante } from '../representante/representante.models';

@Injectable({ providedIn: 'root' })
export class MiEquipoService {
  private readonly http = inject(HttpClient);

  miInforme() {
    return this.http.get<InformeEstudiante>('/api/estudiante/mi-informe');
  }

  miEquipo() {
    return this.http.get<MiEquipo>('/api/estudiante/mi-equipo');
  }
}
