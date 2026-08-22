/** Modelos de la lista de asistencia manual. Espejo de PasarListaDtos.java. */

export type EstadoAsistencia = 'PRESENTE' | 'TARDE' | 'AUSENTE' | 'JUSTIFICADO';

export interface FilaNomina {
  idEstudiante: number;
  nombreCompleto: string;
  /** null = todavia nadie registro nada de este estudiante en esta sesion. */
  estado: EstadoAsistencia | null;
  /** 'QR' cuando lo marco el propio estudiante, 'MANUAL' cuando el entrenador. */
  metodo: 'QR' | 'MANUAL' | null;
  /** Solo la hay si vino del QR: la lista manual no inventa hora de llegada. */
  horaEntrada: string | null;
  observacion: string | null;
}

export interface Nomina {
  idSesion: number;
  categoria: string;
  fecha: string;
  horaInicio: string | null;
  editable: boolean;
  motivoNoEditable: string | null;
  filas: FilaNomina[];
}

export interface MarcaAsistencia {
  idEstudiante: number;
  estado: EstadoAsistencia;
  observacion?: string | null;
}
