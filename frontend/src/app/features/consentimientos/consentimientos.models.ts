
export interface Consentimiento {
  consentId: number;
  guardianId: number;
  studentId: number;
  scope: string;
  grantedAt: string;
  registeredByUsername: string | null;
  revokedAt: string | null;
  vigente: boolean;
}

export interface OtorgarConsentimientoRequest {
  guardianId: number;
  studentId: number;
  scope: string;
}

export interface RepresentanteConVinculos {
  guardianId: number;
  name: string;
  lastName: string;
  active: boolean;
  wards: { studentId: number; relationship: string | null }[];
}

export interface EstudianteOpcion {
  studentId: number;
  fullName: string;
  category: string | null;
}
