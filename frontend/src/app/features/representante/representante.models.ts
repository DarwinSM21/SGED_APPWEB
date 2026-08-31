
export interface EstudianteResumen {
  idEstudiante: number;
  nombreCompleto: string;
  categoria: string;
}

export interface PromedioCriterio {
  criterio: string;
  promedio: number;
}

export interface LesionResumen {
  idLesion: number;
  descripcion: string;
  fechaLesion: string;
  fechaEstimadaRetorno: string | null;
  fechaAlta: string | null;
  activa: boolean;
}

export interface InformeEstudiante {
  idEstudiante: number;
  nombreCompleto: string;
  categoria: string;
  promediosPorCriterio: PromedioCriterio[];
  historialLesiones: LesionResumen[];
  porcentajeAsistencia: number | null;
}

export interface Notificacion {
  idNotificacion: number;
  idEstudiante: number;
  estudiante: string;
  tipo: 'ASISTENCIA' | 'LESION';
  mensaje: string;
  leida: boolean;
  creadaEn: string;
}

export interface ComentarioInforme {
  comentario: string | null;
  disponible: boolean;
  motivo: string | null;
}
