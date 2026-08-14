export interface JugadorPlantilla {
  idEstudiante: number;
  nombreCompleto: string;
  /** Abreviatura de la posicion nominal del estudiante (ej. "ED", "DFC"), o null si no la tiene registrada. */
  posicion: string | null;
  promedioAcumulado: number;
}

export interface Plantilla {
  idSesion: number;
  categoria: string;
  titulares: JugadorPlantilla[];
  suplentes: JugadorPlantilla[];
  excluidosPorLesion: number[];
}

export interface FeedbackPlantilla {
  comentario: string | null;
  generadoPorIa: boolean;
  motivoNoDisponible: string | null;
}

/**
 * Zona de cancha a la que pertenece cada posicion especifica, solo para
 * decidir en que fila del campo se dibuja el token y de que color es el
 * anillo. No es un dato del backend: el catalogo real tiene 10 posiciones
 * (docs/basedatos/DATA-DICTIONARY.md), esto las agrupa en las 4 zonas
 * clasicas para la visualizacion.
 */
export type ZonaCancha = 'POR' | 'DEF' | 'MED' | 'DEL' | 'SIN_POSICION';

const ZONA_POR_ABREVIATURA: Record<string, ZonaCancha> = {
  POR: 'POR',
  DFC: 'DEF', LD: 'DEF', LI: 'DEF',
  MCD: 'MED', MC: 'MED', MP: 'MED',
  ED: 'DEL', EI: 'DEL', DC: 'DEL',
};

export function zonaDe(abreviatura: string | null): ZonaCancha {
  if (!abreviatura) return 'SIN_POSICION';
  return ZONA_POR_ABREVIATURA[abreviatura] ?? 'SIN_POSICION';
}

export function inicialesDe(nombreCompleto: string): string {
  const partes = nombreCompleto.trim().split(/\s+/);
  const ultimo = partes[partes.length - 1] ?? '';
  return ultimo.slice(0, 2).toUpperCase();
}

export function apellidoDe(nombreCompleto: string): string {
  const partes = nombreCompleto.trim().split(/\s+/);
  return partes[partes.length - 1] ?? nombreCompleto;
}

/** El backend manda LocalTime como "HH:mm:ss"; a nadie le interesan los segundos en pantalla. */
export function horaCorta(hora: string | null): string | null {
  return hora ? hora.slice(0, 5) : hora;
}
