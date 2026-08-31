
export interface CategoriaDetalle {
  nombre: string;
  edadMin: number | null;
  edadMax: number | null;
  descripcion: string | null;
}

export interface PosicionResumen {
  nombre: string;
  abreviatura: string | null;
}

export interface EntrenadorAsignado {
  nombre: string;
  especialidad: string | null;
}

export interface Companero {
  idEstudiante: number;
  nombre: string;
  posicion: string | null;
}

export interface MiEquipo {
  categoria: CategoriaDetalle;
  posicion: PosicionResumen | null;
  entrenador: EntrenadorAsignado | null;
  companeros: Companero[];
}
