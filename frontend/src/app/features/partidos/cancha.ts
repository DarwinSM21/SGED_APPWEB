
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
