export interface Sesion {
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

export interface CategoriaOpcion {
  idCategoria: number;
  nombre: string;
  /** La API ya las envia; sirven de subtitulo al elegir ("17 a 18 años"). */
  edadMin?: number;
  edadMax?: number;
}

export interface SesionCrearRequest {
  idCategoria: number;
  fecha: string;
  horaInicio: string;
  horaFin: string;
  campo: string | null;
}

/** 1=Lunes ... 7=Domingo, igual que el CHECK de deportivo.horarios_entrenamiento. */
export interface Horario {
  idHorario: number;
  idCategoria: number;
  categoria: string;
  diaSemana: number;
  horaInicio: string;
  horaFin: string;
  campo: string | null;
  descripcion: string | null;
  activo: boolean;
  /**
   * Con qué otro horario se cruza, si se cruza. null cuando está bien.
   * El entrenador no puede estar en dos canchas a la vez; la cancha sí se
   * puede compartir entre dos grupos, por eso no se valida.
   */
  chocaCon: string | null;
}

export interface HorarioCrearRequest {
  idCategoria: number;
  diaSemana: number;
  horaInicio: string;
  horaFin: string;
  campo: string | null;
  descripcion: string | null;
}

export const DIAS_SEMANA = [
  { valor: 1, nombre: 'Lunes' },
  { valor: 2, nombre: 'Martes' },
  { valor: 3, nombre: 'Miércoles' },
  { valor: 4, nombre: 'Jueves' },
  { valor: 5, nombre: 'Viernes' },
  { valor: 6, nombre: 'Sábado' },
  { valor: 7, nombre: 'Domingo' },
];

/** Una fila del historial de asistencia de una sesion ya ocurrida. */
export interface FilaAsistenciaHistorial {
  idEstudiante: number;
  nombreCompleto: string;
  posicion: string | null;
  /** PRESENTE | TARDE | AUSENTE | JUSTIFICADO | SIN_REGISTRO */
  estado: string;
  /** Hora medida por el QR. Vacia cuando la marco el entrenador a mano. */
  horaEntrada: string | null;
  metodo: string | null;
  observacion: string | null;
}

/**
 * Que paso en un entrenamiento. Sin formacion: eso es de partidos, no de
 * sesiones.
 */
export interface HistorialSesion {
  idSesion: number;
  categoria: string;
  entrenador: string;
  fecha: string;
  horaInicio: string | null;
  horaFin: string | null;
  campo: string | null;
  estado: string;
  tieneEvaluacion: boolean;
  estadoEvaluacion: string | null;
  resumen: {
    convocados: number;
    presentes: number;
    tarde: number;
    ausentes: number;
    justificados: number;
    /** Nadie paso lista por ellos. No es lo mismo que ausente. */
    sinRegistro: number;
  };
  asistencias: FilaAsistenciaHistorial[];
}
