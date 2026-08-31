
export type EstadoAsistencia = 'PRESENTE' | 'TARDE' | 'AUSENTE' | 'JUSTIFICADO';

export interface FilaNomina {
  idEstudiante: number;
  nombreCompleto: string;
  estado: EstadoAsistencia | null;
  metodo: 'QR' | 'MANUAL' | null;
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
