import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import {
  EvaluacionSesion, GuardarJugadorRequest, EstadoGuardado,
} from './evaluacion.models';

/**
 * Servicio de evaluacion diaria.
 *
 * <p>Resuelve el requisito que condiciona todo el modulo: el entrenador
 * califica desde el celular, en la cancha, donde la conexion se cae. Dos
 * mecanismos lo sostienen.
 *
 * <p><b>Autoguardado.</b> Cada cambio se envia solo, con un retardo corto que
 * agrupa los movimientos seguidos de un mismo slider en una sola peticion. El
 * entrenador nunca pulsa "guardar".
 *
 * <p><b>Cola offline.</b> Si la peticion falla por falta de red, el cambio se
 * guarda en localStorage y se reintenta cuando el navegador vuelve a estar en
 * linea. Se guarda por jugador, de modo que un reintento posterior pisa al
 * anterior en vez de acumular peticiones contradictorias: lo que importa es el
 * ultimo valor, no la secuencia.
 */
@Injectable({ providedIn: 'root' })
export class EvaluacionService {

  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/evaluaciones';
  private readonly claveCola = 'sged.evaluacion.pendientes';

  /** Retardo del autoguardado. Suficiente para agrupar el arrastre de un slider. */
  private static readonly RETARDO_MS = 800;

  private temporizadores = new Map<number, ReturnType<typeof setTimeout>>();

  readonly estado = signal<EstadoGuardado>('guardado');
  readonly pendientes = signal<number>(0);
  readonly hayPendientes = computed(() => this.pendientes() > 0);

  constructor() {
    // Al recuperar la conexion se vacia la cola sin que el usuario haga nada.
    window.addEventListener('online', () => this.sincronizarPendientes());
    this.pendientes.set(this.leerCola().size);
  }

  abrirSesion(idSesion: number): Observable<EvaluacionSesion> {
    return this.http.get<EvaluacionSesion>(`${this.apiUrl}/sesion/${idSesion}`);
  }

  // La alineacion/plantilla vive en PlantillaService, no aqui: tiene su
  // propia llamada de IA a demanda que no pertenece a este servicio.

  finalizar(idSesion: number, observacionGeneral: string): Observable<void> {
    return this.http.post<void>(
      `${this.apiUrl}/sesion/${idSesion}/finalizar`, observacionGeneral);
  }

  /**
   * Programa el guardado de un jugador. Llamarlo repetidamente para el mismo
   * jugador reinicia el temporizador: solo se envia el ultimo valor.
   */
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
          // Un 4xx es un rechazo del servidor (por ejemplo, calificar a quien
          // no asistio) y reintentarlo no lo va a arreglar: se muestra el
          // error. Un fallo de red si se encola.
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

  /** Reintenta todo lo encolado. Se dispara al volver la conexion. */
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

  // ------------------------------------------------------------------
  // Cola en localStorage, indexada por estudiante
  // ------------------------------------------------------------------

  private leerCola(): Map<number, EntradaCola> {
    try {
      const crudo = localStorage.getItem(this.claveCola);
      if (!crudo) {
        return new Map();
      }
      return new Map(Object.entries(JSON.parse(crudo) as Record<string, EntradaCola>)
        .map(([k, v]) => [Number(k), v]));
    } catch {
      // Si el contenido esta corrupto es preferible perder la cola que dejar
      // la pantalla inutilizable.
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
