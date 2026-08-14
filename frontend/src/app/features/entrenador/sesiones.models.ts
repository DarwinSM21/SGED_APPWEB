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
  categoria: string;
  diaSemana: number;
  horaInicio: string;
  horaFin: string;
  campo: string | null;
  descripcion: string | null;
  activo: boolean;
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
