import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import {
  EvaluacionSesion, GuardarJugadorRequest, EstadoGuardado, Lesion, PosicionOpcion,
} from './evaluacion.models';

@Injectable({ providedIn: 'root' })
export class EvaluacionService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/evaluaciones';
  private readonly claveCola = 'sged.evaluacion.pendientes';

  private static readonly RETARDO_MS = 800;

  private temporizadores = new Map<number, ReturnType<typeof setTimeout>>();

  readonly estado = signal<EstadoGuardado>('guardado');
  readonly pendientes = signal<number>(0);
  readonly hayPendientes = computed(() => this.pendientes() > 0);

  constructor() {
    window.addEventListener('online', () => this.sincronizarPendientes());
    this.pendientes.set(this.leerCola().size);
  }

  abrirSesion(idSesion: number): Observable<EvaluacionSesion> {
    return this.http.get<EvaluacionSesion>(`${this.apiUrl}/sesion/${idSesion}`);
  }

  posicionesActivas(): Observable<PosicionOpcion[]> {
    return this.http.get<PosicionOpcion[]>('/api/posiciones/activas');
  }

  actualizarPosicionEstudiante(idEstudiante: number, idPosicion: number | null): Observable<void> {
    return this.http.put<void>(`/api/estudiantes/${idEstudiante}/posicion`, { idPosicion });
  }

  finalizar(idSesion: number, observacionGeneral: string): Observable<void> {
    return this.http.post<void>(
      `${this.apiUrl}/sesion/${idSesion}/finalizar`, observacionGeneral);
  }

  registrarLesion(idEstudiante: number, descripcion: string, fechaEstimadaRetorno?: string): Observable<Lesion> {
    return this.http.post<Lesion>('/api/lesiones', { idEstudiante, descripcion, fechaEstimadaRetorno });
  }

  darDeAltaLesion(idLesion: number): Observable<Lesion> {
    return this.http.post<Lesion>(`/api/lesiones/${idLesion}/alta`, {});
  }

  guardarConRetardo(idSesion: number, request: GuardarJugadorRequest): void {
    const anterior = this.temporizadores.get(request.idEstudiante);
    if (anterior) {
      clearTimeout(anterior);
    }
    this.estado.set('guardando');
    this.temporizadores.set(request.idEstudiante, setTimeout(
      () => this.enviar(idSesion, request), EvaluacionService.RETARDO_MS));
  }

  private enviar(idSesion: number, request: GuardarJugadorRequest): void {
    this.temporizadores.delete(request.idEstudiante);

    this.http.put<void>(`${this.apiUrl}/sesion/${idSesion}/jugadores`, request)
      .pipe(
        tap(() => {
          this.quitarDeCola(request.idEstudiante);
          this.estado.set(this.hayPendientes() ? 'pendiente' : 'guardado');
        }),
        catchError((err) => {
          if (err.status >= 400 && err.status < 500) {
            this.estado.set('error');
          } else {
            this.encolar(idSesion, request);
            this.estado.set('pendiente');
          }
          return of(void 0);
        }),
      )
      .subscribe();
  }

  sincronizarPendientes(): void {
    const cola = this.leerCola();
    if (cola.size === 0) {
      return;
    }
    this.estado.set('guardando');
    for (const [, entrada] of cola) {
      this.enviar(entrada.idSesion, entrada.request);
    }
  }

  private leerCola(): Map<number, EntradaCola> {
    try {
      const crudo = localStorage.getItem(this.claveCola);
      if (!crudo) {
        return new Map();
      }
      return new Map(Object.entries(JSON.parse(crudo) as Record<string, EntradaCola>)
        .map(([k, v]) => [Number(k), v]));
    } catch {
      return new Map();
    }
  }

  private escribirCola(cola: Map<number, EntradaCola>): void {
    const obj: Record<string, EntradaCola> = {};
    cola.forEach((v, k) => { obj[String(k)] = v; });
    localStorage.setItem(this.claveCola, JSON.stringify(obj));
    this.pendientes.set(cola.size);
  }

  private encolar(idSesion: number, request: GuardarJugadorRequest): void {
    const cola = this.leerCola();
    cola.set(request.idEstudiante, { idSesion, request });
    this.escribirCola(cola);
  }

  private quitarDeCola(idEstudiante: number): void {
    const cola = this.leerCola();
    if (cola.delete(idEstudiante)) {
      this.escribirCola(cola);
    }
  }
}

interface EntradaCola {
  idSesion: number;
  request: GuardarJugadorRequest;
}
