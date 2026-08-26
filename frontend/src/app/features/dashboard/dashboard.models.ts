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
  /** Cuántos están en riesgo en total. `estudiantes` viene recortado. */
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
  /** Serie completa y sin huecos: un mes sin cobros viaja en cero. */
  meses: IngresoMes[];
  total: number;
  promedioMensual: number;
  /** null si en todo el rango no hubo ningún cobro. */
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
  /** ISO "YYYY-MM-DD". Se guarda como texto y se parsea a mano: `new Date(iso)`
   *  lo interpreta como UTC y en Ecuador (-05) devuelve el día anterior. */
  fecha: string;
  presentes: number;
  esperados: number;
  porcentaje: number;
}

export interface MapaAsistencia {
  desde: string;
  hasta: string;
  /** Solo los días que tuvieron entrenamiento; los demás no son cero, no existen. */
  dias: DiaAsistencia[];
  promedio: number;
  mejorDia: DiaAsistencia | null;
  peorDia: DiaAsistencia | null;
}
