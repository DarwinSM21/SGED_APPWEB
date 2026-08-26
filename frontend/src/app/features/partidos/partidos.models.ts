export interface Partido {
  idPartido: number;
  idCategoria: number;
  categoria: string;
  fecha: string;
  hora: string | null;
  golesFavor: number | null;
  golesContra: number | null;
  observacion: string | null;
  /** GANADO | EMPATADO | PERDIDO | PENDIENTE. Lo calcula el backend. */
  resultado: 'GANADO' | 'EMPATADO' | 'PERDIDO' | 'PENDIENTE';
  tieneAlineacion: boolean;
  titulares: number;
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

/**
 * Un jugador dentro de la convocatoria, con los números que lo pusieron
 * donde está. Se muestran en pantalla a propósito: el entrenador tiene que
 * poder ver por qué el sistema lo sugirió ahí, y tener con qué responder
 * cuando un padre pregunta por qué su hijo quedó en el banco.
 */
export interface JugadorConvocado {
  idEstudiante: number;
  nombreCompleto: string;
  /** Abreviatura del puesto que ocupa. null si está sin puesto asignado. */
  posicion: string | null;
  idPosicion: number | null;
  titular: boolean;
  /** null = no lo evaluaron ni una vez en la ventana. No es lo mismo que cero. */
  promedio: number | null;
  presencias: number;
  entrenamientos: number;
}

export interface NoConvocable {
  idEstudiante: number;
  nombreCompleto: string;
  motivo: string;
}

/** La ventana de rendimiento con la que se calculó la sugerencia. */
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
  /** true = la guardó el entrenador; false = es la sugerencia del sistema. */
  guardada: boolean;
  valoracion: number | null;
  observacion: string | null;
  ventana: VentanaRendimiento;
  titulares: JugadorConvocado[];
  suplentes: JugadorConvocado[];
  disponibles: JugadorConvocado[];
  noConvocables: NoConvocable[];
  cupoTitulares: number;
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
