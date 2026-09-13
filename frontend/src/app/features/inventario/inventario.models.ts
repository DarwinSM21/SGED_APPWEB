
export type TipoArticulo = 'UNIFORME' | 'BALON' | 'IMPLEMENTO' | 'OTRO';

export interface ArticuloRequest {
  name: string;
  type: TipoArticulo;
  size: string | null;
  description: string | null;
  minimumStock: number;
  unitOfMeasure: string | null;
}

export interface ArticuloResponse {
  itemId: number;
  name: string;
  type: TipoArticulo;
  size: string | null;
  description: string | null;
  currentStock: number;
  minimumStock: number;
  unitOfMeasure: string;
  active: boolean;
  createdAt: string;
}

export interface StockBajoResponse {
  totalElements: number;
  items: ArticuloResponse[];
}

export type TipoMovimiento = 'ENTRADA' | 'SALIDA' | 'AJUSTE';

export interface MovimientoRequest {
  itemId: number;
  movementType: TipoMovimiento;
  quantity: number;
  reason: string | null;
}

export interface MovimientoResponse {
  movementId: number;
  itemId: number;
  item: string;
  movementType: TipoMovimiento;
  quantity: number;
  reason: string | null;
  registeredBy: string;
  movementDate: string;
}

export type TipoDestinatario = 'STUDENT' | 'COACH';
export type EstadoAsignacion = 'ASSIGNED' | 'RETURNED' | 'LOST';

export interface AsignacionRequest {
  itemId: number;
  quantity: number;
  recipientType: TipoDestinatario;
  studentId: number | null;
  coachId: number | null;
  expectedReturnDate: string | null;
  notes: string | null;
}

export interface DevolucionRequest {
  status: 'RETURNED' | 'LOST';
  notes: string | null;
}

export interface AsignacionResponse {
  assignmentId: number;
  itemId: number;
  item: string;
  quantity: number;
  recipientType: TipoDestinatario;
  studentId: number | null;
  student: string | null;
  coachId: number | null;
  coach: string | null;
  assignmentDate: string;
  expectedReturnDate: string | null;
  actualReturnDate: string | null;
  status: EstadoAsignacion;
  registeredBy: string;
  notes: string | null;
  createdAt: string;
}

export interface PersonaOpcion {
  id: number;
  fullName: string;
}
