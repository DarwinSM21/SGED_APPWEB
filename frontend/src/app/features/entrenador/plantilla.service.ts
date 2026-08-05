import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Plantilla, FeedbackPlantilla } from './plantilla.models';

@Injectable({ providedIn: 'root' })
export class PlantillaService {

  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/evaluaciones';

  /** Calculo determinista de la alineacion. Nunca llama a la IA. */
  obtener(idSesion: number): Observable<Plantilla> {
    return this.http.get<Plantilla>(`${this.apiUrl}/sesion/${idSesion}/plantilla`);
  }

  /**
   * Comentario de IA sobre la alineacion ya calculada. Es una llamada aparte
   * y explicita (boton "Feedback IA"): no se dispara sola al abrir la
   * pantalla, para no gastar cuota de un servicio externo sin que el
   * entrenador lo haya pedido.
   */
  pedirFeedback(idSesion: number): Observable<FeedbackPlantilla> {
    return this.http.post<FeedbackPlantilla>(`${this.apiUrl}/sesion/${idSesion}/plantilla/feedback`, null);
  }
}
