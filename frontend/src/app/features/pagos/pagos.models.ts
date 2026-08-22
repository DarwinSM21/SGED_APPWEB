/** Espejan PagoDtos del backend (org.uteq.backend.academico.pago.dto). */

export interface EstudianteOpcionPago {
  idEstudiante: number;
  nombreCompleto: string;
  categoria: string;
  /** ISO "YYYY-MM-DD". Marca desde que mes tiene sentido cobrarle. */
  fechaIngreso: string;
}

export interface RegistrarMembresiaRequest {
  idEstudiante: number;
  anio: number;
  meses: number[];
  monto: number;
  fechaPago: string | null;
}

export interface RegistrarDiarioRequest {
  idEstudiante: number;
  monto: number;
  fechaPago: string | null;
}

export type TipoPago = 'MEMBRESIA' | 'DIARIO';

export interface PagoResponse {
  idPago: number;
  idEstudiante: number;
  estudiante: string;
  tipo: TipoPago;
  anio: number | null;
  mes: number | null;
  monto: number;
  fechaPago: string;
  registradoPor: string;
}

export interface IngresosMes {
  anio: number;
  mes: number;
  total: number;
  cantidadPagos: number;
}
