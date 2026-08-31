export interface EstudianteEnRiesgo {
  idEstudiante: number;
  nombreCompleto: string;
  categoria: string | null;
  mensualidadPendiente: boolean;
  asistenciaBaja: boolean;
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
  totalEnRiesgo: number;
  estudiantes: EstudianteEnRiesgo[];
}

export interface IngresoMes {
  anio: number;
  mes: number;
  total: number;
  cantidadPagos: number;
}

export interface HistoricoIngresos {
  meses: IngresoMes[];
  total: number;
  promedioMensual: number;
  mejorMes: IngresoMes | null;
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

export interface DiaAsistencia {
  fecha: string;
  presentes: number;
  esperados: number;
  porcentaje: number;
}

export interface MapaAsistencia {
  desde: string;
  hasta: string;
  dias: DiaAsistencia[];
  promedio: number;
  mejorDia: DiaAsistencia | null;
  peorDia: DiaAsistencia | null;
}
