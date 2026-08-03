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
  /** Clave = nombre del criterio. */
  puntajes: Record<string, number>;
  /**
   * true cuando los puntajes vienen del entrenamiento anterior y el
   * entrenador todavia no los confirmo hoy. La interfaz los distingue para
   * que sepa que esta viendo un valor heredado, no uno que el puso.
   */
  precargado: boolean;
  lesionado: boolean;
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

export interface JugadorPlantilla {
  idEstudiante: number;
  nombreCompleto: string;
  posicion: string | null;
  promedioAcumulado: number;
}

export interface Plantilla {
  idSesion: number;
  categoria: string;
  titulares: JugadorPlantilla[];
  suplentes: JugadorPlantilla[];
  excluidosPorLesion: number[];
  comentario: string | null;
  comentarioGeneradoPorIa: boolean;
}

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
