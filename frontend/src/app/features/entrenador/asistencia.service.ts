import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MarcaAsistencia, Nomina } from './asistencia.models';

@Injectable({ providedIn: 'root' })
export class AsistenciaSesionService {

  private readonly http = inject(HttpClient);

  nomina(idSesion: number): Observable<Nomina> {
    return this.http.get<Nomina>(`/api/asistencias/sesion/${idSesion}`);
  }

  /**
   * Manda la lista completa, no solo lo que cambio. El backend hace upsert por
   * (sesion, estudiante), asi que reenviar todo es idempotente y evita el caso
   * feo de que dos pestañas abiertas dejen media lista de cada una.
   */
  pasarLista(idSesion: number, marcas: MarcaAsistencia[]): Observable<Nomina> {
    return this.http.put<Nomina>(`/api/asistencias/sesion/${idSesion}`, { marcas });
  }
}
