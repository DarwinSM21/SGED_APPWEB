
export interface Asistencia {
  attendanceId: number;
  date: string;
  category: string;
  checkInTime: string | null;
  status: 'PRESENT' | 'LATE';
}

export interface MiHistorial {
  attendances: Asistencia[];
  percentageLast30Days: number | null;
}
