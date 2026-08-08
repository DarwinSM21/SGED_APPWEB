/** Espejan PagoDtos del backend (org.uteq.backend.academico.pago.dto). */

export interface EstudianteOpcionPago {
  idEstudiante: number;
  nombreCompleto: string;
  categoria: string;
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
