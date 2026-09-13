export interface EstudianteOpcionReporte {
  studentId: number;
  fullName: string;
  category: string;
}

export interface CategoriaOpcionReporte {
  categoryId: number;
  name: string;
}

export interface FiltrosReporte {
  estudianteId?: number | null;
  category?: number | null;
  active?: boolean | null;
  fechaDesde?: string;
  fechaHasta?: string;
}
