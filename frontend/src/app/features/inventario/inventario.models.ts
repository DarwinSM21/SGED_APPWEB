/** Espejan los DTOs del backend (org.uteq.backend.inventario.*.dto). */

export type TipoArticulo = 'UNIFORME' | 'BALON' | 'IMPLEMENTO' | 'OTRO';

export interface ArticuloRequest {
  nombre: string;
  tipo: TipoArticulo;
  talla: string | null;
  descripcion: string | null;
  stockMinimo: number;
  unidadMedida: string | null;
}

export interface ArticuloResponse {
  idArticulo: number;
  nombre: string;
  tipo: TipoArticulo;
  talla: string | null;
  descripcion: string | null;
  stockActual: number;
  stockMinimo: number;
  unidadMedida: string;
  activo: boolean;
  createdAt: string;
}

export interface StockBajoResponse {
  total: number;
  articulos: ArticuloResponse[];
}

export type TipoMovimiento = 'ENTRADA' | 'SALIDA' | 'AJUSTE';

export interface MovimientoRequest {
  idArticulo: number;
  tipoMovimiento: TipoMovimiento;
  cantidad: number;
  motivo: string | null;
}

export interface MovimientoResponse {
  idMovimiento: number;
  idArticulo: number;
  articulo: string;
  tipoMovimiento: TipoMovimiento;
  cantidad: number;
  motivo: string | null;
  registradoPor: string;
  fechaMovimiento: string;
}

export type TipoDestinatario = 'ESTUDIANTE' | 'ENTRENADOR';
export type EstadoAsignacion = 'ASIGNADO' | 'DEVUELTO' | 'PERDIDO';

export interface AsignacionRequest {
  idArticulo: number;
  cantidad: number;
  tipoDestinatario: TipoDestinatario;
  idEstudiante: number | null;
  idEntrenador: number | null;
  fechaDevolucionEsperada: string | null;
  observaciones: string | null;
}

export interface DevolucionRequest {
  estado: 'DEVUELTO' | 'PERDIDO';
  observaciones: string | null;
}

export interface AsignacionResponse {
  idAsignacion: number;
  idArticulo: number;
  articulo: string;
  cantidad: number;
  tipoDestinatario: TipoDestinatario;
  idEstudiante: number | null;
  estudiante: string | null;
  idEntrenador: number | null;
  entrenador: string | null;
  fechaAsignacion: string;
  fechaDevolucionEsperada: string | null;
  fechaDevolucionReal: string | null;
  estado: EstadoAsignacion;
  registradoPor: string;
  observaciones: string | null;
  createdAt: string;
}

export interface PersonaOpcion {
  id: number;
  nombreCompleto: string;
}
