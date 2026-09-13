
export interface EstudianteResumen {
  studentId: number;
  fullName: string;
  category: string;
}

export interface PromedioCriterio {
  criterion: string;
  average: number;
}

export interface LesionResumen {
  injuryId: number;
  description: string;
  injuryDate: string;
  estimatedReturnDate: string | null;
  dischargeDate: string | null;
  active: boolean;
}

export interface InformeEstudiante {
  studentId: number;
  fullName: string;
  category: string;
  averagesByCriterion: PromedioCriterio[];
  injuryHistory: LesionResumen[];
  attendancePercentage: number | null;
}

export interface Notificacion {
  notificationId: number;
  studentId: number;
  student: string;
  type: 'ASISTENCIA' | 'LESION';
  message: string;
  read: boolean;
  createdAt: string;
}

export interface ComentarioInforme {
  comment: string | null;
  available: boolean;
  reason: string | null;
}
