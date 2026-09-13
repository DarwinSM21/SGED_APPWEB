
export interface CriterioResponse {
  criterionId: number;
  name: string;
  description: string | null;
  maxScore: number;
}

export interface JugadorEvaluable {
  studentId: number;
  fullName: string;
  category: string;
  positionId: number | null;
  position: string | null;
  attendanceStatus: string | null;
  scores: Record<string, number>;
  preloaded: boolean;
  injured: boolean;
  injuryId: number | null;
  canBeEvaluated: boolean;
  blockReason: string | null;
}

export interface EvaluacionSesion {
  evaluationId: number;
  sessionId: number;
  date: string;
  category: string;
  status: 'BORRADOR' | 'FINALIZADA';
  criteria: CriterioResponse[];
  players: JugadorEvaluable[];
  generalNote: string | null;
}

export interface PuntajeCriterio {
  criterionId: number;
  score: number;
}

export interface GuardarJugadorRequest {
  studentId: number;
  lineupPositionId: number | null;
  scores: PuntajeCriterio[];
}

export interface PosicionOpcion {
  positionId: number;
  name: string;
  abbreviation: string | null;
}

export interface Lesion {
  injuryId: number;
  studentId: number;
  student: string;
  description: string;
  injuryDate: string;
  estimatedReturnDate: string | null;
  dischargeDate: string | null;
  active: boolean;
}

export type EstadoGuardado = 'guardado' | 'guardando' | 'pendiente' | 'error';
