
export interface PersonaRequest {
  name: string;
  lastName: string;
  nationalId: string;
  email: string;
  phone: string | null;
  photo: string | null;
  birthDate: string;
}

export interface PersonaResponse {
  personId: number;
  name: string;
  lastName: string;
  nationalId: string;
  email: string;
  phone: string | null;
  photo: string | null;
  birthDate: string;
  active: boolean;
  createdAt: string;
}

export const ROLES_USUARIO = ['ADMINISTRADOR', 'ENTRENADOR', 'RECEPCIONISTA', 'REPRESENTANTE', 'ESTUDIANTE'] as const;
export type RolUsuario = typeof ROLES_USUARIO[number];

export interface UsuarioRequest {
  personId: number;
  generalStatusId: number;
  username: string;
  password: string | null;
  role: string | null;
}

export interface UsuarioResponse {
  userId: number;
  personId: number;
  personName: string;
  personLastName: string;
  personEmail: string;
  generalStatusId: number;
  generalStatusName: string;
  username: string;
  roles: string[];
  lastAccess: string | null;
  active: boolean;
  createdAt: string;
}

export interface CategoriaOpcion {
  categoryId: number;
  name: string;
}

export interface EstudianteRequest {
  personId: number;
  categoryId: number;
  generalStatusId: number;
  studentCode: string;
  enrollmentDate: string;
  weight: number | null;
  height: number | null;
  positionId: number | null;
}

export interface EstudianteResponse {
  studentId: number;
  personId: number;
  categoryId: number;
  generalStatusId: number;
  personName: string;
  personLastName: string;
  categoryName: string;
  generalStatusName: string;
  studentCode: string;
  enrollmentDate: string;
  weight: number | null;
  height: number | null;
  positionId: number | null;
  positionName: string | null;
  positionAbbreviation: string | null;
  active: boolean;
  createdAt: string;
}

export interface PosicionOpcion {
  positionId: number;
  name: string;
  abbreviation: string;
}

export interface HabilitarAccesoRequest {
  username: string;
  password: string;
}

export interface EspecialidadOpcion {
  specialtyId: number;
  name: string;
}

export interface EntrenadorRequest {
  personId: number;
  userId: number;
  specialtyId: number | null;
  yearsOfExperience: number | null;
  certification: string | null;
}

export interface EntrenadorResponse {
  coachId: number;
  personId: number;
  name: string;
  lastName: string;
  nationalId: string;
  email: string;
  phone: string | null;
  userId: number;
  username: string;
  specialtyId: number | null;
  specialtyName: string | null;
  yearsOfExperience: number | null;
  certification: string | null;
  active: boolean;
  createdAt: string;
}

export interface RepresentanteRequest {
  personId: number;
  userId: number;
  relationship: string | null;
  contactPhone: string | null;
  initialStudentIds: number[];
}

export interface EstudianteVinculado {
  studentId: number;
  fullName: string;
  category: string;
  relationship: string | null;
  primaryContact: boolean;
}

export interface VinculoRequest {
  relationship: string | null;
  primaryContact: boolean;
}

export interface RepresentanteResponse {
  guardianId: number;
  personId: number;
  name: string;
  lastName: string;
  nationalId: string;
  email: string;
  userId: number;
  username: string;
  relationship: string | null;
  contactPhone: string | null;
  active: boolean;
  createdAt: string;
  wards: EstudianteVinculado[];
}

export interface PersonaConEstado {
  persona: PersonaResponse;
  user: UsuarioResponse | null;
  student: EstudianteResponse | null;
  coach: EntrenadorResponse | null;
  representante: RepresentanteResponse | null;
}
