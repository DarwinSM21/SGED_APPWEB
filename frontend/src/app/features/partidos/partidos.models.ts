export interface Partido {
  idPartido: number;
  idCategoria: number;
  categoria: string;
  fecha: string;
  hora: string | null;
  golesFavor: number | null;
  golesContra: number | null;
  observacion: string | null;
  resultado: 'GANADO' | 'EMPATADO' | 'PERDIDO' | 'PENDIENTE';
  tieneAlineacion: boolean;
  titulares: number;
  cerrado: boolean;
  cerradoEn: string | null;
}

export interface PartidoPage {
  contenido: Partido[];
  pagina: number;
  tamano: number;
  total: number;
  totalPaginas: number;
}

export interface CrearPartido {
  idCategoria: number;
  fecha: string;
  hora: string | null;
  observacion: string | null;
}

export interface Resultado {
  golesFavor: number;
  golesContra: number;
  observacion: string | null;
}

export interface JugadorConvocado {
  idEstudiante: number;
  nombreCompleto: string;
  posicion: string | null;
  idPosicion: number | null;
  titular: boolean;
  promedio: number | null;
  presencias: number;
  entrenamientos: number;
}

export interface NoConvocable {
  idEstudiante: number;
  nombreCompleto: string;
  motivo: string;
}

export interface VentanaRendimiento {
  semanas: number;
  desde: string;
  hasta: string;
  entrenamientos: number;
}

export interface Alineacion {
  idPartido: number;
  idCategoria: number;
  categoria: string;
  fecha: string;
  guardada: boolean;
  valoracion: number | null;
  observacion: string | null;
  ventana: VentanaRendimiento;
  titulares: JugadorConvocado[];
  suplentes: JugadorConvocado[];
  disponibles: JugadorConvocado[];
  noConvocables: NoConvocable[];
  cupoTitulares: number;
  cerrado: boolean;
}

export interface JugadorEnCancha {
  idEstudiante: number;
  idPosicion: number | null;
  titular: boolean;
}

export interface FeedbackAlineacion {
  comentario: string | null;
  disponible: boolean;
  motivo: string | null;
}

export interface Posicion {
  idPosicion: number;
  nombre: string;
  abreviatura: string;
}
