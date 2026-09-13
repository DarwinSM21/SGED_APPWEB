
export interface EstudianteOpcionPago {
  studentId: number;
  fullName: string;
  category: string;
  enrollmentDate: string;
}

export interface RegistrarMembresiaRequest {
  studentId: number;
  year: number;
  months: number[];
  amount: number;
  paymentDate: string | null;
}

export interface RegistrarDiarioRequest {
  studentId: number;
  amount: number;
  paymentDate: string | null;
}

export type TipoPago = 'MEMBRESIA' | 'DIARIO';

export interface PagoResponse {
  paymentId: number;
  studentId: number;
  student: string;
  type: TipoPago;
  year: number | null;
  month: number | null;
  amount: number;
  paymentDate: string;
  registeredBy: string;
  voidedAt: string | null;
  voidedBy: string | null;
  voidReason: string | null;
}

export interface IngresosMes {
  year: number;
  month: number;
  total: number;
  paymentCount: number;
}
