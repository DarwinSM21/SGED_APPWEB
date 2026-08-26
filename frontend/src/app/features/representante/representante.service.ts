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

  /**
   * El informe en palabras. Va aparte y a demanda: cada llamada consume cuota
   * de un servicio externo, asi que no se pide al abrir la pantalla.
   */
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
