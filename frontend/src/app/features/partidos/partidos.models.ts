export interface Partido {
  matchId: number;
  categoryId: number;
  category: string;
  date: string;
  time: string | null;
  goalsFor: number | null;
  goalsAgainst: number | null;
  note: string | null;
  result: 'GANADO' | 'EMPATADO' | 'PERDIDO' | 'PENDIENTE';
  hasLineup: boolean;
  starters: number;
  closed: boolean;
  closedAt: string | null;
}

export interface PartidoPage {
  content: Partido[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
}

export interface CrearPartido {
  categoryId: number;
  date: string;
  time: string | null;
  note: string | null;
}

export interface Resultado {
  goalsFor: number;
  goalsAgainst: number;
  note: string | null;
}

export interface JugadorConvocado {
  studentId: number;
  fullName: string;
  position: string | null;
  positionId: number | null;
  starter: boolean;
  average: number | null;
  attendanceRecords: number;
  trainingSessions: number;
}

export interface NoConvocable {
  studentId: number;
  fullName: string;
  reason: string;
}

export interface VentanaRendimiento {
  weeks: number;
  from: string;
  to: string;
  trainingSessions: number;
}

export interface Alineacion {
  matchId: number;
  categoryId: number;
  category: string;
  date: string;
  saved: boolean;
  rating: number | null;
  note: string | null;
  window: VentanaRendimiento;
  starters: JugadorConvocado[];
  substitutes: JugadorConvocado[];
  available: JugadorConvocado[];
  notCallable: NoConvocable[];
  starterSlots: number;
  closed: boolean;
}

export interface JugadorEnCancha {
  studentId: number;
  positionId: number | null;
  starter: boolean;
}

export interface FeedbackAlineacion {
  comment: string | null;
  available: boolean;
  reason: string | null;
}

export interface Posicion {
  positionId: number;
  name: string;
  abbreviation: string;
}
