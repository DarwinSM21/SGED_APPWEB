
export interface EstudianteOpcionPago {
  idEstudiante: number;
  nombreCompleto: string;
  categoria: string;
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
  anuladoEn: string | null;
  anuladoPor: string | null;
  motivoAnulacion: string | null;
}

export interface IngresosMes {
  anio: number;
  mes: number;
  total: number;
  cantidadPagos: number;
}
