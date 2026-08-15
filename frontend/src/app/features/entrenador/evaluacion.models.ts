/**
 * Modelos del modulo Entrenador. Espejan los DTO del backend
 * (org.uteq.backend.deportivo.evaluacion.dto.EvaluacionDtos).
 */

export interface CriterioResponse {
  idCriterio: number;
  nombre: string;
  descripcion: string | null;
  puntajeMaximo: number;
}

export interface JugadorEvaluable {
  idEstudiante: number;
  nombreCompleto: string;
  categoria: string;
  idPosicion: number | null;
  posicion: string | null;
  /** PRESENTE, TARDE, AUSENTE o JUSTIFICADO. */
  estadoAsistencia: string;
  /** Clave = nombre del criterio. */
  puntajes: Record<string, number>;
  /**
   * true cuando los puntajes vienen del entrenamiento anterior y el
   * entrenador todavia no los confirmo hoy. La interfaz los distingue para
   * que sepa que esta viendo un valor heredado, no uno que el puso.
   */
  precargado: boolean;
  lesionado: boolean;
  /** id de la lesion activa, para poder dar de alta; null si no esta lesionado. */
  idLesion: number | null;
  /** false si no marco asistencia: la ficha va bloqueada. */
  puedeEvaluarse: boolean;
  motivoBloqueo: string | null;
}

export interface EvaluacionSesion {
  idEvaluacion: number;
  idSesion: number;
  fecha: string;
  categoria: string;
  estado: 'BORRADOR' | 'FINALIZADA';
  criterios: CriterioResponse[];
  jugadores: JugadorEvaluable[];
  observacionGeneral: string | null;
}

export interface PuntajeCriterio {
  idCriterio: number;
  puntaje: number;
}

export interface GuardarJugadorRequest {
  idEstudiante: number;
  idPosicionJugada: number | null;
  puntajes: PuntajeCriterio[];
}

// Plantilla/alineacion: ver plantilla.models.ts (dueño de ese contrato).

export interface Lesion {
  idLesion: number;
  idEstudiante: number;
  estudiante: string;
  descripcion: string;
  fechaLesion: string;
  fechaEstimadaRetorno: string | null;
  fechaAlta: string | null;
  activa: boolean;
}

/** Estado del autoguardado, para poder mostrarselo al entrenador. */
export type EstadoGuardado = 'guardado' | 'guardando' | 'pendiente' | 'error';
