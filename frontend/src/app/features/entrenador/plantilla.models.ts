export interface JugadorPlantilla {
  idEstudiante: number;
  nombreCompleto: string;
  /** Abreviatura de la posicion nominal del estudiante (ej. "ED", "DFC"), o null si no la tiene registrada. */
  posicion: string | null;
  /** Id del puesto. Hace falta para poder GUARDAR la alineacion: sin el, se
   *  reenviarian posiciones nulas y el once perderia sus puestos. */
  idPosicion?: number | null;
  promedioAcumulado: number;
}

export interface Plantilla {
  idSesion: number;
  categoria: string;
  titulares: JugadorPlantilla[];
  suplentes: JugadorPlantilla[];
  excluidosPorLesion: number[];
}

/**
 * La alineacion de una sesion. `guardada` distingue las dos cosas que el
 * sistema maneja: la sugerencia que calcula el servidor por promedio, y el
 * once que el entrenador decidio poner en cancha.
 */
export interface Alineacion {
  idSesion: number;
  categoria: string;
  fecha: string;
  guardada: boolean;
  valoracion: number | null;
  observacion: string | null;
  titulares: JugadorPlantilla[];
  suplentes: JugadorPlantilla[];
  /** Presentes que no estan en el once: de aqui salen los cambios. */
  disponibles: JugadorPlantilla[];
}

export interface JugadorEnCancha {
  idEstudiante: number;
  idPosicion: number | null;
  titular: boolean;
}

export interface FeedbackPlantilla {
  comentario: string | null;
  generadoPorIa: boolean;
  motivoNoDisponible: string | null;
}

/**
 * Zona de cancha a la que pertenece cada posicion especifica, solo para
 * decidir de que color es el anillo del token y la leyenda. No es un dato
 * del backend: el catalogo real tiene 11 posiciones -una por rol de una
 * formacion 4-3-3, ver deportivo.posiciones-, esto las agrupa en las 4
 * zonas clasicas nada mas para el color.
 */
export type ZonaCancha = 'POR' | 'DEF' | 'MED' | 'DEL' | 'SIN_POSICION';

const ZONA_POR_ABREVIATURA: Record<string, ZonaCancha> = {
  POR: 'POR',
  DLI: 'DEF', DCI: 'DEF', DCD: 'DEF', DLD: 'DEF',
  II: 'MED', MC: 'MED', ID: 'MED',
  EI: 'DEL', DEL: 'DEL', ED: 'DEL',
};

export function zonaDe(abreviatura: string | null): ZonaCancha {
  if (!abreviatura) return 'SIN_POSICION';
  return ZONA_POR_ABREVIATURA[abreviatura] ?? 'SIN_POSICION';
}

/**
 * Coordenada fija de cada posicion sobre la cancha vertical (viewBox
 * 400x560, ataque arriba / arco propio abajo, igual que ORDEN_BANDAS en
 * plantilla.component.ts). Como PlantillaService ahora titulariza a lo sumo
 * un estudiante por posicion (ver PlantillaService.calcular), cada punto es
 * unico -ya no hace falta repartir varios jugadores dentro de una misma
 * banda-, lo que permite ubicar cada rol con precision en vez de solo
 * agruparlo en una de 4 franjas anchas.
 *
 * Izquierda/derecha siguen la convencion estandar de diagramas tacticos
 * vistos desde arriba con el ataque hacia el fondo del rival: la izquierda
 * del equipo (atacando hacia arriba en este dibujo) cae del lado de menor X.
 */
export const COORDENADA_POR_ABREVIATURA: Record<string, { x: number; y: number }> = {
  POR: { x: 200, y: 515 },
  DLI: { x: 65, y: 425 },
  DCI: { x: 155, y: 430 },
  DCD: { x: 245, y: 430 },
  DLD: { x: 335, y: 425 },
  II: { x: 105, y: 280 },
  MC: { x: 200, y: 280 },
  ID: { x: 295, y: 280 },
  EI: { x: 70, y: 105 },
  DEL: { x: 200, y: 85 },
  ED: { x: 330, y: 105 },
};

export function inicialesDe(nombreCompleto: string): string {
  const partes = nombreCompleto.trim().split(/\s+/);
  const ultimo = partes[partes.length - 1] ?? '';
  return ultimo.slice(0, 2).toUpperCase();
}

export function apellidoDe(nombreCompleto: string): string {
  const partes = nombreCompleto.trim().split(/\s+/);
  return partes[partes.length - 1] ?? nombreCompleto;
}

/** El backend manda LocalTime como "HH:mm:ss"; se muestra en 12 horas con AM/PM, sin segundos. */
export function horaCorta(hora: string | null): string | null {
  if (!hora) return hora;
  const [h, m] = hora.slice(0, 5).split(':').map(Number);
  const periodo = h < 12 ? 'AM' : 'PM';
  const h12 = h % 12 === 0 ? 12 : h % 12;
  return `${h12}:${String(m).padStart(2, '0')} ${periodo}`;
}
