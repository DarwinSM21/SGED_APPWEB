export interface EstudianteOpcionReporte {
  idEstudiante: number;
  nombreCompleto: string;
  categoria: string;
}

export interface CategoriaOpcionReporte {
  idCategoria: number;
  nombre: string;
}

export interface FiltrosReporte {
  estudianteId?: number | null;
  categoria?: number | null;
  activo?: boolean | null;
  fechaDesde?: string;
  fechaHasta?: string;
}
