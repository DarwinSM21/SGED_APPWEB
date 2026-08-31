
export interface Asistencia {
  idAsistencia: number;
  fecha: string;
  categoria: string;
  horaEntrada: string | null;
  estado: 'PRESENTE' | 'TARDE';
}

export interface MiHistorial {
  asistencias: Asistencia[];
  porcentajeUltimos30Dias: number | null;
}
