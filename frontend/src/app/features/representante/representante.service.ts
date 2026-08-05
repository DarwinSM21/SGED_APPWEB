import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { EstudianteResumen, InformeEstudiante } from './representante.models';

@Injectable({ providedIn: 'root' })
export class RepresentanteService {
  private readonly http = inject(HttpClient);

  misRepresentados() {
    return this.http.get<EstudianteResumen[]>('/api/representante/estudiantes');
  }

  informeDe(idEstudiante: number) {
    return this.http.get<InformeEstudiante>(`/api/representante/estudiantes/${idEstudiante}/informe`);
  }
}
