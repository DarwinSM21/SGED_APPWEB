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
