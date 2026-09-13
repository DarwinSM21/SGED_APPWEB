
export interface CategoriaDetalle {
  name: string;
  minAge: number | null;
  maxAge: number | null;
  description: string | null;
}

export interface PosicionResumen {
  name: string;
  abbreviation: string | null;
}

export interface EntrenadorAsignado {
  name: string;
  specialty: string | null;
}

export interface Companero {
  studentId: number;
  name: string;
  position: string | null;
}

export interface MiEquipo {
  category: CategoriaDetalle;
  position: PosicionResumen | null;
  coach: EntrenadorAsignado | null;
  teammates: Companero[];
}
