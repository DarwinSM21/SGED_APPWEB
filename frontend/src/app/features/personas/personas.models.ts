/** Espejan los DTOs del backend (seguridad.persona / seguridad.usuario / academico.estudiante / deportivo.entrenador / academico.representante). */

export interface PersonaRequest {
  nombre: string;
  apellido: string;
  cedula: string;
  correo: string;
  telefono: string | null;
  foto: string | null;
  fechaNacimiento: string;
}

export interface PersonaResponse {
  idPersona: number;
  nombre: string;
  apellido: string;
  cedula: string;
  correo: string;
  telefono: string | null;
  foto: string | null;
  fechaNacimiento: string;
  activo: boolean;
  createdAt: string;
}

export const ROLES_USUARIO = ['ADMINISTRADOR', 'ENTRENADOR', 'RECEPCIONISTA', 'REPRESENTANTE', 'ESTUDIANTE'] as const;
export type RolUsuario = typeof ROLES_USUARIO[number];

export interface UsuarioRequest {
  idPersona: number;
  idEstadoGeneral: number;
  username: string;
  /** En blanco/null al editar significa "no cambiar la contraseña actual". Obligatoria al crear. */
  password: string | null;
  rol: string | null;
}

export interface UsuarioResponse {
  idUsuario: number;
  idPersona: number;
  nombrePersona: string;
  apellidoPersona: string;
  correoPersona: string;
  idEstadoGeneral: number;
  estadoGeneralNombre: string;
  username: string;
  roles: string[];
  ultimoAcceso: string | null;
  activo: boolean;
  createdAt: string;
}

export interface CategoriaOpcion {
  idCategoria: number;
  nombre: string;
}

export interface EstudianteRequest {
  idPersona: number;
  idCategoria: number;
  idEstadoGeneral: number;
  codigoEstudiante: string;
  fechaIngreso: string;
  peso: number | null;
  altura: number | null;
}

export interface EstudianteResponse {
  idEstudiante: number;
  idPersona: number;
  idCategoria: number;
  idEstadoGeneral: number;
  nombrePersona: string;
  apellidoPersona: string;
  nombreCategoria: string;
  nombreEstadoGeneral: string;
  codigoEstudiante: string;
  fechaIngreso: string;
  peso: number | null;
  altura: number | null;
  activo: boolean;
  createdAt: string;
}

export interface HabilitarAccesoRequest {
  username: string;
  password: string;
}

export interface EspecialidadOpcion {
  idEspecialidad: number;
  nombre: string;
}

export interface EntrenadorRequest {
  idPersona: number;
  idUsuario: number;
  idEspecialidad: number | null;
  experienciaAnios: number | null;
  certificacion: string | null;
}

export interface EntrenadorResponse {
  idEntrenador: number;
  idPersona: number;
  nombre: string;
  apellido: string;
  cedula: string;
  correo: string;
  telefono: string | null;
  idUsuario: number;
  username: string;
  idEspecialidad: number | null;
  nombreEspecialidad: string | null;
  experienciaAnios: number | null;
  certificacion: string | null;
  activo: boolean;
  createdAt: string;
}

export interface RepresentanteRequest {
  idPersona: number;
  idUsuario: number;
  parentesco: string | null;
  telefonoContacto: string | null;
  idsEstudiantesIniciales: number[];
}

export interface EstudianteVinculado {
  idEstudiante: number;
  nombreCompleto: string;
  categoria: string;
  /** Parentesco de este vínculo puntual, no del representante: puede ser madre de uno y tía de otro. */
  relacion: string | null;
  contactoPrincipal: boolean;
}

/** Cuerpo de POST /api/representantes/{id}/estudiantes/{idEstudiante}. */
export interface VinculoRequest {
  relacion: string | null;
  contactoPrincipal: boolean;
}

export interface RepresentanteResponse {
  idRepresentante: number;
  idPersona: number;
  nombre: string;
  apellido: string;
  cedula: string;
  correo: string;
  idUsuario: number;
  username: string;
  parentesco: string | null;
  telefonoContacto: string | null;
  activo: boolean;
  createdAt: string;
  representados: EstudianteVinculado[];
}

/** Estado derivado de una Persona, cruzando las listas en el cliente (ver personas.service.ts). */
export interface PersonaConEstado {
  persona: PersonaResponse;
  usuario: UsuarioResponse | null;
  estudiante: EstudianteResponse | null;
  entrenador: EntrenadorResponse | null;
  representante: RepresentanteResponse | null;
}
