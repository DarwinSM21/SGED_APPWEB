export interface SesionHoy {
  idSesion: number;
  categoria: string;
  entrenador: string;
  horaInicio: string | null;
  horaFin: string | null;
  campo: string | null;
  estado: string;
  tieneEvaluacion: boolean;
}
