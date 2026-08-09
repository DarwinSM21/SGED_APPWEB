/** Espejan InformeDtos del backend (org.uteq.backend.academico.representante.dto). */

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
  /** Ultimos 30 dias; null si su categoria no tuvo sesiones programadas en ese rango. */
  porcentajeAsistencia: number | null;
}

/** Espeja NotificacionDtos.NotificacionResponse del backend (RF-22). */
export interface Notificacion {
  idNotificacion: number;
  idEstudiante: number;
  estudiante: string;
  tipo: 'ASISTENCIA' | 'LESION';
  mensaje: string;
  leida: boolean;
  creadaEn: string;
}
