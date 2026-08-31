import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ComentarioInforme, EstudianteResumen, InformeEstudiante, Notificacion } from './representante.models';

@Injectable({ providedIn: 'root' })
export class RepresentanteService {
  private readonly http = inject(HttpClient);

  misRepresentados() {
    return this.http.get<EstudianteResumen[]>('/api/representante/estudiantes');
  }

  informeDe(idEstudiante: number) {
    return this.http.get<InformeEstudiante>(`/api/representante/estudiantes/${idEstudiante}/informe`);
  }

  comentarioDe(idEstudiante: number) {
    return this.http.post<ComentarioInforme>(
      `/api/representante/estudiantes/${idEstudiante}/informe/comentario`, null);
  }

  misNotificaciones() {
    return this.http.get<Notificacion[]>('/api/representante/notificaciones');
  }

  marcarLeida(idNotificacion: number) {
    return this.http.post<void>(`/api/representante/notificaciones/${idNotificacion}/leida`, null);
  }
}
