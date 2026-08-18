/** Espejan ConsentimientoDtos del backend. */

export interface Consentimiento {
  idConsentimiento: number;
  idRepresentante: number;
  idEstudiante: number;
  alcance: string;
  otorgadoEn: string;
  registradoPorUsername: string | null;
  revocadoEn: string | null;
  vigente: boolean;
}

export interface OtorgarConsentimientoRequest {
  idRepresentante: number;
  idEstudiante: number;
  alcance: string;
}

/** Lo mínimo que hace falta de cada representante para cruzar vínculos. */
export interface RepresentanteConVinculos {
  idRepresentante: number;
  nombre: string;
  apellido: string;
  activo: boolean;
  representados: { idEstudiante: number; relacion: string | null }[];
}

export interface EstudianteOpcion {
  idEstudiante: number;
  nombreCompleto: string;
  categoria: string | null;
}
