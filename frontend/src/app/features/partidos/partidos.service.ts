import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Alineacion, CrearPartido, FeedbackAlineacion, JugadorEnCancha,
  Partido, PartidoPage, Posicion, Resultado,
} from './partidos.models';

@Injectable({ providedIn: 'root' })
export class PartidosService {

  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/partidos';

  listar(idCategoria: number | null, page = 0, size = 20): Observable<PartidoPage> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (idCategoria) params = params.set('idCategoria', idCategoria);
    return this.http.get<PartidoPage>(this.apiUrl, { params });
  }

  crear(partido: CrearPartido): Observable<Partido> {
    return this.http.post<Partido>(this.apiUrl, partido);
  }

  registrarResultado(idPartido: number, resultado: Resultado): Observable<Partido> {
    return this.http.put<Partido>(`${this.apiUrl}/${idPartido}/resultado`, resultado);
  }

  eliminar(idPartido: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${idPartido}`);
  }

  /**
   * La alineación guardada si existe; si no, la sugerencia calculada con el
   * rendimiento de las últimas semanas. El servidor decide cuál devolver y la
   * bandera `guardada` dice cuál llegó.
   */
  alineacion(idPartido: number): Observable<Alineacion> {
    return this.http.get<Alineacion>(`${this.apiUrl}/${idPartido}/alineacion`);
  }

  guardarAlineacion(idPartido: number, jugadores: JugadorEnCancha[],
                    valoracion: number | null, observacion: string | null): Observable<Alineacion> {
    return this.http.put<Alineacion>(`${this.apiUrl}/${idPartido}/alineacion`,
      { jugadores, valoracion, observacion });
  }

  /** Descarta los cambios y vuelve a la sugerencia del sistema. */
  restablecerAlineacion(idPartido: number): Observable<Alineacion> {
    return this.http.delete<Alineacion>(`${this.apiUrl}/${idPartido}/alineacion`);
  }

  /**
   * Comentario de IA sobre el once. Es una llamada aparte y explícita: no se
   * dispara sola al abrir la pantalla, para no gastar cuota de un servicio
   * externo sin que el entrenador lo haya pedido.
   */
  feedback(idPartido: number): Observable<FeedbackAlineacion> {
    return this.http.post<FeedbackAlineacion>(`${this.apiUrl}/${idPartido}/alineacion/feedback`, null);
  }

  posiciones(): Observable<Posicion[]> {
    return this.http.get<Posicion[]>('/api/posiciones/activas');
  }
}
