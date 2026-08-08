/** Espejan PersonaRequest/EstudianteRequest/EstudianteResponse del backend. */

export interface CategoriaOpcion {
  idCategoria: number;
  nombre: string;
}

export interface PersonaAltaRequest {
  nombre: string;
  apellido: string;
  cedula: string;
  correo: string;
  telefono: string | null;
  foto: string | null;
  fechaNacimiento: string;
}

export interface PersonaCreada {
  idPersona: number;
}

export interface EstudianteAltaRequest {
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
}

export interface EstudiantePagina {
  content: EstudianteResponse[];
}
