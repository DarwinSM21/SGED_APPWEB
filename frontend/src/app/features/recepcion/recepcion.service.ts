import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { SesionHoy } from '../dashboard/dashboard.models';
import { TokenQr } from './recepcion.models';

@Injectable({ providedIn: 'root' })
export class RecepcionService {
  private readonly http = inject(HttpClient);

  sesionesDeHoy() {
    return this.http.get<SesionHoy[]>('/api/sesiones/hoy');
  }

  emitirToken(idSesion: number) {
    return this.http.post<TokenQr>(`/api/asistencias/qr/sesion/${idSesion}/token`, null);
  }
}
