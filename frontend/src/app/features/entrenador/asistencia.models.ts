
export type EstadoAsistencia = 'PRESENT' | 'LATE' | 'ABSENT' | 'EXCUSED';

export interface FilaNomina {
  studentId: number;
  fullName: string;
  status: EstadoAsistencia | null;
  method: 'QR' | 'MANUAL' | null;
  checkInTime: string | null;
  note: string | null;
}

export interface Nomina {
  sessionId: number;
  category: string;
  date: string;
  startTime: string | null;
  editable: boolean;
  nonEditableReason: string | null;
  rows: FilaNomina[];
}

export interface MarcaAsistencia {
  studentId: number;
  status: EstadoAsistencia;
  note?: string | null;
}
