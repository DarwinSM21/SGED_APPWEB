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

export interface FilaAsistenciaHistorial {
  idEstudiante: number;
  nombreCompleto: string;
  posicion: string | null;
  estado: string;
  horaEntrada: string | null;
  metodo: string | null;
  observacion: string | null;
}

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
    sinRegistro: number;
  };
  asistencias: FilaAsistenciaHistorial[];
}
