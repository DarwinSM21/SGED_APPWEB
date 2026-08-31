
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
  estadoAsistencia: string | null;
  puntajes: Record<string, number>;
  precargado: boolean;
  lesionado: boolean;
  idLesion: number | null;
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

export interface PosicionOpcion {
  idPosicion: number;
  nombre: string;
  abreviatura: string | null;
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

export type EstadoGuardado = 'guardado' | 'guardando' | 'pendiente' | 'error';
