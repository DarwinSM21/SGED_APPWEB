export interface EstudianteEnRiesgo {
  studentId: number;
  fullName: string;
  category: string | null;
  pendingMembershipFee: boolean;
  lowAttendance: boolean;
  attendancePercentage: number | null;
  activeInjury: boolean;
  totalAlerts: number;
}

export interface PanelAlertas {
  year: number;
  month: number;
  attendanceThreshold: number;
  activeStudents: number;
  withPendingMembership: number;
  withLowAttendance: number;
  withActiveInjury: number;
  totalAtRisk: number;
  students: EstudianteEnRiesgo[];
}

export interface IngresoMes {
  year: number;
  month: number;
  total: number;
  paymentCount: number;
}

export interface HistoricoIngresos {
  months: IngresoMes[];
  total: number;
  monthlyAverage: number;
  bestMonth: IngresoMes | null;
}

export interface SesionHoy {
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

export interface DiaAsistencia {
  date: string;
  present: number;
  expected: number;
  percentage: number;
}

export interface MapaAsistencia {
  from: string;
  to: string;
  days: DiaAsistencia[];
  average: number;
  bestDay: DiaAsistencia | null;
  worstDay: DiaAsistencia | null;
}
