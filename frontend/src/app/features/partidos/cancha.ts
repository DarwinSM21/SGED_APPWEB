/**
 * Geometria y colores de la cancha. Solo presentacion: el catalogo real
 * de posiciones vive en deportivo.posiciones y llega por
 * /api/posiciones/activas.
 */

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
