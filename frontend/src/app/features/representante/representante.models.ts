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
}
