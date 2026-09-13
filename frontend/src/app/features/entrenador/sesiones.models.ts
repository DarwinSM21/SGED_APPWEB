export interface Sesion {
  sessionId: number;
  category: string;
  coach: string;
  date: string;
  startTime: string | null;
  endTime: string | null;
  field: string | null;
  status: string;
  hasEvaluation: boolean;
}

export interface CategoriaOpcion {
  categoryId: number;
  name: string;
  minAge?: number;
  maxAge?: number;
}

export interface SesionCrearRequest {
  categoryId: number;
  date: string;
  startTime: string;
  endTime: string;
  field: string | null;
}

export interface Horario {
  scheduleId: number;
  categoryId: number;
  category: string;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  field: string | null;
  description: string | null;
  active: boolean;
  conflictsWith: string | null;
}

export interface HorarioCrearRequest {
  categoryId: number;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  field: string | null;
  description: string | null;
}

export const DIAS_SEMANA = [
  { valor: 1, name: 'Lunes' },
  { valor: 2, name: 'Martes' },
  { valor: 3, name: 'Miércoles' },
  { valor: 4, name: 'Jueves' },
  { valor: 5, name: 'Viernes' },
  { valor: 6, name: 'Sábado' },
  { valor: 7, name: 'Domingo' },
];

export interface FilaAsistenciaHistorial {
  studentId: number;
  fullName: string;
  position: string | null;
  status: string;
  checkInTime: string | null;
  method: string | null;
  note: string | null;
}

export interface HistorialSesion {
  sessionId: number;
  category: string;
  coach: string;
  date: string;
  startTime: string | null;
  endTime: string | null;
  field: string | null;
  status: string;
  hasEvaluation: boolean;
  evaluationStatus: string | null;
  summary: {
    calledUp: number;
    present: number;
    late: number;
    absentees: number;
    excused: number;
    withoutRecord: number;
  };
  attendances: FilaAsistenciaHistorial[];
}
