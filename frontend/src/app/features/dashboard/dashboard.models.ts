export interface EstudianteEnRiesgo {
  idEstudiante: number;
  nombreCompleto: string;
  categoria: string | null;
  mensualidadPendiente: boolean;
  asistenciaBaja: boolean;
  /** null si su categoría no tuvo sesiones programadas en el rango medido. */
  porcentajeAsistencia: number | null;
  lesionActiva: boolean;
  totalAlertas: number;
}

export interface PanelAlertas {
  anio: number;
  mes: number;
  umbralAsistencia: number;
  estudiantesActivos: number;
  conMensualidadPendiente: number;
  conAsistenciaBaja: number;
  conLesionActiva: number;
  estudiantes: EstudianteEnRiesgo[];
}

export interface SesionHoy {
  idSesion: number;
  categoria: string;
  entrenador: string;
  fecha: string;
  horaInicio: string | null;
  horaFin: string | null;
  campo: string | null;
  estado: string;
  tieneEvaluacion: boolean;
}
