/** Espejan MiEquipoDtos del backend (org.uteq.backend.academico.estudiante.dto). */

export interface CategoriaDetalle {
  nombre: string;
  edadMin: number | null;
  edadMax: number | null;
  descripcion: string | null;
}

export interface PosicionResumen {
  nombre: string;
  abreviatura: string | null;
}

export interface EntrenadorAsignado {
  nombre: string;
  especialidad: string | null;
}

export interface Companero {
  idEstudiante: number;
  nombre: string;
  posicion: string | null;
}

export interface MiEquipo {
  categoria: CategoriaDetalle;
  /** null si el estudiante no tiene posicion nominal asignada todavia. */
  posicion: PosicionResumen | null;
  /** null si su categoria no tiene ninguna sesion programada a futuro. */
  entrenador: EntrenadorAsignado | null;
  companeros: Companero[];
}

// InformeEstudiante (estadisticas) se reutiliza de features/representante/representante.models:
// mismo DTO que ya usa el representante, el backend tambien lo comparte tal cual (InformeService).
