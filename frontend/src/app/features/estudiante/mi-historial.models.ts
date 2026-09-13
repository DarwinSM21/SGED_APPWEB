
export interface Asistencia {
  attendanceId: number;
  date: string;
  category: string;
  checkInTime: string | null;
  status: 'PRESENTE' | 'TARDE';
}

export interface MiHistorial {
  attendances: Asistencia[];
  percentageLast30Days: number | null;
}
