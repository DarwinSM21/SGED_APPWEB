export interface EstudianteOpcion {
  idEstudiante: number;
  nombreCompleto: string;
  categoria: string;
}

export interface CrearRepresentanteRequest {
  idPersona: number;
  idUsuario: number;
  parentesco: string | null;
  telefonoContacto: string | null;
  idsEstudiantesIniciales: number[];
}

export interface HabilitarAccesoRequest {
  username: string;
  password: string;
}
